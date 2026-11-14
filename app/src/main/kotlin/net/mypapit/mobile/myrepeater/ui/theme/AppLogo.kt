package net.mypapit.mobile.myrepeater.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import net.mypapit.mobile.myrepeater.R

/**
 * The launcher icon drawn inside the app: both adaptive-icon layers, cropped
 * to the middle two-thirds the way a launcher crops them.
 */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    val layerSize = size * 1.5f
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.21f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.requiredSize(layerSize),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(layerSize),
        )
    }
}
