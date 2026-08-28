package app.gamenative.account

import android.content.Context
import app.gamenative.PrefManager
import app.gamenative.data.GameAccountPreference
import app.gamenative.data.GameSource
import app.gamenative.data.PlatformAccount
import app.gamenative.db.dao.GameAccountPreferenceDao
import app.gamenative.db.dao.PlatformAccountDao
import app.gamenative.service.amazon.AmazonAuthManager
import app.gamenative.service.epic.EpicAuthManager
import app.gamenative.service.gog.GOGAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for multi-account support across all platforms.
 *
 * Each platform stores credentials under `{filesDir}/{platform}/accounts/{accountId}/`.
 * The "active" account's credentials are copied to the original single-account location
 * for backward compatibility with existing service code.
 */
@Singleton
class AccountManager @Inject constructor(
    private val platformAccountDao: PlatformAccountDao,
    private val gameAccountPreferenceDao: GameAccountPreferenceDao,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Reactive state ──────────────────────────────────────────────────────

    /** All accounts grouped by platform. */
    val allAccounts: StateFlow<Map<String, List<PlatformAccount>>> =
        platformAccountDao.getAllFlow()
            .map { list -> list.groupBy { it.platform } }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Account count per platform (for quick "has accounts" checks). */
    fun hasAccountsFlow(platform: String): Flow<Boolean> =
        platformAccountDao.countForPlatformFlow(platform).map { it > 0 }

    suspend fun hasAccounts(platform: String): Boolean =
        platformAccountDao.countForPlatform(platform) > 0

    // ── Account CRUD ────────────────────────────────────────────────────────

    /**
     * Add or update an account. If this is the first account for the platform,
     * it becomes primary automatically.
     */
    suspend fun addAccount(
        context: Context,
        platform: String,
        accountId: String,
        displayName: String,
        avatarUrl: String = "",
        credentialsJson: JSONObject,
    ): PlatformAccount {
        val existing = platformAccountDao.getByAccountId(platform, accountId)
        val accountsDir = getAccountsDir(context, platform, accountId)
        accountsDir.mkdirs()

        // Write credentials to account-specific directory
        File(accountsDir, "credentials.json").writeText(credentialsJson.toString())

        val isFirst = platformAccountDao.countForPlatform(platform) == 0
        val account = if (existing != null) {
            existing.copy(
                displayName = displayName,
                avatarUrl = avatarUrl,
                credentialsPath = accountsDir.absolutePath,
            ).also { platformAccountDao.update(it) }
        } else {
            PlatformAccount(
                platform = platform,
                accountId = accountId,
                displayName = displayName,
                avatarUrl = avatarUrl,
                isPrimary = isFirst,
                credentialsPath = accountsDir.absolutePath,
            ).also { platformAccountDao.insert(it) }
        }

        // If primary (first account or already primary), sync to active location
        if (account.isPrimary) {
            syncActiveCredentials(context, platform, accountId)
        }

        Timber.i("[AccountManager] Added/updated $platform account: $displayName ($accountId)")
        return account
    }

    /**
     * Remove an account. If it was primary, promote the next available account.
     */
    suspend fun removeAccount(context: Context, platform: String, accountId: String) {
        val account = platformAccountDao.getByAccountId(platform, accountId)
        if (account == null) {
            Timber.w("[AccountManager] Account not found: $platform/$accountId")
            return
        }

        val wasPrimary = account.isPrimary

        // Delete credentials directory
        val accountsDir = getAccountsDir(context, platform, accountId)
        accountsDir.deleteRecursively()

        // Remove from DB
        platformAccountDao.deleteByAccountId(platform, accountId)

        // If removed account was primary, promote another
        if (wasPrimary) {
            val remaining = platformAccountDao.getByPlatform(platform)
            if (remaining.isNotEmpty()) {
                val newPrimary = remaining.first()
                platformAccountDao.setPrimary(platform, newPrimary.accountId)
                syncActiveCredentials(context, platform, newPrimary.accountId)
                Timber.i("[AccountManager] Promoted new primary $platform account: ${newPrimary.displayName}")
            } else {
                // No accounts left — clear active credentials
                clearActiveCredentials(context, platform)
                Timber.i("[AccountManager] No $platform accounts remaining, cleared active credentials")
            }
        }

        Timber.i("[AccountManager] Removed $platform account: ${account.displayName} ($accountId)")
    }

    /**
     * Set an account as primary (the one used by default for launches).
     */
    suspend fun setPrimaryAccount(context: Context, platform: String, accountId: String) {
        platformAccountDao.clearPrimaryForPlatform(platform)
        platformAccountDao.setPrimary(platform, accountId)
        syncActiveCredentials(context, platform, accountId)
        Timber.i("[AccountManager] Set primary $platform account: $accountId")
    }

    /**
     * Get the primary account for a platform.
     */
    suspend fun getPrimaryAccount(platform: String): PlatformAccount? =
        platformAccountDao.getPrimary(platform)

    fun getPrimaryAccountFlow(platform: String): Flow<PlatformAccount?> =
        platformAccountDao.getPrimaryFlow(platform)

    /**
     * Get all accounts for a platform.
     */
    suspend fun getAccounts(platform: String): List<PlatformAccount> =
        platformAccountDao.getByPlatform(platform)

    fun getAccountsFlow(platform: String): Flow<List<PlatformAccount>> =
        platformAccountDao.getByPlatformFlow(platform)

    // ── Per-game account preference ─────────────────────────────────────────

    /**
     * Get the account to use for a specific game. Falls back to primary if no preference set.
     */
    suspend fun getGameAccount(platform: String, gameId: String): PlatformAccount? {
        val pref = gameAccountPreferenceDao.getForGame(gameId)
        return if (pref != null) {
            platformAccountDao.getByAccountId(platform, pref.accountId)
                ?: platformAccountDao.getPrimary(platform)
        } else {
            platformAccountDao.getPrimary(platform)
        }
    }

    fun getGameAccountFlow(gameId: String): Flow<GameAccountPreference?> =
        gameAccountPreferenceDao.getForGameFlow(gameId)

    /**
     * Set which account to use for a specific game.
     */
    suspend fun setGameAccount(gameId: String, accountId: String, platform: String) {
        gameAccountPreferenceDao.insert(
            GameAccountPreference(
                gameId = gameId,
                accountId = accountId,
                platform = platform,
            )
        )
    }

    /**
     * Clear the per-game account preference (revert to primary).
     */
    suspend fun clearGameAccount(gameId: String) {
        gameAccountPreferenceDao.deleteForGame(gameId)
    }

    // ── Credential file management ──────────────────────────────────────────

    /**
     * Get the directory for a specific account's credentials.
     */
    private fun getAccountsDir(context: Context, platform: String, accountId: String): File {
        val platformDir = when (platform) {
            "STEAM" -> "steam"
            "GOG" -> "gog"
            "EPIC" -> "epic"
            "AMAZON" -> "amazon"
            else -> platform.lowercase()
        }
        return File(context.filesDir, "$platformDir/accounts/$accountId")
    }

    /**
     * Copy an account's credentials to the active (original single-account) location
     * for backward compatibility with existing service code.
     */
    suspend fun syncActiveCredentials(context: Context, platform: String, accountId: String) {
        val sourceDir = getAccountsDir(context, platform, accountId)
        val sourceFile = File(sourceDir, "credentials.json")
        if (!sourceFile.exists()) {
            Timber.w("[AccountManager] Source credentials not found: ${sourceFile.absolutePath}")
            return
        }

        when (platform) {
            "GOG" -> {
                // GOG expects {filesDir}/gog_auth.json with a specific structure
                val source = JSONObject(sourceFile.readText())
                val activeFile = File(context.filesDir, "gog_auth.json")
                activeFile.writeText(source.toString(2))
            }
            "EPIC" -> {
                // Epic expects {filesDir}/epic/credentials.json
                val epicDir = File(context.filesDir, "epic")
                epicDir.mkdirs()
                val activeFile = File(epicDir, "credentials.json")
                activeFile.writeText(sourceFile.readText())
            }
            "AMAZON" -> {
                // Amazon expects {filesDir}/amazon/credentials.json
                val amazonDir = File(context.filesDir, "amazon")
                amazonDir.mkdirs()
                val activeFile = File(amazonDir, "credentials.json")
                activeFile.writeText(sourceFile.readText())
            }
            "STEAM" -> {
                // Steam uses DataStore — we need to load credentials into PrefManager
                val source = JSONObject(sourceFile.readText())
                PrefManager.username = source.optString("username", "")
                PrefManager.accessToken = source.optString("access_token", "")
                PrefManager.refreshToken = source.optString("refresh_token", "")
                PrefManager.steamUserName = source.optString("steam_user_name", "")
                PrefManager.steamUserAvatarHash = source.optString("steam_user_avatar_hash", "")
                PrefManager.steamUserAccountId = source.optInt("steam_user_account_id", 0)
                PrefManager.steamUserSteamId64 = source.optLong("steam_user_steam_id_64", 0L)
                PrefManager.clientId = source.optLong("client_id", 0L)
            }
        }

        Timber.d("[AccountManager] Synced active credentials for $platform/$accountId")
    }

    /**
     * Save the current active Steam credentials to the account's storage.
     */
    suspend fun saveCurrentSteamCredentials(context: Context, accountId: String) {
        val accountsDir = getAccountsDir(context, "STEAM", accountId)
        accountsDir.mkdirs()
        val json = JSONObject().apply {
            put("username", PrefManager.username)
            put("access_token", PrefManager.accessToken)
            put("refresh_token", PrefManager.refreshToken)
            put("steam_user_name", PrefManager.steamUserName)
            put("steam_user_avatar_hash", PrefManager.steamUserAvatarHash)
            put("steam_user_account_id", PrefManager.steamUserAccountId)
            put("steam_user_steam_id_64", PrefManager.steamUserSteamId64)
            put("client_id", PrefManager.clientId ?: 0L)
        }
        File(accountsDir, "credentials.json").writeText(json.toString())
        Timber.d("[AccountManager] Saved current Steam credentials for account $accountId")
    }

    /**
     * Clear active credentials for a platform (when no accounts remain).
     */
    private suspend fun clearActiveCredentials(context: Context, platform: String) {
        when (platform) {
            "GOG" -> GOGAuthManager.clearStoredCredentials(context)
            "EPIC" -> EpicAuthManager.clearStoredCredentials(context)
            "AMAZON" -> AmazonAuthManager.clearStoredCredentials(context)
            "STEAM" -> PrefManager.clearSteamSessionPreferences()
        }
    }

    // ── Migration helpers ───────────────────────────────────────────────────

    /**
     * Migrate existing single-account credentials to the multi-account system.
     * Should be called once on app startup.
     */
    suspend fun migrateExistingCredentials(context: Context) {
        // Migrate GOG
        migrateGogCredentials(context)
        // Migrate Epic
        migrateEpicCredentials(context)
        // Migrate Amazon
        migrateAmazonCredentials(context)
        // Migrate Steam
        migrateSteamCredentials(context)
    }

    private suspend fun migrateGogCredentials(context: Context) {
        if (platformAccountDao.countForPlatform("GOG") > 0) return
        val authFile = File(context.filesDir, "gog_auth.json")
        if (!authFile.exists()) return

        try {
            val json = JSONObject(authFile.readText())
            val clientId = app.gamenative.service.gog.GOGConstants.GOG_CLIENT_ID
            if (!json.has(clientId)) return
            val creds = json.getJSONObject(clientId)
            val userId = creds.optString("user_id", "")
            if (userId.isEmpty()) return

            val accountsDir = getAccountsDir(context, "GOG", userId)
            accountsDir.mkdirs()
            File(accountsDir, "credentials.json").writeText(json.toString())

            platformAccountDao.insert(
                PlatformAccount(
                    platform = "GOG",
                    accountId = userId,
                    displayName = "GOG User",
                    isPrimary = true,
                    credentialsPath = accountsDir.absolutePath,
                )
            )
            Timber.i("[AccountManager] Migrated existing GOG credentials for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "[AccountManager] Failed to migrate GOG credentials")
        }
    }

    private suspend fun migrateEpicCredentials(context: Context) {
        if (platformAccountDao.countForPlatform("EPIC") > 0) return
        val credFile = File(context.filesDir, "epic/credentials.json")
        if (!credFile.exists()) return

        try {
            val json = JSONObject(credFile.readText())
            val accountId = json.optString("account_id", "")
            val displayName = json.optString("display_name", "Epic User")
            if (accountId.isEmpty()) return

            val accountsDir = getAccountsDir(context, "EPIC", accountId)
            accountsDir.mkdirs()
            File(accountsDir, "credentials.json").writeText(json.toString())

            platformAccountDao.insert(
                PlatformAccount(
                    platform = "EPIC",
                    accountId = accountId,
                    displayName = displayName,
                    isPrimary = true,
                    credentialsPath = accountsDir.absolutePath,
                )
            )
            Timber.i("[AccountManager] Migrated existing Epic credentials for user $displayName")
        } catch (e: Exception) {
            Timber.e(e, "[AccountManager] Failed to migrate Epic credentials")
        }
    }

    private suspend fun migrateAmazonCredentials(context: Context) {
        if (platformAccountDao.countForPlatform("AMAZON") > 0) return
        val credFile = File(context.filesDir, "amazon/credentials.json")
        if (!credFile.exists()) return

        try {
            val json = JSONObject(credFile.readText())
            val deviceSerial = json.optString("device_serial", "")
            if (deviceSerial.isEmpty()) return

            val accountsDir = getAccountsDir(context, "AMAZON", deviceSerial)
            accountsDir.mkdirs()
            File(accountsDir, "credentials.json").writeText(json.toString())

            platformAccountDao.insert(
                PlatformAccount(
                    platform = "AMAZON",
                    accountId = deviceSerial,
                    displayName = "Amazon Account",
                    isPrimary = true,
                    credentialsPath = accountsDir.absolutePath,
                )
            )
            Timber.i("[AccountManager] Migrated existing Amazon credentials")
        } catch (e: Exception) {
            Timber.e(e, "[AccountManager] Failed to migrate Amazon credentials")
        }
    }

    private suspend fun migrateSteamCredentials(context: Context) {
        if (platformAccountDao.countForPlatform("STEAM") > 0) return
        val username = PrefManager.username
        val refreshToken = PrefManager.refreshToken
        if (username.isEmpty() || refreshToken.isEmpty()) return

        try {
            val steamId64 = PrefManager.steamUserSteamId64
            val accountId = if (steamId64 != 0L) steamId64.toString() else username

            val accountsDir = getAccountsDir(context, "STEAM", accountId)
            accountsDir.mkdirs()

            val json = JSONObject().apply {
                put("username", username)
                put("access_token", PrefManager.accessToken)
                put("refresh_token", refreshToken)
                put("steam_user_name", PrefManager.steamUserName)
                put("steam_user_avatar_hash", PrefManager.steamUserAvatarHash)
                put("steam_user_account_id", PrefManager.steamUserAccountId)
                put("steam_user_steam_id_64", steamId64)
                put("client_id", PrefManager.clientId ?: 0L)
            }
            File(accountsDir, "credentials.json").writeText(json.toString())

            platformAccountDao.insert(
                PlatformAccount(
                    platform = "STEAM",
                    accountId = accountId,
                    displayName = PrefManager.steamUserName.ifEmpty { username },
                    avatarUrl = PrefManager.steamUserAvatarHash,
                    isPrimary = true,
                    credentialsPath = accountsDir.absolutePath,
                )
            )
            Timber.i("[AccountManager] Migrated existing Steam credentials for user $username")
        } catch (e: Exception) {
            Timber.e(e, "[AccountManager] Failed to migrate Steam credentials")
        }
    }

    companion object {
        /** Convert a GameSource enum to the platform string used by AccountManager. */
        fun platformFromGameSource(gameSource: GameSource): String = when (gameSource) {
            GameSource.STEAM -> "STEAM"
            GameSource.GOG -> "GOG"
            GameSource.EPIC -> "EPIC"
            GameSource.AMAZON -> "AMAZON"
            GameSource.CUSTOM_GAME -> ""
        }
    }
}
