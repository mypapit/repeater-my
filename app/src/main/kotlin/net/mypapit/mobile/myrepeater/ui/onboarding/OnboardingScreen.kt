package net.mypapit.mobile.myrepeater.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.ui.theme.AppLogo

private const val PAGE_COUNT = 5
private const val LOCATION_PAGE = 3

/**
 * Five pages: welcome, ways to find a repeater, experimental features (Android Auto
 * and APRS station viewer), why location helps (with the permission request), and
 * what the app uses plus the privacy promise.
 */
@Composable
fun OnboardingScreen(
    locationGranted: Boolean,
    onRequestLocation: () -> Unit,
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == PAGE_COUNT - 1

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        0 -> WelcomePage()
                        1 -> ModesPage()
                        2 -> ExperimentalPage()
                        LOCATION_PAGE -> LocationPage(locationGranted, onRequestLocation)
                        else -> PermissionsPage()
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (!lastPage) {
                        TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
                    }
                }
                PageDots(current = pagerState.currentPage)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = {
                            if (lastPage) onFinish()
                            else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                    ) {
                        Text(stringResource(if (lastPage) R.string.onboarding_done else R.string.onboarding_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AppLogo(size = 128.dp)
    }
    Spacer(Modifier.height(28.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_body),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ModesPage() {
    PageTitle(R.string.onboarding_modes_title)
    Feature(Icons.Default.LocationOn, R.string.onboarding_modes_nearest_title, R.string.onboarding_modes_nearest_body)
    Feature(Icons.Default.Place, R.string.onboarding_modes_map_title, R.string.onboarding_modes_map_body)
    Feature(Icons.Default.Search, R.string.onboarding_modes_search_title, R.string.onboarding_modes_search_body)
    Feature(Icons.Default.Star, R.string.onboarding_modes_offline_title, R.string.onboarding_modes_offline_body)
}

@Composable
private fun ExperimentalPage() {
    PageTitle(R.string.onboarding_experimental_title)
    Feature(
        R.drawable.ic_onboarding_car,
        R.string.onboarding_experimental_android_auto_title,
        R.string.onboarding_experimental_android_auto_body,
    )
    Feature(
        R.drawable.ic_onboarding_aprs,
        R.string.onboarding_experimental_aprs_title,
        R.string.onboarding_experimental_aprs_body,
    )
}

@Composable
private fun LocationPage(granted: Boolean, onRequest: () -> Unit) {
    PageTitle(R.string.onboarding_location_title)
    Text(
        text = stringResource(R.string.onboarding_location_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(28.dp))
    if (granted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.onboarding_location_enabled),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    } else {
        FilledTonalButton(onClick = onRequest) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.onboarding_location_enable))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_location_alternative),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionsPage() {
    PageTitle(R.string.onboarding_permissions_title)
    Feature(
        Icons.Default.Refresh,
        R.string.onboarding_permissions_internet_title,
        R.string.onboarding_permissions_internet_body,
    )
    Feature(
        Icons.Default.LocationOn,
        R.string.onboarding_permissions_location_title,
        R.string.onboarding_permissions_location_body,
    )
    Feature(
        Icons.Default.Lock,
        R.string.onboarding_permissions_privacy_title,
        R.string.onboarding_permissions_privacy_body,
    )
}

@Composable
private fun PageTitle(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Feature(icon: ImageVector, title: Int, body: Int) {
    Feature(rememberVectorPainter(icon), title, body)
}

@Composable
private fun Feature(@DrawableRes iconRes: Int, title: Int, body: Int) {
    Feature(painterResource(iconRes), title, body)
}

@Composable
private fun Feature(painter: Painter, title: Int, body: Int) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PageDots(current: Int) {
    val description = stringResource(R.string.onboarding_page, current + 1, PAGE_COUNT)
    Row(
        modifier = Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PAGE_COUNT) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 20.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
        }
    }
}
