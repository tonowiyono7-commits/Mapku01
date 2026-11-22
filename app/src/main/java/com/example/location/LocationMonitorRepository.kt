package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ProviderAvailability(
    val gpsEnabled: Boolean,
    val networkEnabled: Boolean,
    val passiveEnabled: Boolean,
)

class LocationMonitorRepository(private val context: Context) {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasFineOrCoarsePermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFinePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getProviderAvailability(): ProviderAvailability {
        val lm = locationManager ?: return ProviderAvailability(false, false, false)
        return ProviderAvailability(
            gpsEnabled = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }
                .getOrDefault(false),
            networkEnabled = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }
                .getOrDefault(false),
            passiveEnabled = runCatching { lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) }
                .getOrDefault(false),
        )
    }

    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(): Location? {
        if (!hasFineOrCoarsePermission()) return null
        val lm = locationManager ?: return null
        val providers = buildList {
            if (hasFinePermission()) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        var best: Location? = null
        for (provider in providers) {
            val candidate = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (best == null || candidate.time > best.time) {
                best = candidate
            }
        }
        return best
    }

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(
        minTimeMs: Long = 1500L,
        minDistanceMeters: Float = 0f,
    ): Flow<Location> = callbackFlow {
        val lm = locationManager
        if (lm == null || !hasFineOrCoarsePermission()) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // No-op for backward compatibility on older APIs
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val providersToSubscribe = buildList {
            if (hasFinePermission() && runCatching {
                    lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                }.getOrDefault(false)
            ) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (runCatching {
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                }.getOrDefault(false)
            ) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (isEmpty()) {
                add(LocationManager.PASSIVE_PROVIDER)
            }
        }

        for (provider in providersToSubscribe) {
            runCatching {
                lm.requestLocationUpdates(
                    provider,
                    minTimeMs,
                    minDistanceMeters,
                    listener,
                    Looper.getMainLooper(),
                )
            }
        }

        awaitClose {
            runCatching { lm.removeUpdates(listener) }
        }
    }
}
