package com.example.ui

import com.example.detector.AuditedAppInfo
import com.example.location.ProviderAvailability

enum class DrawerDestination {
    MAP,
    MARKERS,
    SETTINGS,
    DETECT_MOCK,
}

enum class IntegrityVerdict {
    PERMISSION_REQUIRED,
    AWAITING_FIX,
    VERIFIED_AUTHENTIC,
    CAUTION_APPS_PRESENT,
    MOCK_DETECTED,
}

data class MarkerFilter(
    val showHistory: Boolean = true,
    val showFavorites: Boolean = true,
)

data class SavedMarker(
    val id: Long,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean,
    val isFromMockProvider: Boolean,
    val provider: String,
    val createdAtMs: Long,
)

data class AppSettings(
    val accuracyMeters: Float = 10f,
    val altitudeMeters: Float = 15f,
    val bearingDegrees: Float = 0f,
    val updateIntervalMs: Long = 1500L,
    val movementSimulation: Boolean = false,
    val maxDistanceMovementMeters: Float = 25f,
    val showNotificationBar: Boolean = true,
    val autoZoomMarkerGroup: Boolean = true,
)

data class LocationFixSnapshot(
    val latitude: Double,
    val longitude: Double,
    val provider: String,
    val accuracyMeters: Float,
    val hasAccuracy: Boolean,
    val altitudeMeters: Double,
    val hasAltitude: Boolean,
    val speedMps: Float,
    val hasSpeed: Boolean,
    val fixTimestampMs: Long,
    val isFromMockProvider: Boolean,
)

data class DetectorUiState(
    val destination: DrawerDestination = DrawerDestination.MAP,
    val selectedLatitude: Double = -1.910141,
    val selectedLongitude: Double = 112.20003,
    val mapZoomLevel: Double = 5.2,
    val mapCenterTrigger: Int = 0,
    val hasLocationPermission: Boolean = false,
    val hasFineLocation: Boolean = false,
    val isLiveMonitoring: Boolean = false,
    val isMockServiceActive: Boolean = false,
    val showSelectMockAppDialog: Boolean = false,
    val isScanningPackages: Boolean = false,
    val providerAvailability: ProviderAvailability = ProviderAvailability(false, false, false),
    val currentFix: LocationFixSnapshot? = null,
    val isLocationMocked: Boolean = false,
    val isAllowMockLocationsOn: Boolean = false,
    val hasMockAppsInstalled: Boolean = false,
    val totalPackagesScanned: Int = 0,
    val auditedApps: List<AuditedAppInfo> = emptyList(),
    val flaggedApps: List<AuditedAppInfo> = emptyList(),
    val appSearchQuery: String = "",
    val showOnlyFlaggedApps: Boolean = false,
    val markerFilter: MarkerFilter = MarkerFilter(),
    val savedMarkers: List<SavedMarker> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val sdkInt: Int = android.os.Build.VERSION.SDK_INT,
) {
    val verdict: IntegrityVerdict
        get() = when {
            !hasLocationPermission -> IntegrityVerdict.PERMISSION_REQUIRED
            isLocationMocked || isAllowMockLocationsOn -> IntegrityVerdict.MOCK_DETECTED
            currentFix == null -> IntegrityVerdict.AWAITING_FIX
            hasMockAppsInstalled -> IntegrityVerdict.CAUTION_APPS_PRESENT
            else -> IntegrityVerdict.VERIFIED_AUTHENTIC
        }

    val visibleMapMarkers: List<SavedMarker>
        get() = savedMarkers.filter { marker ->
            (marker.isFavorite && markerFilter.showFavorites) ||
                (!marker.isFavorite && markerFilter.showHistory)
        }

    val filteredApps: List<AuditedAppInfo>
        get() {
            val base = if (showOnlyFlaggedApps) flaggedApps else auditedApps
            val query = appSearchQuery.trim().lowercase()
            if (query.isEmpty()) return base
            return base.filter {
                it.appName.lowercase().contains(query) ||
                    it.packageName.lowercase().contains(query)
            }
        }
}
