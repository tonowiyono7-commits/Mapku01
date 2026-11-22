package com.example.data

import android.content.Context
import com.example.ui.AppSettings
import com.example.ui.SavedMarker
import org.json.JSONArray
import org.json.JSONObject

class AppPreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("mock_detector_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings {
        return AppSettings(
            accuracyMeters = prefs.getFloat(KEY_ACCURACY, 10f),
            altitudeMeters = prefs.getFloat(KEY_ALTITUDE, 15f),
            bearingDegrees = prefs.getFloat(KEY_BEARING, 0f),
            updateIntervalMs = prefs.getLong(KEY_UPDATE_INTERVAL, 1500L),
            movementSimulation = prefs.getBoolean(KEY_MOVEMENT_SIM, false),
            maxDistanceMovementMeters = prefs.getFloat(KEY_MAX_DISTANCE, 25f),
            showNotificationBar = prefs.getBoolean(KEY_SHOW_NOTIFICATION, true),
            autoZoomMarkerGroup = prefs.getBoolean(KEY_AUTO_ZOOM, true),
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putFloat(KEY_ACCURACY, settings.accuracyMeters)
            .putFloat(KEY_ALTITUDE, settings.altitudeMeters)
            .putFloat(KEY_BEARING, settings.bearingDegrees)
            .putLong(KEY_UPDATE_INTERVAL, settings.updateIntervalMs)
            .putBoolean(KEY_MOVEMENT_SIM, settings.movementSimulation)
            .putFloat(KEY_MAX_DISTANCE, settings.maxDistanceMovementMeters)
            .putBoolean(KEY_SHOW_NOTIFICATION, settings.showNotificationBar)
            .putBoolean(KEY_AUTO_ZOOM, settings.autoZoomMarkerGroup)
            .apply()
    }

    fun loadMarkers(): List<SavedMarker> {
        val raw = prefs.getString(KEY_MARKERS_JSON, null) ?: return emptyList()
        return parseMarkersJson(raw)
    }

    fun saveMarkers(markers: List<SavedMarker>) {
        prefs.edit()
            .putString(KEY_MARKERS_JSON, encodeMarkersJson(markers))
            .apply()
    }

    fun encodeMarkersJson(markers: List<SavedMarker>): String {
        val array = JSONArray()
        markers.forEach { m ->
            array.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("label", m.label)
                    put("latitude", m.latitude)
                    put("longitude", m.longitude)
                    put("isFavorite", m.isFavorite)
                    put("isFromMockProvider", m.isFromMockProvider)
                    put("provider", m.provider)
                    put("createdAtMs", m.createdAtMs)
                },
            )
        }
        return array.toString(2)
    }

    fun parseMarkersJson(raw: String): List<SavedMarker> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        SavedMarker(
                            id = obj.optLong("id", System.currentTimeMillis() + i),
                            label = obj.optString("label", "Saved Pin"),
                            latitude = obj.getDouble("latitude"),
                            longitude = obj.getDouble("longitude"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isFromMockProvider = obj.optBoolean("isFromMockProvider", false),
                            provider = obj.optString("provider", "map"),
                            createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_ACCURACY = "accuracy_meters"
        private const val KEY_ALTITUDE = "altitude_meters"
        private const val KEY_BEARING = "bearing_degrees"
        private const val KEY_UPDATE_INTERVAL = "update_interval_ms"
        private const val KEY_MOVEMENT_SIM = "movement_simulation"
        private const val KEY_MAX_DISTANCE = "max_distance_meters"
        private const val KEY_SHOW_NOTIFICATION = "show_notification_bar"
        private const val KEY_AUTO_ZOOM = "auto_zoom_marker_group"
        private const val KEY_MARKERS_JSON = "saved_markers_json"
    }
}
