package net.mypapit.mobile.myrepeater

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.data.location.LocationSource
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.data.sync.SyncState
import net.mypapit.mobile.myrepeater.ui.LocalNavAnimatedVisibilityScope
import net.mypapit.mobile.myrepeater.ui.LocalSharedTransitionScope
import net.mypapit.mobile.myrepeater.ui.MainViewModel
import net.mypapit.mobile.myrepeater.ui.callsign.CallsignScreen
import net.mypapit.mobile.myrepeater.ui.detail.RepeaterDetailScreen
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFilter
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListScreen
import net.mypapit.mobile.myrepeater.ui.map.MapScreen
import net.mypapit.mobile.myrepeater.ui.onboarding.OnboardingScreen
import net.mypapit.mobile.myrepeater.ui.settings.AboutScreen
import net.mypapit.mobile.myrepeater.ui.settings.PresetPickerScreen
import net.mypapit.mobile.myrepeater.ui.settings.SettingsScreen
import net.mypapit.mobile.myrepeater.ui.sync.SyncScreen
import net.mypapit.mobile.myrepeater.ui.theme.RepeaterTheme

private enum class Stage { LOADING, ONBOARDING, FIRST_SYNC, READY }

private const val CONTAINER_FADE_MS = 150

/** How long moving between screens takes. Short, so the app feels quick on a real phone. */
private const val NAV_MS = 150

private object Routes {
    const val LIST = "list"
    const val SETTINGS = "settings"
    const val PRESETS = "presets"
    const val MAP = "map"
    const val CALLSIGNS = "callsigns"
    const val ABOUT = "about"
    const val REPEATER = "repeater/{id}"
    val TOP_LEVEL = setOf(LIST, MAP, CALLSIGNS)

