package app.gamenative.ui.screen.settings

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gamenative.PluviaApp
import app.gamenative.PrefManager
import app.gamenative.R
import app.gamenative.data.PlatformAccount
import app.gamenative.data.SteamFriend
import app.gamenative.enums.AppTheme
import app.gamenative.events.SteamEvent
import app.gamenative.service.SteamService
import app.gamenative.ui.component.GameStatsKey
import app.gamenative.ui.component.OptionListItem
import app.gamenative.ui.component.OptionRadioItem
import app.gamenative.ui.component.OptionSectionHeader
import app.gamenative.ui.enums.AppFilter
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.SteamIconImage
import app.gamenative.utils.getAvatarURL
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.PaletteStyle
import `in`.dragonbra.javasteam.enums.EPersonaState
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.EnumSet

@Composable
fun HomeSettingsScreen(
    appTheme: AppTheme,
    paletteStyle: PaletteStyle,
    onAppTheme: (AppTheme) -> Unit,
    onPaletteStyle: (PaletteStyle) -> Unit,
    gogLoggedIn: Boolean,
    epicLoggedIn: Boolean,
    amazonLoggedIn: Boolean,
    onGogLoginClick: () -> Unit,
    onEpicLoginClick: () -> Unit,
    onAmazonLoginClick: () -> Unit,
    onGogLogoutClick: (PlatformAccount) -> Unit = {},
    onEpicLogoutClick: (PlatformAccount) -> Unit = {},
    onAmazonLogoutClick: (PlatformAccount) -> Unit = {},
    onGogAddAccount: () -> Unit = {},
    onEpicAddAccount: () -> Unit = {},
    onAmazonAddAccount: () -> Unit = {},
    onSteamLoginClick: () -> Unit = {},
    onSteamLogoutClick: (PlatformAccount) -> Unit = {},
    onSteamAddAccount: () -> Unit = {},
    selectedFilters: EnumSet<AppFilter>,
    onFilterChanged: (AppFilter) -> Unit,
    currentView: PaneType,
    onViewChanged: (PaneType) -> Unit,
    isSteamConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountManager = PluviaApp.getInstance().accountManager

    val allAccounts by accountManager.allAccounts.collectAsStateWithLifecycle()
    val steamAccounts = allAccounts["STEAM"] ?: emptyList()
    val gogAccounts = allAccounts["GOG"] ?: emptyList()
    val epicAccounts = allAccounts["EPIC"] ?: emptyList()
    val amazonAccounts = allAccounts["AMAZON"] ?: emptyList()

    val steamPrimary = steamAccounts.firstOrNull { it.isPrimary }
    val gogPrimary = gogAccounts.firstOrNull { it.isPrimary }
    val epicPrimary = epicAccounts.firstOrNull { it.isPrimary }
    val amazonPrimary = amazonAccounts.firstOrNull { it.isPrimary }

    var persona by remember { mutableStateOf<SteamFriend?>(null) }
    var selectedStatus by remember(persona) { mutableStateOf(persona?.state ?: EPersonaState.Online) }
    var showStatusPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        persona = SteamService.instance?.localPersona?.value
        SteamService.userSteamId?.let {
            SteamService.requestUserPersona()
        }
    }

    DisposableEffect(true) {
        val onPersonaStateReceived: (SteamEvent.PersonaStateReceived) -> Unit = { event ->
            persona = event.persona
            selectedStatus = event.persona.state
        }
        PluviaApp.events.on<SteamEvent.PersonaStateReceived, Unit>(onPersonaStateReceived)
        onDispose {
            PluviaApp.events.off<SteamEvent.PersonaStateReceived, Unit>(onPersonaStateReceived)
        }
    }

    val colorOnline = PluviaTheme.colors.statusInstalled
    val colorAway = PluviaTheme.colors.statusAway
    val colorOffline = PluviaTheme.colors.statusOffline

    val getStatusColor: (EPersonaState) -> Color = { state ->
        when (state) {
            EPersonaState.Online -> colorOnline
            EPersonaState.Away -> colorAway
            EPersonaState.Invisible, EPersonaState.Offline -> colorOffline
            else -> colorOnline
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Profile Section
        SettingsSection(
            title = stringResource(R.string.settings_section_profile),
            icon = Icons.Default.Person,
            iconTint = PluviaTheme.colors.accentCyan,
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (SteamService.isLoggedIn) showStatusPicker = !showStatusPicker
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (persona?.avatarHash?.isNotEmpty() == true) {
                            SteamIconImage(
                                size = 48.dp,
                                image = { persona?.avatarHash?.getAvatarURL() },
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = persona?.name ?: stringResource(R.string.default_user_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(getStatusColor(selectedStatus), CircleShape),
                            )
                            Text(
                                text = when (selectedStatus) {
                                    EPersonaState.Online -> stringResource(R.string.status_online)
                                    EPersonaState.Away -> stringResource(R.string.status_away)
                                    EPersonaState.Invisible -> stringResource(R.string.status_invisible)
                                    else -> selectedStatus.name
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (SteamService.isLoggedIn) {
                        Icon(
                            imageVector = if (showStatusPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Status picker dropdown
                DropdownMenu(
                    expanded = showStatusPicker,
                    onDismissRequest = { showStatusPicker = false },
                    modifier = Modifier
                        .width(280.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.status),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        listOf(
                            Triple(EPersonaState.Online, stringResource(R.string.status_online), PluviaTheme.colors.statusInstalled),
                            Triple(EPersonaState.Away, stringResource(R.string.status_away), PluviaTheme.colors.statusAway),
                            Triple(EPersonaState.Invisible, stringResource(R.string.status_invisible), PluviaTheme.colors.statusOffline),
                        ).forEach { (state, label, color) ->
                            StatusOption(
                                text = label,
                                statusColor = color,
                                isSelected = selectedStatus == state,
                                onClick = {
                                    selectedStatus = state
                                    showStatusPicker = false
                                    scope.launch {
                                        SteamService.setPersonaState(state)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // Accounts Section
        SettingsSection(
            title = stringResource(R.string.settings_section_accounts),
            icon = Icons.Default.Person,
            iconTint = PluviaTheme.colors.accentPurple,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Steam
                AccountItem(
                    name = stringResource(R.string.accounts_steam),
                    isLoggedIn = SteamService.isLoggedIn,
                    accountDisplayName = steamPrimary?.displayName ?: PrefManager.steamUserName,
                    isPrimary = true,
                    accounts = steamAccounts,
                    onLoginClick = onSteamLoginClick,
                    onSetPrimary = { account ->
                        scope.launch {
                            accountManager.setPrimaryAccount(context, "STEAM", account.accountId)
                        }
                    },
                    onAddAccount = onSteamAddAccount,
                    onSwitchAccount = { account ->
                        scope.launch {
                            accountManager.setPrimaryAccount(context, "STEAM", account.accountId)
                        }
                    },
                    onLogoutClick = onSteamLogoutClick,
                )

                // GOG
                AccountItem(
                    name = stringResource(R.string.accounts_gog),
                    isLoggedIn = gogLoggedIn,
                    accountDisplayName = gogPrimary?.displayName ?: "",
                    isPrimary = gogPrimary != null,
                    accounts = gogAccounts,
                    onLoginClick = onGogLoginClick,
                    onSetPrimary = { account ->
                        scope.launch {
                            accountManager.setPrimaryAccount(context, "GOG", account.accountId)
                        }
                    },
                    onAddAccount = onGogAddAccount,
                    onSwitchAccount = { account ->
                        scope.launch {
                            accountManager.setPrimaryAccount(context, "GOG", account.accountId)
                        }
                    },
                    onLogoutClick = onGogLogoutClick,
                )

                // Epic
                AccountItem(
                    name = stringResource(R.string.accounts_epic),
                    isLoggedIn = epicLoggedIn,
                    accountDisplayName = epicPrimary?.displayName ?: "",
                    isPrimary = epicPrimary != null,
                    accounts = epicAccounts,
                    onLoginClick = onEpicLoginClick,
                    onSetPrimary = { account ->
                        scope.launch {
                            accountManager.setPrimaryAccount(context, "EPIC", account.accountId)
                        }
                    },
                    onAddAccount = onEpicAddAccount,
                    onSwitchAccount = { account ->
                        scope.launch {
                            accountManager.setPrimaryAccount(context, "EPIC", account.accountId)
                        }
                    },
                    onLogoutClick = onEpicLogoutClick,
                )

                // Amazon
                AccountItem(
                    name = stringResource(R.string.accounts_amazon),
                    isLoggedIn = amazonLoggedIn,
                    accountDisplayName = amazonPrimary?.displayName ?: "",
                    isPrimary = amazonPrimary != null,
                    accounts = amazonAccounts,
                    onLoginClick = onAmazonLoginClick,
                    onSetPrimary = { account ->
                        scope.launch {
                            accountManager.setPrimaryAccount(context, "AMAZON", account.accountId)
                        }
                    },
                    onAddAccount = onAmazonAddAccount,
                    onSwitchAccount = { account ->
                        scope.launch {
                            accountManager.setPrimaryAccount(context, "AMAZON", account.accountId)
                        }
                    },
                    onLogoutClick = onAmazonLogoutClick,
                )
            }
        }

        // Library Options Section
        SettingsSection(
            title = stringResource(R.string.settings_section_library_options),
            icon = Icons.Default.Settings,
            iconTint = PluviaTheme.colors.accentWarning,
        ) {
            // Game Stats Key
            GameStatsKey(modifier = Modifier.padding(horizontal = 8.dp))

            Spacer(modifier = Modifier.height(12.dp))

            // App Type
            OptionSectionHeader(
                text = stringResource(R.string.library_app_type),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AppFilter.entries.forEach { appFilter ->
                    if (appFilter in listOf(
                            AppFilter.GAME,
                            AppFilter.APPLICATION,
                            AppFilter.TOOL,
                            AppFilter.DEMO,
                        )
                    ) {
                        OptionListItem(
                            text = stringResource(appFilter.displayTextRes),
                            selected = selectedFilters.contains(appFilter),
                            onClick = { onFilterChanged(appFilter) },
                            icon = appFilter.icon,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Status
            OptionSectionHeader(
                text = stringResource(R.string.library_app_status),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AppFilter.entries.forEach { appFilter ->
                    if (appFilter in listOf(
                            AppFilter.INSTALLED,
                            AppFilter.SHARED,
                            AppFilter.COMPATIBLE,
                            AppFilter.EXPIRED,
                            AppFilter.PLAYABLE,
                            AppFilter.FIVE_STAR,
                            AppFilter.FIVE_STAR_GPU,
                            AppFilter.PROVEN_GPU,
                        )
                    ) {
                        OptionListItem(
                            text = stringResource(appFilter.displayTextRes),
                            selected = selectedFilters.contains(appFilter),
                            onClick = { onFilterChanged(appFilter) },
                            icon = appFilter.icon,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layout
            OptionSectionHeader(
                text = stringResource(R.string.library_layout_title),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                OptionRadioItem(
                    text = stringResource(R.string.library_layout_list),
                    selected = currentView == PaneType.LIST,
                    onClick = { onViewChanged(PaneType.LIST) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OptionRadioItem(
                    text = stringResource(R.string.library_layout_capsule),
                    selected = currentView == PaneType.GRID_CAPSULE,
                    onClick = { onViewChanged(PaneType.GRID_CAPSULE) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OptionRadioItem(
                    text = stringResource(R.string.library_layout_hero),
                    selected = currentView == PaneType.GRID_HERO,
                    onClick = { onViewChanged(PaneType.GRID_HERO) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OptionRadioItem(
                    text = stringResource(R.string.library_layout_carousel),
                    selected = currentView == PaneType.CAROUSEL,
                    onClick = { onViewChanged(PaneType.CAROUSEL) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Emulation section
        SettingsSection(
            title = stringResource(R.string.settings_emulation_title),
            icon = Icons.Default.Gamepad,
            iconTint = PluviaTheme.colors.accentCyan,
        ) {
            SettingsGroupEmulation()
        }

        // Performance section
        SettingsSection(
            title = stringResource(R.string.settings_performance_title),
            icon = Icons.Default.Speed,
            iconTint = PluviaTheme.colors.accentWarning,
        ) {
            SettingsGroupPerformance()
        }

        // Interface section
        SettingsSection(
            title = stringResource(R.string.settings_interface_title),
            icon = Icons.Default.Palette,
            iconTint = PluviaTheme.colors.accentPurple,
        ) {
            SettingsGroupInterface(
                appTheme = appTheme,
                paletteStyle = paletteStyle,
                onAppTheme = onAppTheme,
                onPaletteStyle = onPaletteStyle,
            )
        }

        // Info section
        SettingsSection(
            title = stringResource(R.string.settings_info_title),
            icon = Icons.Default.Info,
            iconTint = PluviaTheme.colors.accentSuccess,
        ) {
            SettingsGroupInfo()
        }

        // Debug section
        SettingsSection(
            title = stringResource(R.string.settings_debug_title),
            icon = Icons.Default.BugReport,
            iconTint = PluviaTheme.colors.accentWarning,
        ) {
            SettingsGroupDebug()
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AccountItem(
    name: String,
    isLoggedIn: Boolean,
    accountDisplayName: String = "",
    isPrimary: Boolean = false,
    accounts: List<PlatformAccount> = emptyList(),
    onLoginClick: () -> Unit,
    onSetPrimary: (PlatformAccount) -> Unit = {},
    onAddAccount: () -> Unit = {},
    onSwitchAccount: (PlatformAccount) -> Unit = {},
    onLogoutClick: (PlatformAccount) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var showMenu by remember { mutableStateOf(false) }
    var accountToLogout by remember { mutableStateOf<PlatformAccount?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "accountItemScale",
    )

    val backgroundColor = when {
        isFocused -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    if (accountToLogout != null) {
        AlertDialog(
            onDismissRequest = { accountToLogout = null },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.logout_confirm_message_specific,
                        accountToLogout!!.displayName,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val account = accountToLogout!!
                    accountToLogout = null
                    onLogoutClick(account)
                }) {
                    Text(stringResource(R.string.logout_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToLogout = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Box {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .selectable(
                    selected = isFocused,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (isLoggedIn) {
                            showMenu = true
                        } else {
                            onLoginClick()
                        }
                    },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (isLoggedIn) Icons.Default.Person else Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                if (isLoggedIn && accountDisplayName.isNotEmpty()) {
                    Text(
                        text = accountDisplayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isLoggedIn) {
                Icon(
                    imageVector = if (showMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Dropdown menu for logged-in accounts
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .width(300.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // List ALL accounts with star indicators
                accounts.forEach { account ->
                    val isCurrentPrimary = account.isPrimary
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrentPrimary) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                    if (account.accountId.isNotEmpty()) {
                                        Text(
                                            text = account.accountId,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                // Star icon: colored if primary, outline if not
                                Icon(
                                    imageVector = if (isCurrentPrimary) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = if (isCurrentPrimary) {
                                        stringResource(R.string.primary_account)
                                    } else {
                                        stringResource(R.string.set_as_primary)
                                    },
                                    tint = if (isCurrentPrimary) {
                                        PluviaTheme.colors.accentWarning
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .then(
                                            if (!isCurrentPrimary) {
                                                Modifier.clickable {
                                                    showMenu = false
                                                    onSetPrimary(account)
                                                }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onSwitchAccount(account)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isCurrentPrimary) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )

                // Add Account
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_account)) },
                    onClick = {
                        showMenu = false
                        onAddAccount()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )

                // Per-account logout
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.logout_account, account.displayName),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            showMenu = false
                            accountToLogout = account
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                    ),
                    color = Color.White,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = PluviaTheme.colors.borderDefault.copy(alpha = 0.2f),
            )
            content()
        }
    }
}

@Composable
private fun StatusOption(
    text: String,
    statusColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "statusOptionScale",
    )

    val backgroundColor = when {
        isFocused -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isFocused) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_SettingsScreen() {
    val isPreview = LocalInspectionMode.current
    if (!isPreview) {
        val context = LocalContext.current
        PrefManager.init(context)
    }
    PluviaTheme {
        HomeSettingsScreen(
            appTheme = AppTheme.DAY,
            paletteStyle = PaletteStyle.TonalSpot,
            onAppTheme = { },
            onPaletteStyle = { },
            gogLoggedIn = false,
            epicLoggedIn = false,
            amazonLoggedIn = false,
            onGogLoginClick = { },
            onEpicLoginClick = { },
            onAmazonLoginClick = { },
            selectedFilters = EnumSet.of(AppFilter.GAME),
            onFilterChanged = { },
            currentView = PaneType.GRID_CAPSULE,
            onViewChanged = { },
            isSteamConnected = true,
        )
    }
}
