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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.mypapit.mobile.myrepeater.data.location.LocationSource
import net.mypapit.mobile.myrepeater.data.settings.LocationMode
import net.mypapit.mobile.myrepeater.data.sync.SyncState
import net.mypapit.mobile.myrepeater.ui.MainViewModel
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListScreen
import net.mypapit.mobile.myrepeater.ui.onboarding.OnboardingScreen
import net.mypapit.mobile.myrepeater.ui.settings.PresetPickerScreen
import net.mypapit.mobile.myrepeater.ui.settings.SettingsScreen
import net.mypapit.mobile.myrepeater.ui.sync.SyncScreen
import net.mypapit.mobile.myrepeater.ui.theme.RepeaterTheme

private enum class Stage { LOADING, ONBOARDING, FIRST_SYNC, READY }

private object Routes {
    const val LIST = "list"
    const val SETTINGS = "settings"
    const val PRESETS = "presets"
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

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            val items by viewModel.items.collectAsStateWithLifecycle()
            val position by viewModel.position.collectAsStateWithLifecycle()
            val heading = viewModel.heading.collectAsStateWithLifecycle()
            RepeaterListScreen(
                items = items,
                position = position,
                waitingForLocation = current.locationMode == LocationMode.DEVICE && locationGranted && position == null,
                heading = { heading.value },
                refreshing = syncState is SyncState.Downloading || syncState is SyncState.Processing,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable(Routes.SETTINGS) {
            val lastSync by viewModel.lastSync.collectAsStateWithLifecycle()
            SettingsScreen(
                settings = current,
                locationGranted = locationGranted,
                presetName = viewModel.presets.firstOrNull { it.key == current.presetKey }?.name,
                lastSync = lastSync,
                syncState = syncState,
                onBack = { navController.popBackStack() },
                onUseDeviceLocation = viewModel::useDeviceLocation,
                onRequestLocation = requestLocation,
                onOpenSystemSettings = openAppSettings,
                onChoosePreset = { navController.navigate(Routes.PRESETS) { launchSingleTop = true } },
                onSync = viewModel::sync,
            )
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
