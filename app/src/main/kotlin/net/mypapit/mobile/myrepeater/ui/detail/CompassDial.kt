package net.mypapit.mobile.myrepeater.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.ui.theme.OffAirRed

private const val TICK_COUNT = 72
private const val TICK_STEP_DEGREES = 360f / TICK_COUNT

/**
 * A compass card that turns with the phone, with an arrow that stays aimed at
 * the repeater.
 *
 * @param bearing direction of the repeater in degrees clockwise from true north
 * @param heading where the top of the phone points; read while drawing, so a
 *   turning phone redraws the dial without recomposing the screen
 */
@Composable
fun CompassDial(bearing: Float, heading: () -> Float?, modifier: Modifier = Modifier) {
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(R.string.repeater_direction_description)

    Box(
        modifier = modifier.size(184.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // The card: ring, ticks and cardinal letters, rotated against the phone's heading.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -(heading() ?: 0f) }
                .drawBehind {
                    val radius = size.minDimension / 2
                    drawCircle(ringColor, radius - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                    for (i in 0 until TICK_COUNT) {
                        val cardinal = i % (TICK_COUNT / 4) == 0
                        val major = i % (TICK_COUNT / 12) == 0
                        val length = when {
                            cardinal -> 12.dp
                            major -> 8.dp
                            else -> 4.dp
                        }.toPx()
                        rotate(i * TICK_STEP_DEGREES) {
                            drawLine(
                                color = if (major) tickColor else ringColor,
                                start = Offset(center.x, 4.dp.toPx()),
                                end = Offset(center.x, 4.dp.toPx() + length),
                                strokeWidth = (if (cardinal) 2.dp else 1.dp).toPx(),
                            )
                        }
                    }
                },
        ) {
            CardinalLetter("N", Alignment.TopCenter, north = true)
            CardinalLetter("E", Alignment.CenterEnd)
            CardinalLetter("S", Alignment.BottomCenter)
            CardinalLetter("W", Alignment.CenterStart)
        }

        Icon(
            painter = painterResource(R.drawable.ic_navigation),
            contentDescription = null,
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer { rotationZ = bearing - (heading() ?: 0f) },
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.CardinalLetter(
    letter: String,
    alignment: Alignment,
    north: Boolean = false,
) {
    Text(
        text = letter,
        modifier = Modifier.align(alignment).padding(20.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (north) OffAirRed else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
