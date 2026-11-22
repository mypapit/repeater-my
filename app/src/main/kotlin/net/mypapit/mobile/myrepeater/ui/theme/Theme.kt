package net.mypapit.mobile.myrepeater.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The orange of the original app's text, kept as the brand accent. */
val SignalOrange = Color(0xFFFF8500)
val SkyBlue = Color(0xFF33B5E5)
val OnAirGreen = Color(0xFF6DD58C)
val MixedBandPurple = Color(0xFFC9A0FF)

/** Marks APRS stations on the map; kept apart from every repeater colour. */
val AprsPink = Color(0xFFFF5FA2)
val OffAirRed = Color(0xFFFF6B5E)

private val DarkColors = darkColorScheme(
    primary = SignalOrange,
    onPrimary = Color(0xFF2A1500),
    primaryContainer = Color(0xFF5C3000),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = SkyBlue,
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF0E4A63),
    onSecondaryContainer = Color(0xFFC3E8FA),
    tertiary = OnAirGreen,
    background = Color(0xFF121110),
    onBackground = Color(0xFFECE0D8),
    surface = Color(0xFF121110),
    onSurface = Color(0xFFECE0D8),
    surfaceVariant = Color(0xFF2B2623),
    onSurfaceVariant = Color(0xFFB8ABA1),
    surfaceContainerLowest = Color(0xFF0D0C0B),
    surfaceContainerLow = Color(0xFF1A1816),
    surfaceContainer = Color(0xFF1F1C1A),
    surfaceContainerHigh = Color(0xFF292523),
    surfaceContainerHighest = Color(0xFF34302D),
    outline = Color(0xFF85776D),
    outlineVariant = Color(0xFF41392F),
    error = OffAirRed,
)

/** The app is dark-only by design, matching the original and saving battery on the trail. */
@Composable
fun RepeaterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
