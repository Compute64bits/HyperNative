package app.gamenative.ui.enums

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import app.gamenative.R

/**
 * Destinations for Home Screen
 */
enum class HomeDestination(@StringRes val title: Int, val icon: ImageVector) {
    Discover(R.string.destination_discover, Icons.Rounded.Explore),
    Library(R.string.destination_my_games, Icons.Rounded.SportsEsports),
    Settings(R.string.destination_settings, Icons.Default.Settings),
    Downloads(R.string.destination_downloads, Icons.Filled.Download),
}
