package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DetectorViewModel
import com.example.ui.DrawerDestination
import com.example.ui.components.FilterMarkersDialog
import com.example.ui.components.FullScreenOsmMap
import com.example.ui.screens.DetectMockScreen
import com.example.ui.screens.MarkersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerGray
import com.example.ui.theme.DrawerHighlightGray
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.MockDetectorTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#1976D2")),
        )
        setContent {
            MockDetectorTheme {
                MockDetectorApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockDetectorApp(
    viewModel: DetectorViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDrawerOpen by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onPermissionResult()
    }

    val exportMarkersLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = viewModel.exportMarkersJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_markers_exported),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val importMarkersLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val count = runCatching {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                viewModel.importMarkersJson(content)
            }.getOrDefault(0)
            Toast.makeText(
                context,
                if (count > 0) {
                    context.getString(R.string.toast_markers_imported, count)
                } else {
                    context.getString(R.string.toast_import_failed)
                },
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        val perms = buildList {
            if (!uiState.hasLocationPermission) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    BackHandler(enabled = isDrawerOpen || uiState.destination != DrawerDestination.MAP) {
        if (isDrawerOpen) {
            isDrawerOpen = false
        } else {
            viewModel.navigateTo(DrawerDestination.MAP)
        }
    }

    val openDeveloperOptions = {
        val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        val launched = runCatching { context.startActivity(devIntent) }.isSuccess
        if (!launched) {
            val fallback = runCatching {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }.isSuccess
            if (!fallback) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_dev_options_unavailable),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val copyReport = {
        val report = viewModel.buildDiagnosticReport()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Mapku Report", report))
        Toast.makeText(
            context,
            context.getString(R.string.toast_report_copied),
            Toast.LENGTH_SHORT,
        ).show()
    }

    val startMockAction = {
        val started = viewModel.startMockGps()
        if (started) {
            Toast.makeText(
                context,
                context.getString(R.string.mock_started_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val stopMockAction = {
        viewModel.stopMockGps()
        Toast.makeText(
            context,
            context.getString(R.string.mock_stopped_toast),
            Toast.LENGTH_SHORT,
        ).show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color.White,
            topBar = {
                val titleText = when (uiState.destination) {
                    DrawerDestination.MAP -> stringResource(R.string.app_name)
                    DrawerDestination.MARKERS -> stringResource(R.string.nav_markers)
                    DrawerDestination.SETTINGS -> stringResource(R.string.nav_settings)
                    DrawerDestination.DETECT_MOCK -> stringResource(R.string.nav_detect_mock)
                }

                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrandBlue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                    navigationIcon = {
                        if (uiState.destination == DrawerDestination.MAP) {
                            IconButton(
                                onClick = { isDrawerOpen = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("topbar_menu_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = stringResource(R.string.cd_open_drawer),
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.navigateTo(DrawerDestination.MAP) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("topbar_back_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        if (uiState.destination == DrawerDestination.MAP) {
                            IconButton(
                                onClick = startMockAction,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("topbar_play_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(R.string.action_start_monitoring),
                                    modifier = Modifier.alpha(if (uiState.isMockServiceActive) 0.5f else 1f),
                                )
                            }
                            IconButton(
                                onClick = stopMockAction,
                                enabled = uiState.isMockServiceActive || uiState.isLiveMonitoring,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("topbar_stop_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = stringResource(R.string.action_stop_monitoring),
                                    modifier = Modifier.alpha(if (uiState.isMockServiceActive) 1f else 0.7f),
                                )
                            }
                            IconButton(
                                onClick = { showFilterDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("topbar_filter_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = stringResource(R.string.action_filter),
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("topbar_overflow_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = stringResource(R.string.action_more_options),
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    containerColor = Color.White,
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.nav_detect_mock)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.navigateTo(DrawerDestination.DETECT_MOCK)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_add_favorite)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.addCurrentPinToFavorites()
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_favorite_saved),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_copy_coords)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            val coords = String.format(
                                                Locale.US,
                                                "%.6f, %.6f",
                                                uiState.selectedLatitude,
                                                uiState.selectedLongitude,
                                            )
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cb?.setPrimaryClip(ClipData.newPlainText("Coordinates", coords))
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_coords_copied),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.osm_open_web)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            val url = String.format(
                                                Locale.US,
                                                "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=16/%.6f/%.6f",
                                                uiState.selectedLatitude,
                                                uiState.selectedLongitude,
                                                uiState.selectedLatitude,
                                                uiState.selectedLongitude,
                                            )
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (uiState.destination) {
                    DrawerDestination.MAP -> FullScreenOsmMap(
                        uiState = uiState,
                        onMapTap = { lat, lon ->
                            viewModel.selectMapCoordinate(lat, lon, recordHistory = true)
                        },
                        onMyLocationClick = {
                            if (!uiState.hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            } else {
                                viewModel.pollLocationFix(centerMap = true)
                            }
                        },
                        onFabToggleLive = {
                            if (uiState.isMockServiceActive) {
                                stopMockAction()
                            } else {
                                startMockAction()
                            }
                        },
                        onAlertBadgeClick = {
                            viewModel.navigateTo(DrawerDestination.DETECT_MOCK)
                        },
                    )

                    DrawerDestination.MARKERS -> MarkersScreen(
                        markers = uiState.savedMarkers,
                        onSelectMarker = viewModel::jumpToMarker,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onDeleteMarker = viewModel::deleteMarker,
                    )

                    DrawerDestination.SETTINGS -> SettingsScreen(
                        settings = uiState.settings,
                        onUpdateSettings = viewModel::updateSettings,
                        onClearHistory = {
                            viewModel.clearHistoryMarkers()
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_history_cleared),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onExportMarkers = {
                            exportMarkersLauncher.launch("mapku-markers.json")
                        },
                        onImportMarkers = {
                            importMarkersLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                    )

                    DrawerDestination.DETECT_MOCK -> DetectMockScreen(
                        uiState = uiState,
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        },
                        onPollFix = { viewModel.pollLocationFix(centerMap = false) },
                        onRescanPackages = viewModel::refreshPackageAudit,
                        onOpenDevOptions = openDeveloperOptions,
                        onCopyReport = copyReport,
                        onSearchQueryChange = viewModel::updateAppSearchQuery,
                        onToggleShowOnlyFlagged = viewModel::toggleShowOnlyFlagged,
                    )
                }
            }
        }

        // Scrim outside menu: tapping anywhere outside closes the menu
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(180)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isDrawerOpen = false },
                    )
                    .testTag("drawer_scrim"),
            )
        }

        // Slide-in drawer panel (no swipe-to-open gesture)
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(
                animationSpec = tween(220),
                initialOffsetX = { -it },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(180),
                targetOffsetX = { -it },
            ),
        ) {
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .width(290.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .testTag("drawer_panel"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandBlue)
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_custom_clean),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    DrawerRow(
                        icon = Icons.Rounded.Map,
                        label = stringResource(R.string.nav_map),
                        selected = uiState.destination == DrawerDestination.MAP,
                        onClick = {
                            viewModel.navigateTo(DrawerDestination.MAP)
                            isDrawerOpen = false
                        },
                        testTag = "drawer_map",
                    )
                    DrawerRow(
                        icon = Icons.Rounded.LocationOn,
                        label = stringResource(R.string.nav_markers),
                        selected = uiState.destination == DrawerDestination.MARKERS,
                        onClick = {
                            viewModel.navigateTo(DrawerDestination.MARKERS)
                            isDrawerOpen = false
                        },
                        testTag = "drawer_markers",
                    )

                    HorizontalDivider(
                        color = DividerGray,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )

                    Text(
                        text = stringResource(R.string.nav_other_options),
                        style = MaterialTheme.typography.titleSmall,
                        color = InkSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    DrawerRow(
                        icon = Icons.Rounded.Settings,
                        label = stringResource(R.string.nav_settings),
                        selected = uiState.destination == DrawerDestination.SETTINGS,
                        onClick = {
                            viewModel.navigateTo(DrawerDestination.SETTINGS)
                            isDrawerOpen = false
                        },
                        testTag = "drawer_settings",
                    )
                    DrawerRow(
                        icon = Icons.Rounded.Security,
                        label = stringResource(R.string.nav_detect_mock),
                        selected = uiState.destination == DrawerDestination.DETECT_MOCK,
                        onClick = {
                            viewModel.navigateTo(DrawerDestination.DETECT_MOCK)
                            isDrawerOpen = false
                        },
                        testTag = "drawer_detect_mock",
                    )
                    DrawerRow(
                        icon = Icons.Rounded.Settings,
                        label = stringResource(R.string.nav_dev_settings),
                        selected = false,
                        onClick = {
                            isDrawerOpen = false
                            openDeveloperOptions()
                        },
                        testTag = "drawer_dev_settings",
                    )
                    DrawerRow(
                        icon = Icons.Rounded.Help,
                        label = stringResource(R.string.nav_help),
                        selected = false,
                        onClick = {
                            isDrawerOpen = false
                            showHelpDialog = true
                        },
                        testTag = "drawer_help",
                    )
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterMarkersDialog(
            initialFilter = uiState.markerFilter,
            onDismiss = { showFilterDialog = false },
            onConfirm = { showHistory, showFavorites ->
                viewModel.applyMarkerFilter(showHistory, showFavorites)
                showFilterDialog = false
            },
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = Color.White,
            title = { Text(text = stringResource(R.string.help_title)) },
            text = { Text(text = stringResource(R.string.help_body)) },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(text = stringResource(R.string.dialog_ok), color = BrandBlue)
                }
            },
        )
    }

    if (uiState.showSelectMockAppDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSelectMockAppDialog() },
            containerColor = Color.White,
            title = { Text(text = stringResource(R.string.mock_required_title)) },
            text = { Text(text = stringResource(R.string.mock_required_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissSelectMockAppDialog()
                        openDeveloperOptions()
                    },
                    modifier = Modifier.testTag("mock_open_dev_options_button"),
                ) {
                    Text(text = stringResource(R.string.action_open_dev_options), color = BrandBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSelectMockAppDialog() }) {
                    Text(text = stringResource(R.string.dialog_cancel), color = BrandBlue)
                }
            },
        )
    }
}

@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(if (selected) DrawerHighlightGray else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) BrandBlue else InkSecondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) BrandBlue else InkPrimary,
        )
    }
}
