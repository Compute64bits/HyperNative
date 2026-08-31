package app.gamenative.service.epic

import android.content.Context
import app.gamenative.data.EpicCredentials
import app.gamenative.data.EpicGameToken
import app.gamenative.utils.sanitizeForFilename
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Manages Epic Games authentication and account operations.
 */
object EpicAuthManager {

    // Denuvo ownership tokens are valid ~30 minutes and the endpoint is rate-limited
    // (~5 requests / 24h / game). Cache to disk and re-use a few minutes under the
    // server-side validity window to avoid burning the quota on relaunches.
    private const val OWNERSHIP_TOKEN_CACHE_TTL_MS = 25L * 60L * 1000L

    internal fun getCredentialsFilePath(context: Context): String {
        val dir = File(context.filesDir, "epic")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "credentials.json").absolutePath
    }

    private fun ownershipTokenCacheFile(context: Context, namespace: String, catalogItemId: String): File {
        val dir = File(context.filesDir, "epic/ownership_tokens").also { it.mkdirs() }
        return File(dir, "${namespace.sanitizeForFilename()}_${catalogItemId.sanitizeForFilename()}.hex")
    }

    private fun readCachedOwnershipTokenHex(context: Context, namespace: String, catalogItemId: String): String? {
        val file = ownershipTokenCacheFile(context, namespace, catalogItemId)
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() >= OWNERSHIP_TOKEN_CACHE_TTL_MS) return null
        return runCatching { file.readText().trim().takeIf { it.isNotEmpty() } }.getOrNull()
    }

    private fun writeOwnershipTokenHex(context: Context, namespace: String, catalogItemId: String, hex: String) {
        runCatching {
            ownershipTokenCacheFile(context, namespace, catalogItemId).writeText(hex)
        }.onFailure { Timber.tag("Epic").w(it, "Failed caching ownership token for $namespace:$catalogItemId") }
    }

    private fun clearOwnershipTokenCache(context: Context) {
        runCatching {
            File(context.filesDir, "epic/ownership_tokens").listFiles()?.forEach { it.delete() }
        }.onFailure { Timber.tag("Epic").w(it, "Failed clearing ownership token cache") }
    }


    fun hasStoredCredentials(context: Context): Boolean {
        val credentialsFile = File(getCredentialsFilePath(context))
        return credentialsFile.exists()
    }

        /**
         * Clear stored credentials (logout)
         */
        fun clearStoredCredentials(context: Context): Boolean {
            return try {
                clearOwnershipTokenCache(context)
                val authFile = File(getCredentialsFilePath(context))
                if (authFile.exists()) {
                    authFile.delete()
                } else {
                    true
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear Epic credentials")
                false
            }
        }

    /**
     * Extract authorization code from various input formats:
     * - Full URL: https://www.epicgames.com/id/api/redirect?code=abc123
     * - Just code: abc123
     */
    private fun extractCodeFromInput(input: String): String {
        val trimmed = input.trim()
        // Check if it's a URL with code parameter
        if (trimmed.startsWith("http")) {
            val codeMatch = Regex("[?&]code=([^&]+)").find(trimmed)
            return codeMatch?.groupValues?.get(1) ?: ""
        }
        // Otherwise assume it's already the code
        return trimmed
    }

    /**
     * Authenticate with Epic Games using authorization code from OAuth2 flow
     * Users must visit Epic login page, authenticate, and copy the authorization code
     *
     * @param context Android context
     * @param authorizationCode OAuth authorization code from Epic redirect
     * @return Result containing EpicCredentials on success, exception on failure
     */
    suspend fun authenticateWithCode(context: Context, authorizationCode: String): Result<EpicCredentials> {
        return try {
            Timber.i("Starting Epic authentication with authorization code...")

            // Extract the actual authorization code from URL if needed
            val actualCode = extractCodeFromInput(authorizationCode)
            if (actualCode.isEmpty()) {
                return Result.failure(Exception("Invalid authorization URL: no code parameter found"))
            }

            // Use native API client for authentication
            Timber.d("Authenticating via EpicAuthClient...")

            val authResult = EpicAuthClient.authenticateWithCode(actualCode)

            if (authResult.isFailure) {
                val error = authResult.exceptionOrNull()
                Timber.e(error, "Epic authentication failed: ${error?.message}")
                return Result.failure(error ?: Exception("Authentication failed"))
            }

            val authResponse = authResult.getOrNull()!!

            // Save credentials to file
            val credentials = EpicCredentials(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                accountId = authResponse.accountId,
                displayName = authResponse.displayName,
                expiresAt = authResponse.expiresAt
            )

            saveCredentials(context, credentials)

            Timber.i("Epic authentication successful: ${credentials.displayName}")
            Result.success(credentials)
        } catch (e: Exception) {
            Timber.e(e, "Epic authentication exception: ${e.message}")
            Result.failure(Exception("Authentication exception: ${e.message}", e))
        }
    }

    suspend fun getStoredCredentials(context: Context): Result<EpicCredentials> {
        return try {
            if (!hasStoredCredentials(context)) {
                return Result.failure(Exception("No stored credentials found"))
            }

            val credentials = loadCredentials(context)
            if (credentials == null) {
                return Result.failure(Exception("Failed to load credentials"))
            }

            refreshIfNeeded(context, credentials)
        } catch (e: Exception) {
            Timber.e(e, "Error getting Epic credentials: ${e.message}")
            Result.failure(Exception("Error getting credentials: ${e.message}", e))
        }
    }

    /**
     * Get credentials for a specific account ID, loading from the per-account store.
     * Falls back to the active credentials if the per-account file doesn't exist.
     * Handles token refresh automatically.
     */
    suspend fun getCredentialsForAccount(context: Context, accountId: String): Result<EpicCredentials> {
        return try {
            if (accountId.isEmpty()) {
                return getStoredCredentials(context)
            }

            val accountFile = File(context.filesDir, "epic/accounts/$accountId/credentials.json")
            val credentials = if (accountFile.exists()) {
                loadCredentialsFromFile(accountFile)
            } else {
                null
            }

            // If per-account file missing or unreadable, fall back to active credentials
            // (they may already belong to this account)
            val resolved = credentials ?: loadCredentials(context)
            if (resolved == null) {
                return Result.failure(Exception("No credentials found for account $accountId"))
            }

            refreshIfNeeded(context, resolved)
        } catch (e: Exception) {
            Timber.e(e, "Error getting Epic credentials for account $accountId: ${e.message}")
            Result.failure(Exception("Error getting credentials for account $accountId: ${e.message}", e))
        }
    }

    private suspend fun refreshIfNeeded(context: Context, credentials: EpicCredentials): Result<EpicCredentials> {
        val now = System.currentTimeMillis()
        val bufferMs = 5 * 60 * 1000 // 5 minutes

        if (now + bufferMs >= credentials.expiresAt) {
            Timber.d("Access token expired, refreshing...")

            val refreshResult = EpicAuthClient.refreshAccessToken(credentials.refreshToken)

            if (refreshResult.isFailure) {
                Timber.e("Failed to refresh token")
                return Result.failure(Exception("Failed to refresh expired token: ${refreshResult.exceptionOrNull()?.message}"))
            }

            val authResponse = refreshResult.getOrNull()!!
            val refreshedCredentials = EpicCredentials(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                accountId = authResponse.accountId,
                displayName = authResponse.displayName,
                expiresAt = authResponse.expiresAt
            )

            saveCredentials(context, refreshedCredentials)
            Timber.i("Access token refreshed successfully")

            return Result.success(refreshedCredentials)
        }

        return Result.success(credentials)
    }

    /**
     * Get game launch token for authenticating with Epic Games Services
     * This should be called immediately before launching a game that requires online authentication
     *
     */
    suspend fun getGameLaunchToken(
        context: Context,
        namespace: String? = null,
        catalogItemId: String? = null,
        requiresOwnershipToken: Boolean = false
    ): Result<EpicGameToken> {
        return try {
            // Get current valid credentials (will refresh if expired)
            val credentialsResult = getStoredCredentials(context)
            if (credentialsResult.isFailure) {
                return Result.failure(credentialsResult.exceptionOrNull() ?: Exception("Not authenticated"))
            }

            val credentials = credentialsResult.getOrNull()!!

            // Get game exchange token (required for all games)
            Timber.d("Getting game exchange token for launch...")
            val exchangeTokenResult = EpicAuthClient.getGameExchangeToken(credentials.accessToken)
            if (exchangeTokenResult.isFailure) {
                return Result.failure(exchangeTokenResult.exceptionOrNull() ?: Exception("Failed to get exchange token"))
            }
            val exchangeCode = exchangeTokenResult.getOrNull()!!

            // Get ownership token if required (for DRM-protected games)
            var ownershipTokenHex: String? = null
            if (requiresOwnershipToken) {
                if (namespace.isNullOrEmpty() || catalogItemId.isNullOrEmpty()) {
                    return Result.failure(Exception("Namespace and catalogItemId required for ownership token"))
                }

                val cachedHex = readCachedOwnershipTokenHex(context, namespace, catalogItemId)
                if (cachedHex != null) {
                    Timber.d("Using cached ownership token for $namespace:$catalogItemId")
                    ownershipTokenHex = cachedHex
                } else {
                    Timber.d("Getting ownership token for $namespace:$catalogItemId...")
                    val ownershipResult = EpicAuthClient.getOwnershipToken(
                        accessToken = credentials.accessToken,
                        accountId = credentials.accountId,
                        namespace = namespace,
                        catalogItemId = catalogItemId
                    )

                    if (ownershipResult.isFailure) {
                        val error = ownershipResult.exceptionOrNull()?.message ?: "Unknown error"
                        Timber.e("Failed to get required ownership token: $error")
                        return Result.failure(
                            Exception("Failed to get ownership token for DRM-protected game: $error")
                        )
                    } else {
                        // Convert binary token to hex string for easier handling
                        // Use toInt() and 0xFF to prevent sign extension of negative bytes
                        val tokenBytes = ownershipResult.getOrNull()!!
                        ownershipTokenHex = tokenBytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
                        writeOwnershipTokenHex(context, namespace, catalogItemId, ownershipTokenHex)
                        Timber.d("Ownership token obtained (${tokenBytes.size} bytes) and cached")
                    }
                }
            }

            val gameToken = EpicGameToken(
                authCode = exchangeCode,
                accountId = credentials.accountId,
                displayName = credentials.displayName,
                ownershipToken = ownershipTokenHex
            )

            Timber.i("Successfully obtained game launch token")
            Result.success(gameToken)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get game launch token")
            Result.failure(e)
        }
    }

    suspend fun logout(context: Context): Result<Unit> {
        return try {
            clearOwnershipTokenCache(context)
            val credentialsFile = File(getCredentialsFilePath(context))
            if (credentialsFile.exists()) {
                credentialsFile.delete()
                Timber.i("Epic credentials cleared")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear Epic credentials")
            Result.failure(e)
        }
    }

    private fun saveCredentials(context: Context, credentials: EpicCredentials) {
        val json = JSONObject().apply {
            put("access_token", credentials.accessToken)
            put("refresh_token", credentials.refreshToken)
            put("account_id", credentials.accountId)
            put("display_name", credentials.displayName)
            put("expires_at", credentials.expiresAt)
        }

        val content = json.toString()

        val file = File(getCredentialsFilePath(context))
        file.writeText(content)

        // Also update the per-account store so that switching accounts doesn't
        // restore stale (invalidated) refresh tokens.
        val accountFile = File(context.filesDir, "epic/accounts/${credentials.accountId}/credentials.json")
        if (accountFile.parentFile?.exists() == true) {
            accountFile.writeText(content)
        }

        Timber.d("Credentials saved to ${file.absolutePath}")
    }

    private fun loadCredentials(context: Context): EpicCredentials? {
        return loadCredentialsFromFile(File(getCredentialsFilePath(context)))
    }

    private fun loadCredentialsFromFile(file: File): EpicCredentials? {
        return try {
            if (!file.exists()) {
                return null
            }

            val json = JSONObject(file.readText())

            EpicCredentials(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                accountId = json.getString("account_id"),
                displayName = json.getString("display_name"),
                expiresAt = json.getLong("expires_at")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to load credentials from ${file.absolutePath}")
            null
        }
    }
}
