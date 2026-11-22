package net.mypapit.mobile.myrepeater.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

private const val FADE_IN_MS = 70
private const val FADE_OUT_MS = 90

/** How long a card takes to grow into its page, or shrink back. */
private const val BOUNDS_MS = 200

/** The scope that links matching elements on two screens; null outside the navigation host. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The entering or leaving screen's own animation scope; null outside a navigation destination. */
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks a repeater's container. A list card and the detail screen carrying the
 * same [id] are animated as one surface: the card grows into the page when it
 * is opened and shrinks back when the page is closed.
 *
 * Does nothing where the scopes are not provided (previews, tests, or a screen
 * reached without a matching card, such as from the map).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.repeaterContainer(id: Long, shape: Shape): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        this@repeaterContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "repeater-$id"),
            animatedVisibilityScope = visibility,
            // Staggered, not crossed: the new content is fully in before the old starts to
            // leave, so the surface never turns see-through over the cards behind it.
            enter = fadeIn(tween(FADE_IN_MS)),
            boundsTransform = { _, _ -> tween(BOUNDS_MS) },
            exit = fadeOut(tween(FADE_OUT_MS, delayMillis = FADE_IN_MS)),
            // Lay the content out again at each size, so text is never stretched.
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}
