package com.ireddragonicy.qiblaku.math

import android.hardware.GeomagneticField
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class QiblaTheoretical(
    val wgs84BearingDeg: Double,
    val wgs84DistanceKm: Double,
    val vincentyIterations: Int,
    val masaSphericalDeg: Double,
    val nuSphericalDeg: Double,
    val declinationDeg: Double,
    val wmmExpectedFieldUt: Double,
    val wmmHorizontalUt: Double,
    val wmmInclinationDeg: Double
)

data class CircularStats(
    val count: Int,
    val meanDeg: Double,
    val stdDeg: Double,
    val minDeg: Double,
    val maxDeg: Double
)

object QiblaCalculator {
    // WGS84 Ellipsoid parameters
    private const val WGS84_A = 6378137.0
    private const val WGS84_F = 1.0 / 298.257223563
    private const val WGS84_B = (1.0 - WGS84_F) * WGS84_A

    // Reference Ka'bah center (WGS84 / Masjidil Haram)
    const val KAABA_REF_LAT = 21.422487
    const val KAABA_REF_LON = 39.826206

    // Exact constants recovered via RE from MASA (com.labmu.masa)
    const val MASA_KAABA_LAT = 21.4225
    const val MASA_KAABA_LON = 39.8262

    // Exact constants recovered via RE from NU Online (package:adhan QiblaUtil)
    const val NU_KAABA_LAT = 21.4225241
    const val NU_KAABA_LON = 39.8261818

    fun wrap360(deg: Double): Double {
        if (!deg.isFinite()) return 0.0
        val m = deg % 360.0
        return if (m < 0.0) m + 360.0 else m
    }

    /**
     * Signed angular difference (target-relative): positive = clockwise/east of reference,
     * negative = counter-clockwise/west of reference, wrapped to (-180, +180].
     */
    fun signedDeltaDeg(measuredDeg: Double, referenceDeg: Double): Double {
        if (!measuredDeg.isFinite() || !referenceDeg.isFinite()) return 0.0
        var d = (measuredDeg - referenceDeg) % 360.0
        if (d <= -180.0) d += 360.0
        if (d > 180.0) d -= 360.0
        return d
    }

    fun computeAll(
        latDeg: Double,
        lonDeg: Double,
        altMeters: Double,
        timeMillis: Long = System.currentTimeMillis()
    ): QiblaTheoretical {
        val (wgsBearing, distKm, iters) = computeVincenty(
            latDeg,
            lonDeg,
            KAABA_REF_LAT,
            KAABA_REF_LON
        )
        val masaDeg = computeMasaSpherical(latDeg, lonDeg)
        val nuDeg = computeNuAdhanSpherical(latDeg, lonDeg)

        val geo = GeomagneticField(
            latDeg.toFloat(),
            lonDeg.toFloat(),
            altMeters.toFloat(),
            timeMillis
        )
        return QiblaTheoretical(
            wgs84BearingDeg = wgsBearing,
            wgs84DistanceKm = distKm,
            vincentyIterations = iters,
            masaSphericalDeg = masaDeg,
            nuSphericalDeg = nuDeg,
            declinationDeg = geo.declination.toDouble(),
            wmmExpectedFieldUt = geo.fieldStrength.toDouble() / 1000.0,
            wmmHorizontalUt = geo.horizontalStrength.toDouble() / 1000.0,
            wmmInclinationDeg = geo.inclination.toDouble()
        )
    }

    /**
     * Exact MASA (com.labmu.masa) spherical great-circle initial bearing formula.
     */
    fun computeMasaSpherical(latDeg: Double, lonDeg: Double): Double {
        val lat1 = Math.toRadians(latDeg)
        val lon1 = Math.toRadians(lonDeg)
        val lat2 = Math.toRadians(MASA_KAABA_LAT)
        val lon2 = Math.toRadians(MASA_KAABA_LON)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return wrap360(Math.toDegrees(atan2(y, x)))
    }

    /**
     * Exact NU Online (package:adhan QiblaUtil) tangent-form spherical formula.
     */
    fun computeNuAdhanSpherical(latDeg: Double, lonDeg: Double): Double {
        val lat1 = Math.toRadians(latDeg)
        val lon1 = Math.toRadians(lonDeg)
        val lat2 = Math.toRadians(NU_KAABA_LAT)
        val lon2 = Math.toRadians(NU_KAABA_LON)
        val dLon = lon2 - lon1
        val y = sin(dLon)
        val x = cos(lat1) * tan(lat2) - sin(lat1) * cos(dLon)
        val deg = Math.toDegrees(atan2(y, x))
        return deg - floor(deg / 360.0) * 360.0
    }

