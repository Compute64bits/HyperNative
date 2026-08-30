package app.gamenative.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.data.PlatformAccount

/**
 * A compact account display that shows the current account name.
 * When multiple accounts are available, it becomes a clickable dropdown to switch accounts.
 * Designed to be placed near the play button in the game detail screen.
 */
@Composable
fun AccountSelector(
    currentAccount: PlatformAccount?,
    allAccounts: List<PlatformAccount>,
    onAccountSelected: (PlatformAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasMultipleAccounts = allAccounts.size > 1
    var expanded by remember { mutableStateOf(false) }

    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentAccount?.displayName ?: stringResource(R.string.select_account),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
            if (currentAccount?.isPrimary == true) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(R.string.primary_account),
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (hasMultipleAccounts) {
        TextButton(
            onClick = { expanded = true },
            modifier = modifier,
        ) {
            content()
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            allAccounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.displayName,
                                fontWeight = if (account.accountId == currentAccount?.accountId) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                                maxLines = 1,
                            )
                        }
                    },
                    onClick = {
                        onAccountSelected(account)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (account.isPrimary) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = if (account.isPrimary) {
                                stringResource(R.string.primary_account)
                            } else {
                                null
                            },
                            modifier = Modifier.size(16.dp),
                            tint = if (account.isPrimary) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        )
                    },
                )
            }
        }
    } else {
        // Single account — display name only, not clickable
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}
