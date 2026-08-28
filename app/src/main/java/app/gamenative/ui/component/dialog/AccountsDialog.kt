package app.gamenative.ui.component.dialog

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.PluviaApp
import app.gamenative.R
import app.gamenative.data.PlatformAccount
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.SteamIconImage
import app.gamenative.utils.getAvatarURL
import kotlinx.coroutines.launch

/**
 * Dialog that shows all accounts for a given platform.
 * Allows adding, removing, and selecting the primary account.
 */
@Composable
fun AccountsDialog(
    openDialog: Boolean,
    platform: String,
    accounts: List<PlatformAccount>,
    onDismiss: () -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (PlatformAccount) -> Unit,
    onSetPrimary: (PlatformAccount) -> Unit,
) {
    if (!openDialog) return

    val scope = rememberCoroutineScope()
    var accountToRemove by remember { mutableStateOf<PlatformAccount?>(null) }

    val platformTitle = when (platform) {
        "STEAM" -> stringResource(R.string.accounts_steam)
        "GOG" -> stringResource(R.string.accounts_gog)
        "EPIC" -> stringResource(R.string.accounts_epic)
        "AMAZON" -> stringResource(R.string.accounts_amazon)
        else -> platform
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = platformTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (accounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_accounts_connected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(accounts, key = { it.id }) { account ->
                            AccountListItem(
                                account = account,
                                onSetPrimary = { onSetPrimary(account) },
                                onRemove = { accountToRemove = account },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Account button
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onAddAccount()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.add_account))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        },
    )

    // Confirmation dialog for account removal
    accountToRemove?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text(stringResource(R.string.remove_account)) },
            text = {
                Text(
                    stringResource(
                        R.string.remove_account_confirmation,
                        account.displayName,
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveAccount(account)
                        accountToRemove = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.remove),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AccountListItem(
    account: PlatformAccount,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
        leadingContent = {
            if (account.avatarUrl.isNotEmpty() && account.platform == "STEAM") {
                SteamIconImage(
                    size = 40.dp,
                    image = { account.avatarUrl.getAvatarURL() },
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        headlineContent = {
            Text(
                text = account.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (account.isPrimary) FontWeight.Bold else FontWeight.Normal,
            )
        },
        supportingContent = {
            if (account.isPrimary) {
                Text(
                    text = stringResource(R.string.primary_account),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Star button to set as primary
                IconButton(
                    onClick = onSetPrimary,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (account.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(R.string.set_as_primary),
                        tint = if (account.isPrimary) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Delete button (only show if not the only account or not primary)
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.remove_account),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_AccountsDialog() {
    PluviaTheme {
        Surface {
            AccountsDialog(
                openDialog = true,
                platform = "STEAM",
                accounts = listOf(
                    PlatformAccount(
                        id = 1,
                        platform = "STEAM",
                        accountId = "76561198000000001",
                        displayName = "Player One",
                        isPrimary = true,
                        credentialsPath = "/tmp/1",
                    ),
                    PlatformAccount(
                        id = 2,
                        platform = "STEAM",
                        accountId = "76561198000000002",
                        displayName = "Player Two",
                        isPrimary = false,
                        credentialsPath = "/tmp/2",
                    ),
                ),
                onDismiss = {},
                onAddAccount = {},
                onRemoveAccount = {},
                onSetPrimary = {},
            )
        }
    }
}