    /**
     * Vincenty inverse geodesic solution on WGS84 ellipsoid.
     * Returns Triple(forwardAzimuthDeg, distanceKm, iterations).
     */
    fun computeVincenty(
        lat1Deg: Double,
        lon1Deg: Double,
        lat2Deg: Double,
        lon2Deg: Double
    ): Triple<Double, Double, Int> {
        val phi1 = Math.toRadians(lat1Deg)
        val phi2 = Math.toRadians(lat2Deg)
        val u1 = atan2((1.0 - WGS84_F) * sin(phi1), cos(phi1))
        val u2 = atan2((1.0 - WGS84_F) * sin(phi2), cos(phi2))
        val l = Math.toRadians(lon2Deg - lon1Deg)

        val sinU1 = sin(u1)
        val cosU1 = cos(u1)
        val sinU2 = sin(u2)
        val cosU2 = cos(u2)

        var lambda = l
        var sinSigma = 0.0
        var cosSigma = 0.0
        var sigma = 0.0
        var cosSqAlpha = 0.0
        var cos2SigmaM = 0.0
        var iters = 0

        for (i in 1..200) {
            iters = i
            val sinLam = sin(lambda)
            val cosLam = cos(lambda)
            sinSigma = sqrt(
                (cosU2 * sinLam).pow(2) +
                    (cosU1 * sinU2 - sinU1 * cosU2 * cosLam).pow(2)
            )
            if (sinSigma == 0.0) {
                return Triple(0.0, 0.0, i)
            }
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLam
            sigma = atan2(sinSigma, cosSigma)
            val sinAlpha = cosU1 * cosU2 * sinLam / sinSigma
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha
            cos2SigmaM = if (cosSqAlpha != 0.0) {
                cosSigma - 2.0 * sinU1 * sinU2 / cosSqAlpha
            } else {
                0.0
            }
            val c = (WGS84_F / 16.0) * cosSqAlpha * (4.0 + WGS84_F * (4.0 - 3.0 * cosSqAlpha))
            val lambdaPrev = lambda
            lambda = l + (1.0 - c) * WGS84_F * sinAlpha * (
                sigma + c * sinSigma * (
                    cos2SigmaM + c * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)
                )
            )
            if (abs(lambda - lambdaPrev) < 1e-12) break
        }

        val uSq = cosSqAlpha * (WGS84_A * WGS84_A - WGS84_B * WGS84_B) / (WGS84_B * WGS84_B)
        val capA = 1.0 + (uSq / 16384.0) * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val capB = (uSq / 1024.0) * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))
        val deltaSigma = capB * sinSigma * (
            cos2SigmaM + (capB / 4.0) * (
                cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                    (capB / 6.0) * cos2SigmaM *
                    (-3.0 + 4.0 * sinSigma * sinSigma) *
                    (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)
            )
        )
        val distanceM = WGS84_B * capA * (sigma - deltaSigma)
        val alpha1 = atan2(
            cosU2 * sin(lambda),
            cosU1 * sinU2 - sinU1 * cosU2 * cos(lambda)
        )
        return Triple(wrap360(Math.toDegrees(alpha1)), distanceM / 1000.0, iters)
    }

    fun computeCircularStats(anglesDeg: List<Double>): CircularStats {
        if (anglesDeg.isEmpty()) {
            return CircularStats(0, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
        }
        var sumSin = 0.0
        var sumCos = 0.0
        for (a in anglesDeg) {
            val r = Math.toRadians(a)
            sumSin += sin(r)
            sumCos += cos(r)
        }
        val n = anglesDeg.size.toDouble()
        val meanRad = atan2(sumSin, sumCos)
        val meanDeg = wrap360(Math.toDegrees(meanRad))
        val rLen = (sqrt(sumSin * sumSin + sumCos * sumCos) / n).coerceIn(1e-12, 1.0)
        val stdDeg = Math.toDegrees(sqrt(-2.0 * ln(rLen)))

        // Compute signed min/max spread relative to circular mean
        var minSigned = 0.0
        var maxSigned = 0.0
        for (a in anglesDeg) {
            val d = signedDeltaDeg(a, meanDeg)
            if (d < minSigned) minSigned = d
            if (d > maxSigned) maxSigned = d
        }
        return CircularStats(
            count = anglesDeg.size,
            meanDeg = meanDeg,
            stdDeg = stdDeg,
            minDeg = wrap360(meanDeg + minSigned),
            maxDeg = wrap360(meanDeg + maxSigned)
        )
    }
}
