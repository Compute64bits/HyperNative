package app.gamenative.ui.screen.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.gamenative.PrefManager
import app.gamenative.PluviaApp
import app.gamenative.R
import app.gamenative.data.LibraryItem
import app.gamenative.events.AndroidEvent
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.screen.library.components.RecommendationDisclosureDialog

@Composable
fun DiscoverScreen(
    onNavigate: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var recDisclosureShown by remember { mutableStateOf(PrefManager.recDisclosureShown) }

    Box(modifier = modifier.fillMaxSize()) {
        if (recDisclosureShown) {
            RecommendedTabPane(
                currentPaneType = PaneType.GRID_CAPSULE,
                onNavigate = onNavigate,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.gog_rec_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            RecommendationDisclosureDialog(
                onContinue = {
                    PrefManager.recDisclosureShown = true
                    recDisclosureShown = true
                    PluviaApp.events.emit(AndroidEvent.RecommendationToggleChanged)
                },
                onDismiss = { },
            )
        }
    }
}
