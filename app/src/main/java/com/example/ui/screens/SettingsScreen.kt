package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.AppSettings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerGray
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary

private enum class NumericSettingKind {
    ACCURACY,
    ALTITUDE,
    BEARING,
    UPDATE_INTERVAL,
    MAX_DISTANCE,
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onClearHistory: () -> Unit,
    onExportMarkers: () -> Unit,
    onImportMarkers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingSetting by remember { mutableStateOf<NumericSettingKind?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // GPS Settings
        item {
            SettingsSectionHeader(title = stringResource(R.string.settings_section_gps))
            SettingsActionRow(
                icon = Icons.Rounded.GpsFixed,
                title = stringResource(R.string.settings_accuracy),
                subtitle = "${stringResource(R.string.settings_accuracy_sub)} (${settings.accuracyMeters.toInt()} m)",
                onClick = { editingSetting = NumericSettingKind.ACCURACY },
                testTag = "setting_accuracy",
            )
            SettingsActionRow(
                icon = Icons.Outlined.Landscape,
                title = stringResource(R.string.settings_altitude),
                subtitle = "${stringResource(R.string.settings_altitude_sub)} (${settings.altitudeMeters.toInt()} m)",
                onClick = { editingSetting = NumericSettingKind.ALTITUDE },
                testTag = "setting_altitude",
            )
            SettingsActionRow(
                icon = Icons.Outlined.Explore,
                title = stringResource(R.string.settings_bearing),
                subtitle = "${stringResource(R.string.settings_bearing_sub)} (${settings.bearingDegrees.toInt()}°)",
                onClick = { editingSetting = NumericSettingKind.BEARING },
                testTag = "setting_bearing",
            )
            SettingsActionRow(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.settings_update_interval),
                subtitle = "${stringResource(R.string.settings_update_interval_sub)} (${settings.updateIntervalMs} ms)",
                onClick = { editingSetting = NumericSettingKind.UPDATE_INTERVAL },
                testTag = "setting_update_interval",
            )
            HorizontalDivider(color = DividerGray)
        }

        // Movement Settings
        item {
            SettingsSectionHeader(title = stringResource(R.string.settings_section_movement))
            SettingsCheckboxRow(
                icon = Icons.Rounded.LocationSearching,
                title = stringResource(R.string.settings_movement_sim),
                subtitle = stringResource(R.string.settings_movement_sim_sub),
                checked = settings.movementSimulation,
                onCheckedChange = { checked ->
                    onUpdateSettings { it.copy(movementSimulation = checked) }
                },
                testTag = "setting_movement_sim",
            )
            SettingsActionRow(
                icon = Icons.Rounded.CompareArrows,
                title = stringResource(R.string.settings_max_distance),
                subtitle = "${stringResource(R.string.settings_max_distance_sub)} (${settings.maxDistanceMovementMeters.toInt()} m)",
                enabled = settings.movementSimulation,
                onClick = { editingSetting = NumericSettingKind.MAX_DISTANCE },
                testTag = "setting_max_distance",
            )
            HorizontalDivider(color = DividerGray)
        }

