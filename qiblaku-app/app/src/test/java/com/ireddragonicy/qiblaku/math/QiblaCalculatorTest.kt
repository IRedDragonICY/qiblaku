package com.ireddragonicy.qiblaku.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {

    @Test
    fun verifyMasaAndNuAndVincentyAtSampleYogyakartaPoint() {
        val lat = -7.8000
        val lon = 110.3800

        val masa = QiblaCalculator.computeMasaSpherical(lat, lon)
        val nu = QiblaCalculator.computeNuAdhanSpherical(lat, lon)
        val (vincenty, distKm, iters) = QiblaCalculator.computeVincenty(
            lat,
            lon,
            QiblaCalculator.NU_KAABA_LAT,
            QiblaCalculator.NU_KAABA_LON
        )

        assertEquals(294.709566, masa, 1e-4)
        assertEquals(294.709585, nu, 1e-4)
        assertEquals(294.582318, vincenty, 1e-4)
        assertTrue(distKm in 8300.0..8450.0)
        assertTrue(iters in 1..20)
    }

    @Test
    fun verifyCircularStatsWrapAroundZero() {
        val stats = QiblaCalculator.computeCircularStats(listOf(359.0, 1.0, 358.0, 2.0))
        assertEquals(0.0, QiblaCalculator.signedDeltaDeg(stats.meanDeg, 0.0), 1e-6)
        assertTrue(stats.stdDeg < 2.0)
    }

    @Test
    fun verifySignedDelta() {
        assertEquals(-2.0, QiblaCalculator.signedDeltaDeg(359.0, 1.0), 1e-6)
        assertEquals(2.0, QiblaCalculator.signedDeltaDeg(1.0, 359.0), 1e-6)
        assertEquals(180.0, QiblaCalculator.signedDeltaDeg(180.0, 0.0), 1e-6)
    }
}
