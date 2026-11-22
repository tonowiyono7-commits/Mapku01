package com.example.ui

import android.app.Application
import android.location.Location
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferencesRepository
import com.example.detector.MockLocationDetector
import com.example.location.LocationMonitorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class DetectorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocationMonitorRepository(application.applicationContext)
    private val prefsRepo = AppPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        DetectorUiState(
            settings = prefsRepo.loadSettings(),
            savedMarkers = prefsRepo.loadMarkers(),
        ),
    )
    val uiState: StateFlow<DetectorUiState> = _uiState.asStateFlow()

    private var liveCollectionJob: Job? = null
    private val nextMarkerId = AtomicLong(
        (_uiState.value.savedMarkers.maxOfOrNull { it.id } ?: 0L) + 1L,
    )

    init {
        refreshPermissionsAndEnvironment()
        refreshPackageAudit()
    }

    fun navigateTo(destination: DrawerDestination) {
        _uiState.update { it.copy(destination = destination) }
    }

    fun selectMapCoordinate(latitude: Double, longitude: Double, recordHistory: Boolean = true) {
        _uiState.update { state ->
            val updatedMarkers = if (recordHistory) {
                val entry = SavedMarker(
                    id = nextMarkerId.getAndIncrement(),
                    label = String.format(Locale.US, "Pin %.4f, %.4f", latitude, longitude),
                    latitude = latitude,
                    longitude = longitude,
                    isFavorite = false,
                    isFromMockProvider = state.isLocationMocked,
                    provider = "map",
                    createdAtMs = System.currentTimeMillis(),
                )
                (listOf(entry) + state.savedMarkers).take(120).also {
                    prefsRepo.saveMarkers(it)
                }
            } else {
                state.savedMarkers
            }
            state.copy(
                selectedLatitude = latitude,
                selectedLongitude = longitude,
                savedMarkers = updatedMarkers,
            )
        }
    }

    fun jumpToMarker(marker: SavedMarker) {
        _uiState.update { state ->
            state.copy(
                destination = DrawerDestination.MAP,
                selectedLatitude = marker.latitude,
                selectedLongitude = marker.longitude,
                mapZoomLevel = if (state.settings.autoZoomMarkerGroup) 15.0 else state.mapZoomLevel,
                mapCenterTrigger = state.mapCenterTrigger + 1,
            )
        }
    }

    fun addCurrentPinToFavorites() {
        _uiState.update { state ->
            val fav = SavedMarker(
                id = nextMarkerId.getAndIncrement(),
                label = String.format(
                    Locale.US,
                    "Favorite %.4f, %.4f",
                    state.selectedLatitude,
                    state.selectedLongitude,
                ),
                latitude = state.selectedLatitude,
                longitude = state.selectedLongitude,
                isFavorite = true,
                isFromMockProvider = state.isLocationMocked,
                provider = state.currentFix?.provider ?: "map",
                createdAtMs = System.currentTimeMillis(),
            )
            val updated = (listOf(fav) + state.savedMarkers).take(120)
            prefsRepo.saveMarkers(updated)
            state.copy(savedMarkers = updated)
        }
    }

    fun toggleFavorite(markerId: Long) {
        _uiState.update { state ->
            val updated = state.savedMarkers.map {
                if (it.id == markerId) it.copy(isFavorite = !it.isFavorite) else it
            }
            prefsRepo.saveMarkers(updated)
            state.copy(savedMarkers = updated)
        }
    }

    fun deleteMarker(markerId: Long) {
        _uiState.update { state ->
            val updated = state.savedMarkers.filterNot { it.id == markerId }
            prefsRepo.saveMarkers(updated)
            state.copy(savedMarkers = updated)
        }
    }

    fun clearHistoryMarkers() {
        _uiState.update { state ->
            val updated = state.savedMarkers.filter { it.isFavorite }
            prefsRepo.saveMarkers(updated)
            state.copy(savedMarkers = updated)
        }
    }

    fun applyMarkerFilter(showHistory: Boolean, showFavorites: Boolean) {
        _uiState.update { state ->
            state.copy(
                markerFilter = MarkerFilter(
                    showHistory = showHistory,
                    showFavorites = showFavorites,
                ),
            )
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val currentMonitoring = _uiState.value.isLiveMonitoring
        val oldInterval = _uiState.value.settings.updateIntervalMs
        _uiState.update { state ->
            val updated = transform(state.settings)
            prefsRepo.saveSettings(updated)
            state.copy(settings = updated)
        }
        if (currentMonitoring && _uiState.value.settings.updateIntervalMs != oldInterval) {
            startLiveMonitoring()
        }
    }

    fun exportMarkersJson(): String {
        return prefsRepo.encodeMarkersJson(_uiState.value.savedMarkers)
    }

    fun importMarkersJson(rawJson: String): Int {
        val parsed = prefsRepo.parseMarkersJson(rawJson)
        if (parsed.isEmpty()) return 0
        _uiState.update { state ->
            val existingCoords = state.savedMarkers.map {
                "${it.latitude},${it.longitude},${it.isFavorite}"
            }.toSet()
            val fresh = parsed.filterNot {
                "${it.latitude},${it.longitude},${it.isFavorite}" in existingCoords
            }.map {
                it.copy(id = nextMarkerId.getAndIncrement())
            }
            val merged = (fresh + state.savedMarkers).take(200)
            prefsRepo.saveMarkers(merged)
            state.copy(savedMarkers = merged)
        }
        return parsed.size
    }

    fun updateAppSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun toggleShowOnlyFlagged() {
        _uiState.update { it.copy(showOnlyFlaggedApps = !it.showOnlyFlaggedApps) }
    }

    fun onPermissionResult() {
        refreshPermissionsAndEnvironment()
        if (_uiState.value.hasLocationPermission) {
            pollLocationFix(centerMap = true)
            startLiveMonitoring()
        }
    }

    fun refreshPermissionsAndEnvironment() {
        val context = getApplication<Application>().applicationContext
        val hasPermission = repository.hasFineOrCoarsePermission()
        val hasFine = repository.hasFinePermission()
        val providers = repository.getProviderAvailability()
        val legacySettingOn = MockLocationDetector.isAllowMockLocationsOn(context)

        _uiState.update {
            it.copy(
                hasLocationPermission = hasPermission,
                hasFineLocation = hasFine,
                providerAvailability = providers,
                isAllowMockLocationsOn = legacySettingOn,
            )
        }
    }

    fun runFullDiagnosticScan() {
        refreshPermissionsAndEnvironment()
        refreshPackageAudit()
        if (_uiState.value.hasLocationPermission) {
            pollLocationFix(centerMap = false)
        }
    }

    fun refreshPackageAudit() {
        if (_uiState.value.isScanningPackages) return
        _uiState.update { it.copy(isScanningPackages = true) }

        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val auditResult = withContext(Dispatchers.IO) {
                MockLocationDetector.inspectVisiblePackages(context)
            }
            val legacySettingOn = MockLocationDetector.isAllowMockLocationsOn(context)

            _uiState.update { state ->
                state.copy(
                    isScanningPackages = false,
                    isAllowMockLocationsOn = legacySettingOn,
                    hasMockAppsInstalled = auditResult.hasMockApps,
                    totalPackagesScanned = auditResult.totalPackagesScanned,
                    auditedApps = auditResult.allApps,
                    flaggedApps = auditResult.flaggedApps,
                )
            }
        }
    }

    fun pollLocationFix(centerMap: Boolean = true) {
        refreshPermissionsAndEnvironment()
        if (!_uiState.value.hasLocationPermission) return
        val location = repository.getBestLastKnownLocation() ?: return
        processIncomingLocation(location, centerMap = centerMap)
    }

    fun startLiveMonitoring() {
        refreshPermissionsAndEnvironment()
        if (!_uiState.value.hasLocationPermission) return
        liveCollectionJob?.cancel()

        val intervalMs = _uiState.value.settings.updateIntervalMs.coerceIn(250L, 60_000L)
        _uiState.update { it.copy(isLiveMonitoring = true) }
        liveCollectionJob = viewModelScope.launch {
            repository.observeLocationUpdates(minTimeMs = intervalMs).collect { location ->
                processIncomingLocation(location, centerMap = true)
            }
        }
    }

    fun stopLiveMonitoring() {
        liveCollectionJob?.cancel()
        liveCollectionJob = null
        _uiState.update { it.copy(isLiveMonitoring = false) }
    }

    private fun processIncomingLocation(location: Location, centerMap: Boolean) {
        val context = getApplication<Application>().applicationContext
        val isMocked = MockLocationDetector.isLocationFromMockProvider(context, location)
        val allowMockSettingOn = MockLocationDetector.isAllowMockLocationsOn(context)

        val snapshot = LocationFixSnapshot(
            latitude = location.latitude,
            longitude = location.longitude,
            provider = location.provider ?: "unknown",
            accuracyMeters = location.accuracy,
            hasAccuracy = location.hasAccuracy(),
            altitudeMeters = location.altitude,
            hasAltitude = location.hasAltitude(),
            speedMps = location.speed,
            hasSpeed = location.hasSpeed(),
            fixTimestampMs = location.time,
            isFromMockProvider = isMocked,
        )

        val historyMarker = SavedMarker(
            id = nextMarkerId.getAndIncrement(),
            label = "${snapshot.provider.uppercase(Locale.US)} Fix",
            latitude = location.latitude,
            longitude = location.longitude,
            isFavorite = false,
            isFromMockProvider = isMocked,
            provider = snapshot.provider,
            createdAtMs = System.currentTimeMillis(),
        )

        _uiState.update { state ->
            val updatedMarkers = (listOf(historyMarker) + state.savedMarkers).take(120)
            prefsRepo.saveMarkers(updatedMarkers)
            state.copy(
                currentFix = snapshot,
                isLocationMocked = isMocked,
                isAllowMockLocationsOn = allowMockSettingOn,
                selectedLatitude = location.latitude,
                selectedLongitude = location.longitude,
                mapZoomLevel = if (centerMap) 15.0 else state.mapZoomLevel,
                mapCenterTrigger = if (centerMap) state.mapCenterTrigger + 1 else state.mapCenterTrigger,
                savedMarkers = updatedMarkers,
            )
        }
    }

    fun buildDiagnosticReport(): String {
        val state = _uiState.value
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val now = formatter.format(Date())
        return buildString {
            appendLine("=== MockDetector Diagnostic Report ===")
            appendLine("Generated: $now")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${state.sdkInt})")
            appendLine("Verdict: ${state.verdict.name}")
            appendLine("isLocationFromMockProvider: ${state.isLocationMocked}")
            appendLine("isAllowMockLocationsOn (<API 23): ${state.isAllowMockLocationsOn}")
            appendLine("checkForAllowMockLocationsApps: ${state.hasMockAppsInstalled} (${state.flaggedApps.size}/${state.totalPackagesScanned})")
            appendLine("Selected Pin: ${"%.6f".format(Locale.US, state.selectedLatitude)}, ${"%.6f".format(Locale.US, state.selectedLongitude)}")
            state.currentFix?.let { fix ->
                appendLine("Live Fix (${fix.provider}): ${"%.6f".format(Locale.US, fix.latitude)}, ${"%.6f".format(Locale.US, fix.longitude)} (±${"%.1f".format(Locale.US, fix.accuracyMeters)}m)")
            }
        }
    }

    override fun onCleared() {
        stopLiveMonitoring()
        super.onCleared()
    }
}