        // Appearance
        item {
            SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))
            SettingsCheckboxRow(
                icon = Icons.Outlined.NotificationsActive,
                title = stringResource(R.string.settings_show_notification),
                subtitle = stringResource(R.string.settings_show_notification_sub),
                checked = settings.showNotificationBar,
                onCheckedChange = { checked ->
                    onUpdateSettings { it.copy(showNotificationBar = checked) }
                },
                testTag = "setting_show_notification",
            )
            SettingsCheckboxRow(
                icon = Icons.Rounded.FitScreen,
                title = stringResource(R.string.settings_auto_zoom),
                subtitle = stringResource(R.string.settings_auto_zoom_sub),
                checked = settings.autoZoomMarkerGroup,
                onCheckedChange = { checked ->
                    onUpdateSettings { it.copy(autoZoomMarkerGroup = checked) }
                },
                testTag = "setting_auto_zoom",
            )
            HorizontalDivider(color = DividerGray)
        }

        // Reset
        item {
            SettingsSectionHeader(title = stringResource(R.string.settings_section_reset))
            SettingsActionRow(
                icon = Icons.Outlined.DeleteOutline,
                title = stringResource(R.string.settings_clear_history),
                subtitle = stringResource(R.string.settings_clear_history_sub),
                onClick = { showClearConfirm = true },
                testTag = "setting_clear_history",
            )
            HorizontalDivider(color = DividerGray)
        }

        // Export/Import
        item {
            SettingsSectionHeader(title = stringResource(R.string.settings_section_export_import))
            SettingsActionRow(
                icon = Icons.Rounded.NoteAdd,
                title = stringResource(R.string.settings_export_markers),
                subtitle = stringResource(R.string.settings_export_markers_sub),
                onClick = onExportMarkers,
                testTag = "setting_export_markers",
            )
            SettingsActionRow(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.settings_import_markers),
                subtitle = stringResource(R.string.settings_import_markers_sub),
                onClick = onImportMarkers,
                testTag = "setting_import_markers",
            )
            HorizontalDivider(color = DividerGray)
        }

        // Information
        item {
            SettingsSectionHeader(title = stringResource(R.string.settings_section_info))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_app_version),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
            }
        }
    }

    editingSetting?.let { kind ->
        val (title, currentVal) = when (kind) {
            NumericSettingKind.ACCURACY -> stringResource(R.string.settings_accuracy) to settings.accuracyMeters.toString()
            NumericSettingKind.ALTITUDE -> stringResource(R.string.settings_altitude) to settings.altitudeMeters.toString()
            NumericSettingKind.BEARING -> stringResource(R.string.settings_bearing) to settings.bearingDegrees.toString()
            NumericSettingKind.UPDATE_INTERVAL -> stringResource(R.string.settings_update_interval) to settings.updateIntervalMs.toString()
            NumericSettingKind.MAX_DISTANCE -> stringResource(R.string.settings_max_distance) to settings.maxDistanceMovementMeters.toString()
        }
        var input by remember(kind) { mutableStateOf(currentVal) }

        AlertDialog(
            onDismissRequest = { editingSetting = null },
            containerColor = Color.White,
            title = { Text(text = title, color = InkPrimary) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = input.toFloatOrNull()
                        if (parsed != null && parsed >= 0f) {
                            when (kind) {
                                NumericSettingKind.ACCURACY -> onUpdateSettings { it.copy(accuracyMeters = parsed) }
                                NumericSettingKind.ALTITUDE -> onUpdateSettings { it.copy(altitudeMeters = parsed) }
                                NumericSettingKind.BEARING -> onUpdateSettings { it.copy(bearingDegrees = parsed.coerceIn(0f, 360f)) }
                                NumericSettingKind.UPDATE_INTERVAL -> onUpdateSettings {
                                    it.copy(updateIntervalMs = parsed.toLong().coerceIn(250L, 60_000L))
                                }
                                NumericSettingKind.MAX_DISTANCE -> onUpdateSettings { it.copy(maxDistanceMovementMeters = parsed) }
                            }
                        }
                        editingSetting = null
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_ok), color = BrandBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSetting = null }) {
                    Text(text = stringResource(R.string.dialog_cancel), color = BrandBlue)
                }
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = Color.White,
            title = { Text(text = stringResource(R.string.settings_clear_history)) },
            text = { Text(text = stringResource(R.string.settings_clear_history_sub)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_ok), color = BrandBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = stringResource(R.string.dialog_cancel), color = BrandBlue)
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = BrandBlue,
        modifier = Modifier.padding(start = 72.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(28.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
        }
    }
}

@Composable
private fun SettingsCheckboxRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(28.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = BrandBlue,
                uncheckedColor = InkSecondary,
            ),
        )
    }
}