    fun repeater(id: Long) = "repeater/$id"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent),
        )
        super.onCreate(savedInstanceState)

        val container = (application as RepeaterApp).container
        setContent {
            RepeaterTheme {
                val viewModel: MainViewModel = viewModel { MainViewModel(container) }
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val dataReady by viewModel.dataReady.collectAsStateWithLifecycle()
                val syncState by viewModel.syncState.collectAsStateWithLifecycle()
                val locationGranted by viewModel.locationPermission.collectAsStateWithLifecycle()

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { viewModel.refreshLocationPermission() }
                val requestLocation = { permissionLauncher.launch(LocationSource.PERMISSIONS) }

                // The user may have changed the permission in system settings while away.
                LifecycleResumeEffect(Unit) {
                    viewModel.refreshLocationPermission()
                    onPauseOrDispose { }
                }

                val stage = when {
                    settings == null || dataReady == null -> Stage.LOADING
                    settings?.onboardingDone != true -> Stage.ONBOARDING
                    dataReady != true -> Stage.FIRST_SYNC
                    else -> Stage.READY
                }

                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Crossfade(targetState = stage, label = "stage") { current ->
                        when (current) {
                            Stage.LOADING -> Box(Modifier.fillMaxSize())
                            Stage.ONBOARDING -> OnboardingScreen(
                                locationGranted = locationGranted,
                                onRequestLocation = requestLocation,
                                onFinish = viewModel::completeOnboarding,
                            )
                            Stage.FIRST_SYNC -> SyncScreen(state = syncState, onRetry = viewModel::sync)
                            Stage.READY -> AppNavigation(viewModel, requestLocation, ::openAppSettings)
                        }
                    }
                }
            }
        }
    }

    private fun openAppSettings() {
        startActivity(
            Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AppNavigation(
    viewModel: MainViewModel,
    requestLocation: () -> Unit,
    openAppSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val locationGranted by viewModel.locationPermission.collectAsStateWithLifecycle()
    val current = settings ?: return

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val openTab = { target: String ->
        navController.navigate(target) {
            popUpTo(Routes.LIST) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openRepeater = { id: Long -> navController.navigate(Routes.repeater(id)) { launchSingleTop = true } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            // Only the top-level screens carry the bar; detail and settings screens are full-height.
            if (route == null || route in Routes.TOP_LEVEL) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == null || route == Routes.LIST,
                        onClick = { openTab(Routes.LIST) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_repeaters)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.MAP,
                        onClick = { openTab(Routes.MAP) },
                        icon = { Icon(Icons.Default.Place, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_map)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.CALLSIGNS,
                        onClick = { openTab(Routes.CALLSIGNS) },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_callsigns)) },
                    )
                }
            }
        },
    ) { barPadding ->
        // Lets a list card and its detail screen animate as one growing surface.
        SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.padding(barPadding).consumeWindowInsets(barPadding),
            // Tabs cross-fade; screens opened on top of a tab slide in from the side.
            enterTransition = {
                if (targetState.destination.route in Routes.TOP_LEVEL) fadeIn(tween(NAV_MS))
                else slideInHorizontally(tween(NAV_MS)) { it / 4 } + fadeIn(tween(NAV_MS))
            },
            exitTransition = { fadeOut(tween(NAV_MS)) },
            popEnterTransition = { fadeIn(tween(NAV_MS)) },
            popExitTransition = {
                if (initialState.destination.route in Routes.TOP_LEVEL) fadeOut(tween(NAV_MS))
                else slideOutHorizontally(tween(NAV_MS)) { it / 4 } + fadeOut(tween(NAV_MS))
            },
        ) {
            composable(Routes.LIST) {
                val items by viewModel.items.collectAsStateWithLifecycle()
                val all by viewModel.allRepeaters.collectAsStateWithLifecycle()
                val filter by viewModel.filter.collectAsStateWithLifecycle()
                val position by viewModel.position.collectAsStateWithLifecycle()
                val heading = viewModel.heading.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                RepeaterListScreen(
                    items = items,
                    totalCount = all.size,
                    filter = filter,
                    bands = remember(all) { RepeaterFilter.bandsIn(all) },
                    states = remember(all) { RepeaterFilter.statesIn(all) },
                    position = position,
                    waitingForLocation = current.locationMode == LocationMode.DEVICE && locationGranted && position == null,
                    heading = { heading.value },
                    refreshing = syncState is SyncState.Downloading || syncState is SyncState.Processing,
                    noticeDismissed = current.listNoticeDismissed,
                    onDismissNotice = viewModel::dismissListNotice,
                    onFilterChange = viewModel::setFilter,
                    onOpenRepeater = openRepeater,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                )
                }
            }
            composable(Routes.MAP) {
                val all by viewModel.allRepeaters.collectAsStateWithLifecycle()
                val position by viewModel.position.collectAsStateWithLifecycle()
                val focus by viewModel.mapFocus.collectAsStateWithLifecycle()
                val aprs by viewModel.aprs.collectAsStateWithLifecycle()
                MapScreen(
                    repeaters = all,
                    position = position,
                    aprs = aprs,
                    focusRequest = focus,
                    onFocusHandled = viewModel::clearMapFocus,
                    onOpenRepeater = openRepeater,
                )
            }
            composable(Routes.CALLSIGNS) {
                val query by viewModel.callsignQuery.collectAsStateWithLifecycle()
                val results by viewModel.callsignResults.collectAsStateWithLifecycle()
                CallsignScreen(query = query, state = results, onQueryChange = viewModel::setCallsignQuery)
            }
            composable(
                route = Routes.REPEATER,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                // No slide here: the card growing into the page is the whole transition.
                enterTransition = { fadeIn(tween(CONTAINER_FADE_MS)) },
                popExitTransition = { fadeOut(tween(CONTAINER_FADE_MS)) },
            ) { entry ->
                val id = entry.arguments?.getLong("id")
                val all by viewModel.allRepeaters.collectAsStateWithLifecycle()
                val position by viewModel.position.collectAsStateWithLifecycle()
                val heading = viewModel.heading.collectAsStateWithLifecycle()
                val repeater = all.firstOrNull { it.id == id }
                if (repeater != null) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    RepeaterDetailScreen(
                        repeater = repeater,
                        position = position,
                        heading = { heading.value },
                        onBack = { navController.popBackStack() },
                        onShowOnMap = {
                            val lat = repeater.latitude
                            val lon = repeater.longitude
                            if (lat != null && lon != null) {
                                viewModel.showOnMap(GeoPoint(lat, lon))
                                navController.popBackStack(Routes.LIST, inclusive = false)
                                openTab(Routes.MAP)
                            }
                        },
                    )
                    }
                } else if (all.isNotEmpty()) {
                    // A data refresh replaced this repeater's row while the screen was open.
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
            composable(Routes.SETTINGS) {
                val lastSync by viewModel.lastSync.collectAsStateWithLifecycle()
                val lastAttempt by viewModel.lastAttempt.collectAsStateWithLifecycle()
                SettingsScreen(
                    settings = current,
                    locationGranted = locationGranted,
                    presetName = viewModel.presets.firstOrNull { it.key == current.presetKey }?.name,
                    lastSync = lastSync,
                    lastSyncAttempt = lastAttempt,
                    syncState = syncState,
                    onBack = { navController.popBackStack() },
                    onUseDeviceLocation = viewModel::useDeviceLocation,
                    onRequestLocation = requestLocation,
                    onOpenSystemSettings = openAppSettings,
                    onChoosePreset = { navController.navigate(Routes.PRESETS) { launchSingleTop = true } },
                    onSync = viewModel::sync,
                    onAprsChange = viewModel::setAprsEnabled,
                    onOpenAbout = { navController.navigate(Routes.ABOUT) { launchSingleTop = true } },
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PRESETS) {
                PresetPickerScreen(
                    presets = viewModel.presets,
                    selectedKey = current.presetKey.takeIf { current.locationMode == LocationMode.PRESET },
                    onBack = { navController.popBackStack() },
                    onPick = {
                        viewModel.usePreset(it)
                        navController.popBackStack()
                    },
                )
            }
        }
        }
        }
    }
}
