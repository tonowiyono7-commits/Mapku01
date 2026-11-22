package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.DetectorUiState
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSemanticStatusColors
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale

@Composable
fun FullScreenOsmMap(
    uiState: DetectorUiState,
    onMapTap: (Double, Double) -> Unit,
    onMyLocationClick: () -> Unit,
    onFabToggleLive: () -> Unit,
    onAlertBadgeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val statusColors = LocalSemanticStatusColors.current

    val bluePinDrawable = remember(context) { createPinDrawable(context, 0xFF1E88E5.toInt(), 0xFF1565C0.toInt()) }
    val redPinDrawable = remember(context) { createPinDrawable(context, 0xFFE53935.toInt(), 0xFFB71C1C.toInt()) }
    val favPinDrawable = remember(context) { createPinDrawable(context, 0xFFFB8C00.toInt(), 0xFFE65100.toInt()) }

    val mapView = remember(context) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(uiState.mapZoomLevel)
            controller.setCenter(GeoPoint(uiState.selectedLatitude, uiState.selectedLongitude))
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(uiState.mapCenterTrigger) {
        if (uiState.mapCenterTrigger > 0) {
            mapView.controller.setZoom(uiState.mapZoomLevel)
            mapView.controller.animateTo(GeoPoint(uiState.selectedLatitude, uiState.selectedLongitude))
        }
    }

    LaunchedEffect(
        uiState.selectedLatitude,
        uiState.selectedLongitude,
        uiState.visibleMapMarkers,
        uiState.isLocationMocked,
    ) {
        mapView.overlays.clear()

        val eventsOverlay = MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        onMapTap(p.latitude, p.longitude)
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        onMapTap(p.latitude, p.longitude)
                    }
                    return true
                }
            },
        )
        mapView.overlays.add(eventsOverlay)

        // Filtered History / Favorites pins
        uiState.visibleMapMarkers.take(30).forEach { saved ->
            val m = Marker(mapView).apply {
                position = GeoPoint(saved.latitude, saved.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = when {
                    saved.isFavorite -> favPinDrawable
                    saved.isFromMockProvider -> redPinDrawable
                    else -> bluePinDrawable
                }
                alpha = 0.72f
                title = saved.label
                snippet = String.format(Locale.US, "%.6f, %.6f", saved.latitude, saved.longitude)
            }
            mapView.overlays.add(m)
        }

        // Primary selected pin
        val activeMarker = Marker(mapView).apply {
            position = GeoPoint(uiState.selectedLatitude, uiState.selectedLongitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = if (uiState.isLocationMocked) redPinDrawable else bluePinDrawable
            title = if (uiState.isLocationMocked) "Mock Location Detected" else "Selected Location"
            snippet = String.format(
                Locale.US,
                "%.6f, %.6f",
                uiState.selectedLatitude,
                uiState.selectedLongitude,
            )
        }
        mapView.overlays.add(activeMarker)
        mapView.invalidate()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("fullscreen_osm_map"),
        )

        // Top-left status chip when Mock GPS is active or mock detection triggers
        if (uiState.isMockServiceActive || uiState.isLocationMocked || uiState.isAllowMockLocationsOn || uiState.hasMockAppsInstalled) {
            val isSpoofed = !uiState.isMockServiceActive && (uiState.isLocationMocked || uiState.isAllowMockLocationsOn)
            val pillColor = when {
                uiState.isMockServiceActive -> BrandBlue
                isSpoofed -> statusColors.spoofed
                else -> statusColors.caution
            }
            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, pillColor.copy(alpha = 0.6f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clickable(onClick = onAlertBadgeClick)
                    .testTag("map_mock_alert_chip"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = if (uiState.isMockServiceActive) Icons.Rounded.PlayArrow else Icons.Rounded.ReportProblem,
                        contentDescription = null,
                        tint = pillColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            uiState.isMockServiceActive -> stringResource(R.string.mock_active_badge)
                            isSpoofed -> stringResource(R.string.status_mock_detected_title)
                            else -> stringResource(R.string.status_caution_title)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = pillColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Top-right My Location square button (matches Screenshot 1)
        Surface(
            onClick = onMyLocationClick,
            shape = RoundedCornerShape(3.dp),
            color = Color.White.copy(alpha = 0.88f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(48.dp)
                .testTag("map_my_location_button"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = stringResource(R.string.action_my_location),
                    tint = Color(0xFF555555),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Bottom-left OpenStreetMap attribution
        Surface(
            color = Color.White.copy(alpha = 0.78f),
            shape = RoundedCornerShape(3.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.osm_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF444444),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        // Bottom-center Latitude / Longitude translucent card (matches Screenshot 1)
        Surface(
            color = Color.White.copy(alpha = 0.84f),
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .testTag("map_coordinates_box"),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_latitude_colon),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF111111),
                        modifier = Modifier.width(82.dp),
                    )
                    Text(
                        text = String.format(Locale.US, "%.6f", uiState.selectedLatitude),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF111111),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_longitude_colon),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF111111),
                        modifier = Modifier.width(82.dp),
                    )
                    Text(
                        text = String.format(Locale.US, "%.5f", uiState.selectedLongitude),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF111111),
                    )
                }
            }
        }

        // Bottom-right circular bright blue Play/Stop FAB (matches Screenshot 1)
        FloatingActionButton(
            onClick = onFabToggleLive,
            shape = CircleShape,
            containerColor = BrandBlue,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(56.dp)
                .testTag("map_fab_toggle"),
        ) {
            Icon(
                imageVector = if (uiState.isMockServiceActive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (uiState.isMockServiceActive) {
                        R.string.action_stop_monitoring
                    } else {
                        R.string.action_start_monitoring
                    },
                ),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

private fun createPinDrawable(context: Context, fillColor: Int, strokeColor: Int): Drawable {
    val sizePx = (40 * context.resources.displayMetrics.density).toInt().coerceAtLeast(64)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = sizePx / 2f
    val r = sizePx * 0.28f
    val cy = sizePx * 0.34f

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.04f
    }

    val pinPath = Path().apply {
        moveTo(cx, sizePx * 0.92f)
        cubicTo(
            cx - r * 1.1f, cy + r * 0.8f,
            cx - r, cy + r * 0.2f,
            cx - r, cy,
        )
        arcTo(cx - r, cy - r, cx + r, cy + r, 180f, 180f, false)
        cubicTo(
            cx + r, cy + r * 0.2f,
            cx + r * 1.1f, cy + r * 0.8f,
            cx, sizePx * 0.92f,
        )
        close()
    }
    canvas.drawPath(pinPath, fillPaint)
    canvas.drawPath(pinPath, strokePaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, r * 0.36f, dotPaint)

    val whiteRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.03f
    }
    canvas.drawCircle(cx, sizePx * 0.92f, sizePx * 0.055f, fillPaint)
    canvas.drawCircle(cx, sizePx * 0.92f, sizePx * 0.055f, whiteRingPaint)

    return BitmapDrawable(context.resources, bitmap)
}
