package app.gamenative.utils

import android.content.Context
import app.gamenative.PluviaApp
import app.gamenative.service.amazon.AmazonAuthManager
import app.gamenative.service.amazon.AmazonService
import app.gamenative.service.epic.EpicAuthManager
import app.gamenative.service.epic.EpicService
import app.gamenative.service.gog.GOGAuthManager
import app.gamenative.service.gog.GOGConstants
import app.gamenative.service.gog.GOGService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File

object PlatformOAuthHandlers {

    suspend fun handleGogAuthentication(
        context: Context,
        authCode: String,
        coroutineScope: CoroutineScope,
        onLoadingChange: (Boolean) -> Unit,
        onError: (String?) -> Unit,
        onSuccess: (Int) -> Unit,
        onDialogClose: () -> Unit,
    ) {
        onLoadingChange(true)
        onError(null)

        try {
            Timber.d("[PlatformOAuth]: Starting GOG authentication...")
            val result = GOGService.authenticateWithCode(context, authCode)

            if (result.isSuccess) {
                Timber.i("[PlatformOAuth]: GOG authentication successful!")
                // Register account in multi-account system
                registerGogAccount(context)
                GOGService.start(context)
                GOGService.triggerLibrarySync(context)
                onSuccess(0)
                onLoadingChange(false)
                onDialogClose()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Authentication failed"
                Timber.e("[PlatformOAuth]: GOG authentication failed: $error")
                onLoadingChange(false)
                onError(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "[PlatformOAuth]: GOG authentication exception: ${e.message}")
            onLoadingChange(false)
            onError(e.message ?: "Authentication failed")
        }
    }

    suspend fun handleEpicAuthentication(
        context: Context,
        authCode: String,
        coroutineScope: CoroutineScope,
        onLoadingChange: (Boolean) -> Unit,
        onError: (String?) -> Unit,
        onSuccess: () -> Unit,
        onDialogClose: () -> Unit,
    ) {
        onLoadingChange(true)
        onError(null)

        try {
            Timber.d("[PlatformOAuth]: Starting Epic authentication...")
            val result = EpicService.authenticateWithCode(context, authCode)

            if (result.isSuccess) {
                Timber.i("[PlatformOAuth]: Epic authentication successful!")
                // Register account in multi-account system
                registerEpicAccount(context)
                EpicService.start(context)
                EpicService.triggerLibrarySync(context)
                onSuccess()
                onLoadingChange(false)
                onDialogClose()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Authentication failed"
                Timber.e("[PlatformOAuth]: Epic authentication failed: $error")
                onLoadingChange(false)
                onError(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "[PlatformOAuth]: Epic authentication exception: ${e.message}")
            onLoadingChange(false)
            onError(e.message ?: "Authentication failed")
        }
    }

    suspend fun handleAmazonAuthentication(
        context: Context,
        authCode: String,
        coroutineScope: CoroutineScope,
        onLoadingChange: (Boolean) -> Unit,
        onError: (String?) -> Unit,
        onSuccess: () -> Unit,
        onDialogClose: () -> Unit,
    ) {
        onLoadingChange(true)
        onError(null)

        try {
            Timber.d("[PlatformOAuth]: Starting Amazon authentication...")
            val result = AmazonService.authenticateWithCode(context, authCode)

            if (result.isSuccess) {
                Timber.i("[PlatformOAuth]: Amazon authentication successful!")
                // Register account in multi-account system
                registerAmazonAccount(context)
                AmazonService.start(context)
                AmazonService.triggerLibrarySync(context)
                onSuccess()
                onLoadingChange(false)
                onDialogClose()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Authentication failed"
                Timber.e("[PlatformOAuth]: Amazon authentication failed: $error")
                onLoadingChange(false)
                onError(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "[PlatformOAuth]: Amazon authentication exception: ${e.message}")
            onLoadingChange(false)
            onError(e.message ?: "Authentication failed")
        }
    }

    // ── Account registration helpers ────────────────────────────────────────

    private suspend fun registerGogAccount(context: Context) {
        try {
            val accountManager = PluviaApp.getInstance().accountManager
            val authFile = File(GOGAuthManager.getAuthConfigPath(context))
            if (!authFile.exists()) return
            val json = JSONObject(authFile.readText())
            val creds = json.optJSONObject(GOGConstants.GOG_CLIENT_ID) ?: return
            val userId = creds.optString("user_id", "")
            if (userId.isEmpty()) return

            accountManager.addAccount(
                context = context,
                platform = "GOG",
                accountId = userId,
                displayName = "GOG User",
                credentialsJson = json,
            )
        } catch (e: Exception) {
            Timber.e(e, "[PlatformOAuth] Failed to register GOG account")
        }
    }

    private suspend fun registerEpicAccount(context: Context) {
        try {
            val accountManager = PluviaApp.getInstance().accountManager
            val credFile = File(EpicAuthManager.getCredentialsFilePath(context))
            if (!credFile.exists()) return
            val json = JSONObject(credFile.readText())
            val accountId = json.optString("account_id", "")
            val displayName = json.optString("display_name", "Epic User")
            if (accountId.isEmpty()) return

            accountManager.addAccount(
                context = context,
                platform = "EPIC",
                accountId = accountId,
                displayName = displayName,
                credentialsJson = json,
            )
        } catch (e: Exception) {
            Timber.e(e, "[PlatformOAuth] Failed to register Epic account")
        }
    }

    private suspend fun registerAmazonAccount(context: Context) {
        try {
            val accountManager = PluviaApp.getInstance().accountManager
            val credFile = File(AmazonAuthManager.getCredentialsFilePath(context))
            if (!credFile.exists()) return
            val json = JSONObject(credFile.readText())
            val deviceSerial = json.optString("device_serial", "")
            if (deviceSerial.isEmpty()) return

            accountManager.addAccount(
                context = context,
                platform = "AMAZON",
                accountId = deviceSerial,
                displayName = "Amazon Account",
                credentialsJson = json,
            )
        } catch (e: Exception) {
            Timber.e(e, "[PlatformOAuth] Failed to register Amazon account")
        }
    }
}
