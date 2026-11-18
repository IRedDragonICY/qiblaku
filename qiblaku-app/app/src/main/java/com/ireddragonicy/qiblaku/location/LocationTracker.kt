package com.ireddragonicy.qiblaku.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationSnapshot(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeM: Double = 0.0,
    val accuracyM: Float? = null,
    val provider: String = "Belum ada fix",
    val timestampMs: Long = 0L,
    val isLocked: Boolean = false,
    val isManual: Boolean = false
) {
    val hasFix: Boolean get() = latitude != null && longitude != null
}

class LocationTracker(context: Context) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _state = MutableStateFlow(LocationSnapshot())
    val state: StateFlow<LocationSnapshot> = _state.asStateFlow()

    private var isListening = false
    private var bestLiveLocation: Location? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleNewLocation(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isListening) return
        isListening = true

        val providers = buildList {
            if (locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                locationManager.allProviders.contains(LocationManager.FUSED_PROVIDER)
            ) {
                add(LocationManager.FUSED_PROVIDER)
            }
            if (locationManager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        for (p in providers) {
            try {
                locationManager.getLastKnownLocation(p)?.let { handleNewLocation(it) }
                locationManager.requestLocationUpdates(
                    p,
                    1000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {
                // Permission not yet granted
            } catch (_: IllegalArgumentException) {
                // Provider unavailable
            }
        }
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        try {
            locationManager.removeUpdates(listener)
        } catch (_: SecurityException) {
        }
    }

    private fun handleNewLocation(loc: Location) {
        val currentBest = bestLiveLocation
        val isBetter = currentBest == null ||
            isBetterLocation(loc, currentBest)
        if (isBetter) {
            bestLiveLocation = loc
        }

        val currentState = _state.value
        if (currentState.isLocked || currentState.isManual) return

        val chosen = bestLiveLocation ?: loc
        _state.value = LocationSnapshot(
            latitude = chosen.latitude,
            longitude = chosen.longitude,
            altitudeM = if (chosen.hasAltitude()) chosen.altitude else 113.0,
            accuracyM = if (chosen.hasAccuracy()) chosen.accuracy else null,
            provider = chosen.provider ?: "unknown",
            timestampMs = chosen.time,
            isLocked = false,
            isManual = false
        )
    }

    private fun isBetterLocation(candidate: Location, current: Location): Boolean {
        val timeDelta = candidate.time - current.time
        val isSignificantlyNewer = timeDelta > 15_000L
        val isSignificantlyOlder = timeDelta < -15_000L
        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) return false

        val accDelta = (candidate.accuracy - current.accuracy).toInt()
        val isMoreAccurate = accDelta < 0
        val isSignificantlyLessAccurate = accDelta > 15
        val isFromGps = candidate.provider == LocationManager.GPS_PROVIDER

        return when {
            isMoreAccurate -> true
            timeDelta > 0 && !isSignificantlyLessAccurate && isFromGps -> true
            timeDelta > 0 && candidate.accuracy <= current.accuracy -> true
            else -> false
        }
    }

    fun toggleLock() {
        val cur = _state.value
        if (!cur.hasFix) return
        if (cur.isLocked) {
            val live = bestLiveLocation
            _state.value = if (live != null) {
                LocationSnapshot(
                    latitude = live.latitude,
                    longitude = live.longitude,
                    altitudeM = if (live.hasAltitude()) live.altitude else 113.0,
                    accuracyM = if (live.hasAccuracy()) live.accuracy else null,
                    provider = live.provider ?: "unknown",
                    timestampMs = live.time,
                    isLocked = false,
                    isManual = false
                )
            } else {
                cur.copy(isLocked = false, isManual = false)
            }
        } else {
            _state.value = cur.copy(isLocked = true)
        }
    }

    fun setManualCoordinates(lat: Double, lon: Double, altM: Double = 113.0) {
        _state.value = LocationSnapshot(
            latitude = lat,
            longitude = lon,
            altitudeM = altM,
            accuracyM = 0f,
            provider = "Manual",
            timestampMs = System.currentTimeMillis(),
            isLocked = true,
            isManual = true
        )
    }

    fun unlockToLive() {
        val live = bestLiveLocation
        _state.value = if (live != null) {
            LocationSnapshot(
                latitude = live.latitude,
                longitude = live.longitude,
                altitudeM = if (live.hasAltitude()) live.altitude else 113.0,
                accuracyM = if (live.hasAccuracy()) live.accuracy else null,
                provider = live.provider ?: "unknown",
                timestampMs = live.time,
                isLocked = false,
                isManual = false
            )
        } else {
            LocationSnapshot()
        }
    }
}
