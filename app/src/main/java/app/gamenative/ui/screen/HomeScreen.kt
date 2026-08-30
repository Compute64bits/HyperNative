package app.gamenative.ui.screen

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.gamenative.PluviaApp
import app.gamenative.PrefManager
import app.gamenative.R
import app.gamenative.enums.AppTheme
import app.gamenative.ui.enums.HomeDestination
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.enums.SortOption
import app.gamenative.ui.enums.AppFilter
import app.gamenative.ui.model.HomeViewModel
import app.gamenative.ui.screen.auth.AmazonOAuthActivity
import app.gamenative.ui.screen.auth.EpicOAuthActivity
import app.gamenative.ui.screen.auth.GOGOAuthActivity
import app.gamenative.ui.screen.downloads.HomeDownloadsScreen
import app.gamenative.ui.screen.library.DiscoverScreen
import app.gamenative.ui.screen.library.HomeLibraryScreen
import app.gamenative.ui.screen.settings.HomeSettingsScreen
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.SnackbarManager
import app.gamenative.utils.PlatformOAuthHandlers
import app.gamenative.service.gog.GOGAuthManager
import app.gamenative.service.epic.EpicAuthManager
import app.gamenative.service.amazon.AmazonAuthManager
import com.materialkolor.PaletteStyle
import java.util.EnumSet
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onChat: (Long) -> Unit,
    onClickExit: () -> Unit,
    onClickPlay: (String, Boolean) -> Unit,
    onTestGraphics: (String) -> Unit,
    onPlayWithDiagnostics: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onGoOnline: () -> Unit,
    isOffline: Boolean = false,
    isSteamConnected: Boolean = false,
    appTheme: AppTheme = AppTheme.AUTO,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    onAppTheme: (AppTheme) -> Unit = {},
    onPaletteStyle: (PaletteStyle) -> Unit = {},
) {
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    val accountManager = PluviaApp.getInstance().accountManager

    val gogOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(GOGOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.gog_login_cancel)
            SnackbarManager.show(message)
            return@rememberLauncherForActivityResult
        }
        val code = result.data?.getStringExtra(GOGOAuthActivity.EXTRA_AUTH_CODE)
        if (code == null) {
            val message = result.data?.getStringExtra(GOGOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.gog_login_cancel)
            SnackbarManager.show(message)
            return@rememberLauncherForActivityResult
        }
        lifecycleScope.launch {
            PlatformOAuthHandlers.handleGogAuthentication(
                context = context,
                authCode = code,
                coroutineScope = lifecycleScope,
                onLoadingChange = { },
                onError = { msg -> if (msg != null) SnackbarManager.show(msg) },
                onSuccess = { SnackbarManager.show(context.getString(R.string.gog_login_success_title)) },
                onDialogClose = { },
            )
        }
    }

    val epicOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(EpicOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.epic_login_cancel)
            SnackbarManager.show(message)
            return@rememberLauncherForActivityResult
        }
        val code = result.data?.getStringExtra(EpicOAuthActivity.EXTRA_AUTH_CODE)
        if (code == null) {
            val message = result.data?.getStringExtra(EpicOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.epic_login_cancel)
            SnackbarManager.show(message)
            return@rememberLauncherForActivityResult
        }
        lifecycleScope.launch {
            PlatformOAuthHandlers.handleEpicAuthentication(
                context = context,
                authCode = code,
                coroutineScope = lifecycleScope,
                onLoadingChange = { },
                onError = { msg -> if (msg != null) SnackbarManager.show(msg) },
                onSuccess = { SnackbarManager.show(context.getString(R.string.epic_login_success_title)) },
                onDialogClose = { },
            )
        }
    }

    val amazonOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(AmazonOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.amazon_login_cancel)
            SnackbarManager.show(message)
            return@rememberLauncherForActivityResult
        }
        val code = result.data?.getStringExtra(AmazonOAuthActivity.EXTRA_AUTH_CODE)
        if (code == null) {
            val message = result.data?.getStringExtra(AmazonOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.amazon_login_cancel)
            SnackbarManager.show(message)
            return@rememberLauncherForActivityResult
        }
        lifecycleScope.launch {
            PlatformOAuthHandlers.handleAmazonAuthentication(
                context = context,
                authCode = code,
                coroutineScope = lifecycleScope,
                onLoadingChange = { },
                onError = { msg -> if (msg != null) SnackbarManager.show(msg) },
                onSuccess = { SnackbarManager.show(context.getString(R.string.amazon_login_success_title)) },
                onDialogClose = { },
            )
        }
    }

    var currentSortOption by remember { mutableStateOf(PrefManager.librarySortOption) }
    var selectedFilters by remember { mutableStateOf(PrefManager.libraryFilter) }
    var currentView by remember { mutableStateOf(PrefManager.libraryLayout) }

    val gogLoggedIn = GOGAuthManager.hasStoredCredentials(context)
    val epicLoggedIn = EpicAuthManager.hasStoredCredentials(context)
    val amazonLoggedIn = AmazonAuthManager.hasStoredCredentials(context)

    BackHandler {
        if (homeState.currentDestination != HomeDestination.Library) {
            viewModel.onDestination(HomeDestination.Library)
        } else {
            onClickExit()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (homeState.currentDestination) {
                HomeDestination.Discover -> DiscoverScreen(
                    onNavigate = { item ->
                        viewModel.onDestination(HomeDestination.Library)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars),
                )

                HomeDestination.Library -> HomeLibraryScreen(
                    onClickPlay = onClickPlay,
                    onTestGraphics = onTestGraphics,
                    onPlayWithDiagnostics = onPlayWithDiagnostics,
                    onNavigateRoute = onNavigateRoute,
                    onLogout = onLogout,
                    onGoOnline = onGoOnline,
                    onDownloadsClick = { viewModel.onDestination(HomeDestination.Downloads) },
                    isOffline = isOffline,
                    isSteamConnected = isSteamConnected,
                    onGogLoginClick = { gogOAuthLauncher.launch(Intent(context, GOGOAuthActivity::class.java)) },
                    onEpicLoginClick = { epicOAuthLauncher.launch(Intent(context, EpicOAuthActivity::class.java)) },
                    onAmazonLoginClick = { amazonOAuthLauncher.launch(Intent(context, AmazonOAuthActivity::class.java)) },
                )

                HomeDestination.Settings -> HomeSettingsScreen(
                    appTheme = appTheme,
                    paletteStyle = paletteStyle,
                    onAppTheme = onAppTheme,
                    onPaletteStyle = onPaletteStyle,
                    gogLoggedIn = gogLoggedIn,
                    epicLoggedIn = epicLoggedIn,
                    amazonLoggedIn = amazonLoggedIn,
                    onGogLoginClick = { gogOAuthLauncher.launch(Intent(context, GOGOAuthActivity::class.java)) },
                    onEpicLoginClick = { epicOAuthLauncher.launch(Intent(context, EpicOAuthActivity::class.java)) },
                    onAmazonLoginClick = { amazonOAuthLauncher.launch(Intent(context, AmazonOAuthActivity::class.java)) },
                    onGogLogoutClick = { account ->
                        lifecycleScope.launch {
                            accountManager.removeAccount(context, "GOG", account.accountId)
                            SnackbarManager.show(context.getString(R.string.gog_logout_success))
                        }
                    },
                    onEpicLogoutClick = { account ->
                        lifecycleScope.launch {
                            accountManager.removeAccount(context, "EPIC", account.accountId)
                            SnackbarManager.show(context.getString(R.string.epic_logout_success))
                        }
                    },
                    onAmazonLogoutClick = { account ->
                        lifecycleScope.launch {
                            accountManager.removeAccount(context, "AMAZON", account.accountId)
                            SnackbarManager.show(context.getString(R.string.amazon_logout_success))
                        }
                    },
                    onGogAddAccount = { gogOAuthLauncher.launch(Intent(context, GOGOAuthActivity::class.java)) },
                    onEpicAddAccount = { epicOAuthLauncher.launch(Intent(context, EpicOAuthActivity::class.java)) },
                    onAmazonAddAccount = { amazonOAuthLauncher.launch(Intent(context, AmazonOAuthActivity::class.java)) },
                    selectedFilters = selectedFilters,
                    onFilterChanged = { filter ->
                        val newFilters = EnumSet.copyOf(selectedFilters as EnumSet<AppFilter>)
                        if (newFilters.contains(filter)) newFilters.remove(filter) else newFilters.add(filter)
                        selectedFilters = newFilters
                        PrefManager.libraryFilter = newFilters
                    },
                    currentSortOption = currentSortOption,
                    onSortOptionChanged = { option ->
                        currentSortOption = option
                        PrefManager.librarySortOption = option
                    },
                    currentView = currentView,
                    onViewChanged = { view ->
                        currentView = view
                        PrefManager.libraryLayout = view
                    },
                    isSteamConnected = isSteamConnected,
                    modifier = Modifier.fillMaxSize(),
                )

                HomeDestination.Downloads -> HomeDownloadsScreen(
                    onBack = { viewModel.onDestination(HomeDestination.Library) },
                    onClickPlay = onClickPlay,
                    onTestGraphics = onTestGraphics,
                    onPlayWithDiagnostics = onPlayWithDiagnostics,
                )
            }
        }

        if (homeState.currentDestination != HomeDestination.Downloads && !PluviaApp.isAmbientDownloadActive) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                listOf(
                    HomeDestination.Discover,
                    HomeDestination.Library,
                    HomeDestination.Settings,
                ).forEach { destination ->
                    NavigationBarItem(
                        selected = homeState.currentDestination == destination,
                        onClick = { viewModel.onDestination(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.title),
                            )
                        },
                        label = { Text(stringResource(destination.title)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=1080px,height=1920px,dpi=440,orientation=landscape",
)
@Composable
private fun Preview_HomeScreenContent() {
    PluviaTheme {
        HomeScreen(
            onChat = {},
            onClickPlay = { _, _ -> },
            onTestGraphics = { },
            onPlayWithDiagnostics = { },
            onLogout = {},
            onNavigateRoute = {},
            onClickExit = {},
            onGoOnline = {},
        )
    }
}
