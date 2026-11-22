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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.SavedMarker
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.DividerGray
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSemanticStatusColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MarkersScreen(
    markers: List<SavedMarker>,
    onSelectMarker: (SavedMarker) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onDeleteMarker: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.US) }

    val displayed = remember(markers, selectedTabIndex) {
        when (selectedTabIndex) {
            1 -> markers.filter { it.isFavorite }
            2 -> markers.filterNot { it.isFavorite }
            else -> markers
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("markers_screen"),
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = BrandBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = BrandBlue,
                )
            },
        ) {
            listOf(
                stringResource(R.string.markers_tab_all),
                stringResource(R.string.markers_tab_favorites),
                stringResource(R.string.markers_tab_history),
            ).forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title) },
                )
            }
        }

        if (displayed.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = BrandBlue.copy(alpha = 0.65f),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.markers_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.markers_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(
                    items = displayed,
                    key = { it.id },
                ) { marker ->
                    MarkerListRow(
                        marker = marker,
                        timeText = formatter.format(Date(marker.createdAtMs)),
                        onClick = { onSelectMarker(marker) },
                        onToggleFavorite = { onToggleFavorite(marker.id) },
                        onDelete = { onDeleteMarker(marker.id) },
                    )
                    HorizontalDivider(color = DividerGray)
                }
            }
        }
    }
}

@Composable
private fun MarkerListRow(
    marker: SavedMarker,
    timeText: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusColors = LocalSemanticStatusColors.current
    val pinTint = when {
        marker.isFromMockProvider -> statusColors.spoofed
        marker.isFavorite -> CautionAmber
        else -> BrandBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            .testTag("marker_row_${marker.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = pinTint,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = marker.label,
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    marker.latitude,
                    marker.longitude,
                ),
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
            )
            Text(
                text = "$timeText · ${marker.provider.uppercase(Locale.US)}${
                    if (marker.isFromMockProvider) " · MOCK" else ""
                }",
                style = MaterialTheme.typography.labelSmall,
                color = if (marker.isFromMockProvider) statusColors.spoofed else InkSecondary,
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (marker.isFavorite) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                contentDescription = "Favorite",
                tint = if (marker.isFavorite) CautionAmber else InkSecondary,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete",
                tint = InkSecondary,
            )
        }
    }
}
