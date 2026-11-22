package com.example.location

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppPreferencesRepository
import com.example.ui.AppSettings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.random.Random

data class MockEngineState(
    val isRunning: Boolean = false,
    val latitude: Double = -1.910141,
    val longitude: Double = 112.20003,
    val lastError: String? = null,
)

object MockLocationStateBus {
    private val _state = MutableStateFlow(MockEngineState())
    val state: StateFlow<MockEngineState> = _state.asStateFlow()

    fun update(transform: (MockEngineState) -> MockEngineState) {
        _state.value = transform(_state.value)
    }
}

object MockLocationHelper {

    fun isMockAppSelected(context: Context): Boolean {
        val appOpsAllowed = runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName,
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)

        if (appOpsAllowed) return true

        // Fallback probe on devices where AppOps returns MODE_DEFAULT until addTestProvider is called
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            val probeProvider = "mapku_probe"
            registerTestProvider(lm, probeProvider)
            runCatching { lm.removeTestProvider(probeProvider) }
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    @Suppress("DEPRECATION")
    fun registerTestProvider(lm: LocationManager, providerName: String) {
        runCatching { lm.removeTestProvider(providerName) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val properties = ProviderProperties.Builder()
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                .setAccuracy(ProviderProperties.ACCURACY_FINE)
                .build()
            lm.addTestProvider(providerName, properties)
        } else {
            lm.addTestProvider(
                providerName,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE,
            )
        }
        lm.setTestProviderEnabled(providerName, true)
    }
}

class MockLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var injectionJob: Job? = null
    private lateinit var locationManager: LocationManager
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var prefsRepo: AppPreferencesRepository

    @Volatile
    private var currentLat: Double = -1.910141

    @Volatile
    private var currentLon: Double = 112.20003

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        prefsRepo = AppPreferencesRepository(applicationContext)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMockEngine()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_COORDS -> {
                currentLat = intent.getDoubleExtra(EXTRA_LAT, currentLat)
                currentLon = intent.getDoubleExtra(EXTRA_LON, currentLon)
                MockLocationStateBus.update {
                    it.copy(latitude = currentLat, longitude = currentLon)
                }
                updateNotification()
                return START_STICKY
            }
            ACTION_START, null -> {
                currentLat = intent?.getDoubleExtra(EXTRA_LAT, currentLat) ?: currentLat
                currentLon = intent?.getDoubleExtra(EXTRA_LON, currentLon) ?: currentLon
                startForegroundCompat(buildNotification(currentLat, currentLon))
                startMockLoop()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMockLoop() {
        injectionJob?.cancel()

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val registeredProviders = mutableListOf<String>()
        var securityFailed = false

        for (provider in providers) {
            try {
                MockLocationHelper.registerTestProvider(locationManager, provider)
                registeredProviders.add(provider)
            } catch (sec: SecurityException) {
                securityFailed = true
            } catch (_: Throwable) {
                // Ignore provider if unavailable on specific OEM
            }
        }

        if (securityFailed && registeredProviders.isEmpty()) {
            MockLocationStateBus.update {
                it.copy(
                    isRunning = false,
                    lastError = "MOCK_PERMISSION_DENIED",
                )
            }
            stopMockEngine()
            return
        }

        runCatching { fusedClient.setMockMode(true) }

        MockLocationStateBus.update {
            it.copy(
                isRunning = true,
                latitude = currentLat,
                longitude = currentLon,
                lastError = null,
            )
        }

        injectionJob = serviceScope.launch {
            while (isActive) {
                val settings = prefsRepo.loadSettings()
                if (settings.movementSimulation && settings.maxDistanceMovementMeters > 0f) {
                    val stepMeters = Random.nextDouble(0.0, settings.maxDistanceMovementMeters.toDouble())
                    val angleRad = Random.nextDouble(0.0, 2.0 * Math.PI)
                    val dLat = (stepMeters * kotlin.math.sin(angleRad)) / 111_320.0
                    val cosLat = cos(Math.toRadians(currentLat)).coerceAtLeast(0.01)
                    val dLon = (stepMeters * cos(angleRad)) / (111_320.0 * cosLat)
                    currentLat = (currentLat + dLat).coerceIn(-89.999, 89.999)
                    currentLon = (currentLon + dLon).coerceIn(-179.999, 179.999)
                }

                for (provider in registeredProviders) {
                    val mockLoc = buildMockLocation(provider, currentLat, currentLon, settings)
                    runCatching {
                        locationManager.setTestProviderLocation(provider, mockLoc)
                    }
                }

                val fusedLoc = buildMockLocation(
                    LocationManager.GPS_PROVIDER,
                    currentLat,
                    currentLon,
                    settings,
                )
                runCatching { fusedClient.setMockLocation(fusedLoc) }

                MockLocationStateBus.update {
                    it.copy(
                        isRunning = true,
                        latitude = currentLat,
                        longitude = currentLon,
                    )
                }
                if (settings.showNotificationBar) {
                    updateNotification()
                }

                delay(settings.updateIntervalMs.coerceIn(250L, 60_000L))
            }
        }
    }

    private fun buildMockLocation(
        provider: String,
        lat: Double,
        lon: Double,
        settings: AppSettings,
    ): Location {
        return Location(provider).apply {
            latitude = lat
            longitude = lon
            altitude = settings.altitudeMeters.toDouble()
            accuracy = settings.accuracyMeters.coerceAtLeast(1f)
            bearing = settings.bearingDegrees
            speed = if (settings.movementSimulation) 1.4f else 0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bearingAccuracyDegrees = 1.0f
                verticalAccuracyMeters = 1.0f
                speedAccuracyMetersPerSecond = 0.5f
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopMockEngine() {
        injectionJob?.cancel()
        injectionJob = null

        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            runCatching { locationManager.setTestProviderEnabled(provider, false) }
            runCatching { locationManager.removeTestProvider(provider) }
        }
        runCatching { fusedClient.setMockMode(false) }

        MockLocationStateBus.update { it.copy(isRunning = false) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.mock_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(currentLat, currentLon))
    }

    private fun buildNotification(lat: Double, lon: Double): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MockLocationService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val contentText = String.format(Locale.US, "%.6f, %.6f", lat, lon)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.mock_notif_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.mock_notif_stop),
                stopIntent,
            )
            .build()
    }

    override fun onDestroy() {
        injectionJob?.cancel()
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            runCatching { locationManager.removeTestProvider(provider) }
        }
        runCatching { fusedClient.setMockMode(false) }
        MockLocationStateBus.update { it.copy(isRunning = false) }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.location.ACTION_START_MOCK"
        const val ACTION_UPDATE_COORDS = "com.example.location.ACTION_UPDATE_COORDS"
        const val ACTION_STOP = "com.example.location.ACTION_STOP_MOCK"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        private const val CHANNEL_ID = "mapku_mock_location"
        private const val NOTIFICATION_ID = 4101
    }
}
