package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationDisabled
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.detector.AuditedAppInfo
import com.example.ui.DetectorUiState
import com.example.ui.IntegrityVerdict
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerGray
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.LocalSemanticStatusColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetectMockScreen(
    uiState: DetectorUiState,
    onRequestPermission: () -> Unit,
    onPollFix: () -> Unit,
    onRescanPackages: () -> Unit,
    onOpenDevOptions: () -> Unit,
    onCopyReport: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleShowOnlyFlagged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColors = LocalSemanticStatusColors.current
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val (accent, container, title, desc, icon) = when (uiState.verdict) {
        IntegrityVerdict.VERIFIED_AUTHENTIC -> Tuple5(
            statusColors.verified,
            statusColors.verifiedContainer,
            stringResource(R.string.status_verified_title),
            stringResource(R.string.status_verified_desc),
            Icons.Rounded.VerifiedUser,
        )
        IntegrityVerdict.MOCK_DETECTED -> Tuple5(
            statusColors.spoofed,
            statusColors.spoofedContainer,
            stringResource(R.string.status_mock_detected_title),
            stringResource(R.string.status_mock_detected_desc),
            Icons.Rounded.ReportProblem,
        )
        IntegrityVerdict.CAUTION_APPS_PRESENT -> Tuple5(
            statusColors.caution,
            statusColors.cautionContainer,
            stringResource(R.string.status_caution_title),
            stringResource(R.string.status_caution_desc),
            Icons.Rounded.Warning,
        )
        IntegrityVerdict.AWAITING_FIX -> Tuple5(
            BrandBlue,
            statusColors.telemetryContainer,
            stringResource(R.string.status_awaiting_title),
            stringResource(R.string.status_awaiting_desc),
            Icons.Rounded.GpsFixed,
        )
        IntegrityVerdict.PERMISSION_REQUIRED -> Tuple5(
            statusColors.caution,
            statusColors.cautionContainer,
            stringResource(R.string.status_permission_title),
            stringResource(R.string.status_permission_desc),
            Icons.Rounded.LocationDisabled,
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("detect_mock_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Status Verdict Card
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = container,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(32.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary,
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary,
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!uiState.hasLocationPermission) {
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                modifier = Modifier.height(48.dp),
                            ) {
                                Text(text = stringResource(R.string.action_grant_permission))
                            }
                        } else {
                            Button(
                                onClick = onPollFix,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                modifier = Modifier.height(48.dp),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = stringResource(R.string.action_poll_fix))
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenDevOptions,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = stringResource(R.string.action_open_dev_options))
                        }
                        OutlinedButton(
                            onClick = onCopyReport,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = stringResource(R.string.action_copy_report))
                        }
                    }
                }
            }
        }

        // Core 3 MockLocationDetector Methods
        item {
            Text(
                text = stringResource(R.string.section_detection_matrix),
                style = MaterialTheme.typography.titleSmall,
                color = BrandBlue,
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MethodCheckCard(
                    icon = Icons.Rounded.GpsFixed,
                    title = stringResource(R.string.check_mock_provider_title),
                    subtitle = stringResource(R.string.check_mock_provider_sub),
                    badgeText = when {
                        !uiState.hasLocationPermission -> "NO PERM"
                        uiState.currentFix == null -> "PENDING"
                        uiState.isLocationMocked -> "TRUE (MOCK)"
                        else -> "FALSE"
                    },
                    badgeColor = when {
                        !uiState.hasLocationPermission || uiState.currentFix == null -> BrandBlue
                        uiState.isLocationMocked -> statusColors.spoofed
                        else -> statusColors.verified
                    },
                )
                MethodCheckCard(
                    icon = Icons.Rounded.SettingsApplications,
                    title = stringResource(R.string.check_allow_mock_title),
                    subtitle = stringResource(R.string.check_allow_mock_sub),
                    badgeText = if (uiState.sdkInt >= 23) "FALSE (API 23+)" else if (uiState.isAllowMockLocationsOn) "TRUE" else "FALSE",
                    badgeColor = if (uiState.isAllowMockLocationsOn) statusColors.spoofed else statusColors.verified,
                )
                MethodCheckCard(
                    icon = Icons.Rounded.Apps,
                    title = stringResource(R.string.check_mock_apps_title),
                    subtitle = stringResource(R.string.check_mock_apps_sub),
                    badgeText = if (uiState.isScanningPackages) "SCANNING" else if (uiState.hasMockAppsInstalled) "TRUE (${uiState.flaggedApps.size})" else "FALSE (${uiState.totalPackagesScanned})",
                    badgeColor = if (uiState.hasMockAppsInstalled) statusColors.caution else statusColors.verified,
                )
            }
        }

        // Live Fix Telemetry
        uiState.currentFix?.let { fix ->
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.section_live_telemetry),
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandBlue,
                        )
                        Text(
                            text = "${fix.provider.uppercase(Locale.US)} · ${"%.6f".format(Locale.US, fix.latitude)}, ${"%.6f".format(Locale.US, fix.longitude)} · ±${"%.1f".format(Locale.US, fix.accuracyMeters)}m · ${formatter.format(Date(fix.fixTimestampMs))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkPrimary,
                        )
                    }
                }
            }
        }

        // Installed Package Audit
        item {
            HorizontalDivider(color = DividerGray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.audit_header),
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                    )
                    Text(
                        text = stringResource(R.string.audit_scanned_count, uiState.totalPackagesScanned),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                    )
                }
                OutlinedButton(
                    onClick = onRescanPackages,
                    enabled = !uiState.isScanningPackages,
                    modifier = Modifier.height(48.dp),
                ) {
                    if (uiState.isScanningPackages) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(R.string.action_scan_now))
                }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.appSearchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                placeholder = { Text(text = stringResource(R.string.audit_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.appSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            FilterChip(
                selected = uiState.showOnlyFlaggedApps,
                onClick = onToggleShowOnlyFlagged,
                label = { Text(text = stringResource(R.string.audit_only_flagged)) },
                leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.height(48.dp),
            )
        }

        val apps = uiState.filteredApps
        if (apps.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.audit_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        } else {
            items(apps, key = { it.packageName }) { app ->
                AuditedAppRow(app = app)
            }
        }
    }
}

@Composable
private fun MethodCheckCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color.White,
        border = BorderStroke(1.dp, DividerGray),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
            Surface(color = badgeColor.copy(alpha = 0.14f), shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun AuditedAppRow(app: AuditedAppInfo) {
    val statusColors = LocalSemanticStatusColors.current
    val badgeColor = if (app.requestsMockPermission) statusColors.spoofed else statusColors.verified

    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color.White,
        border = BorderStroke(1.dp, if (app.requestsMockPermission) badgeColor.copy(alpha = 0.5f) else DividerGray),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (app.requestsMockPermission) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(color = badgeColor.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    text = stringResource(if (app.requestsMockPermission) R.string.badge_flagged else R.string.badge_clean),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
