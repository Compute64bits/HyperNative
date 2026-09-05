package app.gamenative.ui.component.settings

import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.gamenative.R
import com.alorma.compose.settings.ui.base.internal.LocalSettingsGroupEnabled
import com.alorma.compose.settings.ui.base.internal.SettingsTileColors
import com.alorma.compose.settings.ui.base.internal.SettingsTileDefaults
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold
import kotlin.math.roundToInt

private const val HEADER_PHONE = "__header_phone__"
private const val HEADER_STANDARD = "__header_standard__"

data class ScreenSizeItem(val label: String, val isHeader: Boolean)

fun buildScreenSizeCategories(context: android.content.Context): List<ScreenSizeItem> {
    val items = mutableListOf<ScreenSizeItem>()

    items.add(ScreenSizeItem(context.getString(R.string.screen_size_custom), isHeader = false))

    items.add(ScreenSizeItem(HEADER_PHONE, isHeader = true))
    val phoneRes = getPhoneResolution(context)
    if (phoneRes != null) {
        val (w, h) = phoneRes
        items.add(ScreenSizeItem("${w}x${h}", isHeader = false))
        items.add(ScreenSizeItem("${(w / 2f).roundToInt()}x${(h / 2f).roundToInt()} /2", isHeader = false))
        items.add(ScreenSizeItem("${(w / 3f).roundToInt()}x${(h / 3f).roundToInt()} /3", isHeader = false))
        items.add(ScreenSizeItem("${(w / 4f).roundToInt()}x${(h / 4f).roundToInt()} /4", isHeader = false))
    }

    items.add(ScreenSizeItem(HEADER_STANDARD, isHeader = true))
    items.add(ScreenSizeItem("640x480 (4:3)", isHeader = false))
    items.add(ScreenSizeItem("800x600 (4:3)", isHeader = false))
    items.add(ScreenSizeItem("854x480 (16:9)", isHeader = false))
    items.add(ScreenSizeItem("960x540 (16:9)", isHeader = false))
    items.add(ScreenSizeItem("1024x768 (4:3)", isHeader = false))
    items.add(ScreenSizeItem("1280x720 (16:9)", isHeader = false))
    items.add(ScreenSizeItem("1280x800 (16:10)", isHeader = false))
    items.add(ScreenSizeItem("1280x960 (4:3)", isHeader = false))
    items.add(ScreenSizeItem("1280x1024 (5:4)", isHeader = false))
    items.add(ScreenSizeItem("1366x768 (16:9)", isHeader = false))
    items.add(ScreenSizeItem("1440x900 (16:10)", isHeader = false))
    items.add(ScreenSizeItem("1600x900 (16:9)", isHeader = false))
    items.add(ScreenSizeItem("1920x1080 (16:9)", isHeader = false))

    return items
}

private fun getPhoneResolution(context: android.content.Context): Pair<Int, Int>? {
    return try {
        val displayManager = context.getSystemService(android.content.Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return null

        val width: Int
        val height: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mode = display.mode
            width = mode.physicalWidth
            height = mode.physicalHeight
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(displayMetrics)
            width = displayMetrics.widthPixels
            height = displayMetrics.heightPixels
        }

        val landscapeW = maxOf(width, height)
        val landscapeH = minOf(width, height)
        Pair(landscapeW, landscapeH)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ScreenSizeDropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = LocalSettingsGroupEnabled.current,
    value: Int,
    items: List<ScreenSizeItem>,
    onItemSelected: (Int) -> Unit,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    colors: SettingsTileColors = SettingsTileDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
    action: @Composable (() -> Unit)? = null,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val selectableItems = items.filter { !it.isHeader }
    val selectedText = if (value >= 0 && value < selectableItems.size) {
        selectableItems[value].label
    } else {
        ""
    }

    val focusRequester = remember { FocusRequester() }
    val phoneCategoryLabel = stringResource(R.string.screen_size_category_phone)
    val standardCategoryLabel = stringResource(R.string.screen_size_category_standard)

    SettingsTileScaffold(
        modifier = Modifier
            .focusRequester(focusRequester)
            .clickable(
                enabled = enabled,
                onClick = { isDropdownExpanded = true },
            )
            .then(modifier),
        enabled = enabled,
        title = title,
        subtitle = {
            if (subtitle != null) {
                Column {
                    ProvideTextStyle(value = LocalTextStyle.current.merge(TextStyle(fontStyle = FontStyle.Italic))) {
                        subtitle()
                    }
                    Text(
                        text = selectedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Text(
                    text = selectedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontWeight = FontWeight.Bold),
                )
            }
        },
        icon = icon,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = {
                isDropdownExpanded = false
                focusRequester.requestFocus()
            },
        ) {
            var selectableIndex = 0
            items.forEach { item ->
                if (item.isHeader) {
                    val headerText = when (item.label) {
                        HEADER_PHONE -> phoneCategoryLabel
                        HEADER_STANDARD -> standardCategoryLabel
                        else -> item.label
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = headerText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    val currentIndex = selectableIndex
                    val isSelected = currentIndex == value
                    DropdownMenuItem(
                        enabled = enabled,
                        text = {
                            Text(
                                text = item.label,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onItemSelected(currentIndex)
                            isDropdownExpanded = false
                            focusRequester.requestFocus()
                        },
                    )
                    selectableIndex++
                }
            }
        }
        Row {
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                imageVector = if (isDropdownExpanded) {
                    Icons.Filled.ArrowDropUp
                } else {
                    Icons.Filled.ArrowDropDown
                },
                contentDescription = "Dropdown arrow",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (action != null) {
                Spacer(modifier.width(16.dp))
                action()
            }
        }
    }
}
