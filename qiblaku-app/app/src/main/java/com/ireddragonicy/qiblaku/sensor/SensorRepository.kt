package com.ireddragonicy.qiblaku.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.ireddragonicy.qiblaku.math.QiblaCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class SensorSnapshot(
    val timestampNs: Long = 0L,
    val hasRotationVector: Boolean = false,
    val hasGameRotationVector: Boolean = false,
    val hasGeoMagRotationVector: Boolean = false,
    val hasGyroscope: Boolean = false,
    val rotVecMagDeg: Double = Double.NaN,
    val rotVecEstErrDeg: Double = Double.NaN,
    val pitchDeg: Double = 0.0,
    val rollDeg: Double = 0.0,
    val gameYawDeg: Double = Double.NaN,
    val geoMagOnlyMagDeg: Double = Double.NaN,
    val lpf45MagDeg: Double = Double.NaN,
    val lpf15MagDeg: Double = Double.NaN,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val gyroNormDps: Double = 0.0,
    val gyroUncalBiasNormDps: Double = Double.NaN,
    val accX: Float = 0f,
    val accY: Float = 0f,
    val accZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val magTotalUt: Double = 0.0,
    val magHardIronNormUt: Double = Double.NaN,
    val magAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val rotVecAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val gyroAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val gyroTrace: List<Float> = emptyList(),
    val magTotalTrace: List<Float> = emptyList(),
    val rotVecTrace: List<Float> = emptyList(),
    val geoMagTrace: List<Float> = emptyList()
)

class SensorRepository(context: Context) : SensorEventListener {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotVecSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gameRotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val geoMagRotSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gyroUncalSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
    private val accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val magUncalSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)

    private val _state = MutableStateFlow(
        SensorSnapshot(
            hasRotationVector = rotVecSensor != null,
            hasGameRotationVector = gameRotSensor != null,
            hasGeoMagRotationVector = geoMagRotSensor != null,
            hasGyroscope = gyroSensor != null
        )
    )
    val state: StateFlow<SensorSnapshot> = _state.asStateFlow()

    @Volatile
    var onRawSampleListener: ((SensorSnapshot) -> Unit)? = null

    private var isStarted = false
    private var lastUiEmitMs = 0L

    // Scratch buffers
    private val rotMat = FloatArray(9)
    private val gameRotMat = FloatArray(9)
    private val geoMagRotMat = FloatArray(9)
    private val lpfMat = FloatArray(9)
    private val orient = FloatArray(3)

    // Raw & LPF states
    private val accRaw = FloatArray(3)
    private val magRaw = FloatArray(3)
    private val accLpf45 = FloatArray(3)
    private val magLpf45 = FloatArray(3)
    private val accLpf15 = FloatArray(3)
    private val magLpf15 = FloatArray(3)
    private var hasAcc = false
    private var hasMag = false

    private var rotVecMagDeg = Double.NaN
    private var rotVecEstErrDeg = Double.NaN
    private var pitchDeg = 0.0
    private var rollDeg = 0.0
    private var gameYawDeg = Double.NaN
    private var geoMagOnlyMagDeg = Double.NaN
    private var lpf45MagDeg = Double.NaN
    private var lpf15MagDeg = Double.NaN

    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f
    private var gyroNormDps = 0.0
    private var gyroUncalBiasNormDps = Double.NaN
    private var magTotalUt = 0.0
    private var magHardIronNormUt = Double.NaN

    private var magAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
    private var rotVecAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
    private var gyroAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE

    private val traceLock = Any()
    private val gyroTrace = ArrayDeque<Float>(MAX_TRACE)
    private val magTotalTrace = ArrayDeque<Float>(MAX_TRACE)
    private val rotVecTrace = ArrayDeque<Float>(MAX_TRACE)
    private val geoMagTrace = ArrayDeque<Float>(MAX_TRACE)

    fun start() {
        if (isStarted) return
        isStarted = true
        val delay = SensorManager.SENSOR_DELAY_GAME
        listOfNotNull(
            rotVecSensor,
            gameRotSensor,
            geoMagRotSensor,
            gyroSensor,
            gyroUncalSensor,
            accSensor,
            magSensor,
            magUncalSensor
        ).forEach { sensor ->
            sensorManager.registerListener(this, sensor, delay)
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (sensor?.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> magAccuracy = accuracy
            Sensor.TYPE_ROTATION_VECTOR -> rotVecAccuracy = accuracy
            Sensor.TYPE_GYROSCOPE -> gyroAccuracy = accuracy
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val vecLen = minOf(4, event.values.size)
                val vec = event.values.copyOfRange(0, vecLen)
                SensorManager.getRotationMatrixFromVector(rotMat, vec)
                SensorManager.getOrientation(rotMat, orient)
                rotVecMagDeg = QiblaCalculator.wrap360(Math.toDegrees(orient[0].toDouble()))
                pitchDeg = Math.toDegrees(orient[1].toDouble())
                rollDeg = Math.toDegrees(orient[2].toDouble())
                rotVecEstErrDeg = if (event.values.size >= 5 && event.values[4] >= 0f) {
                    Math.toDegrees(event.values[4].toDouble())
                } else {
                    Double.NaN
                }
                rotVecAccuracy = event.accuracy

                val snap = buildSnapshot(event.timestamp, recordTrace = true)
                onRawSampleListener?.invoke(snap)
                maybeEmitUi(snap)
            }

            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val vecLen = minOf(4, event.values.size)
                val vec = event.values.copyOfRange(0, vecLen)
                SensorManager.getRotationMatrixFromVector(gameRotMat, vec)
                SensorManager.getOrientation(gameRotMat, orient)
                gameYawDeg = QiblaCalculator.wrap360(Math.toDegrees(orient[0].toDouble()))
            }

            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                val vecLen = minOf(4, event.values.size)
                val vec = event.values.copyOfRange(0, vecLen)
                SensorManager.getRotationMatrixFromVector(geoMagRotMat, vec)
                SensorManager.getOrientation(geoMagRotMat, orient)
                geoMagOnlyMagDeg = QiblaCalculator.wrap360(Math.toDegrees(orient[0].toDouble()))
            }

            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0]
                gyroY = event.values[1]
                gyroZ = event.values[2]
                val normRad = sqrt(
                    (gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ).toDouble()
                )
                gyroNormDps = Math.toDegrees(normRad)
                gyroAccuracy = event.accuracy
            }

            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                if (event.values.size >= 6) {
                    val bx = event.values[3]
                    val by = event.values[4]
                    val bz = event.values[5]
                    gyroUncalBiasNormDps = Math.toDegrees(
                        sqrt((bx * bx + by * by + bz * bz).toDouble())
                    )
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accRaw, 0, 3)
                if (!hasAcc) {
                    System.arraycopy(accRaw, 0, accLpf45, 0, 3)
                    System.arraycopy(accRaw, 0, accLpf15, 0, 3)
                    hasAcc = true
                } else {
                    applyLpf(accRaw, accLpf45, 0.45f)
                    applyLpf(accRaw, accLpf15, 0.15f)
                }
                updateLpfAzimuths()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magRaw, 0, 3)
                magTotalUt = sqrt(
                    (magRaw[0] * magRaw[0] + magRaw[1] * magRaw[1] + magRaw[2] * magRaw[2]).toDouble()
                )
                magAccuracy = event.accuracy
                if (!hasMag) {
                    System.arraycopy(magRaw, 0, magLpf45, 0, 3)
                    System.arraycopy(magRaw, 0, magLpf15, 0, 3)
                    hasMag = true
                } else {
                    applyLpf(magRaw, magLpf45, 0.45f)
                    applyLpf(magRaw, magLpf15, 0.15f)
                }
                updateLpfAzimuths()

                if (rotVecSensor == null) {
                    val snap = buildSnapshot(event.timestamp, recordTrace = true)
                    onRawSampleListener?.invoke(snap)
                    maybeEmitUi(snap)
                }
            }

            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> {
                if (event.values.size >= 6) {
                    val bx = event.values[3]
                    val by = event.values[4]
                    val bz = event.values[5]
                    magHardIronNormUt = sqrt((bx * bx + by * by + bz * bz).toDouble())
                }
            }
        }
    }

    private fun applyLpf(input: FloatArray, state: FloatArray, alpha: Float) {
        for (i in 0..2) {
            state[i] = state[i] + alpha * (input[i] - state[i])
        }
    }

    private fun updateLpfAzimuths() {
        if (!hasAcc || !hasMag) return
        if (SensorManager.getRotationMatrix(lpfMat, null, accLpf45, magLpf45)) {
            SensorManager.getOrientation(lpfMat, orient)
            lpf45MagDeg = QiblaCalculator.wrap360(Math.toDegrees(orient[0].toDouble()))
        }
        if (SensorManager.getRotationMatrix(lpfMat, null, accLpf15, magLpf15)) {
            SensorManager.getOrientation(lpfMat, orient)
            lpf15MagDeg = QiblaCalculator.wrap360(Math.toDegrees(orient[0].toDouble()))
        }
    }

    private fun buildSnapshot(timestampNs: Long, recordTrace: Boolean): SensorSnapshot {
        val gList: List<Float>
        val mList: List<Float>
        val rList: List<Float>
        val gmList: List<Float>

        synchronized(traceLock) {
            if (recordTrace) {
                pushTrace(gyroTrace, gyroNormDps.toFloat())
                pushTrace(magTotalTrace, magTotalUt.toFloat())
                if (rotVecMagDeg.isFinite()) pushTrace(rotVecTrace, rotVecMagDeg.toFloat())
                if (geoMagOnlyMagDeg.isFinite()) pushTrace(geoMagTrace, geoMagOnlyMagDeg.toFloat())
            }
            gList = gyroTrace.toList()
            mList = magTotalTrace.toList()
            rList = rotVecTrace.toList()
            gmList = geoMagTrace.toList()
        }

        return SensorSnapshot(
            timestampNs = timestampNs,
            hasRotationVector = rotVecSensor != null,
            hasGameRotationVector = gameRotSensor != null,
            hasGeoMagRotationVector = geoMagRotSensor != null,
            hasGyroscope = gyroSensor != null,
            rotVecMagDeg = rotVecMagDeg,
            rotVecEstErrDeg = rotVecEstErrDeg,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            gameYawDeg = gameYawDeg,
            geoMagOnlyMagDeg = geoMagOnlyMagDeg,
            lpf45MagDeg = lpf45MagDeg,
            lpf15MagDeg = lpf15MagDeg,
            gyroX = gyroX,
            gyroY = gyroY,
            gyroZ = gyroZ,
            gyroNormDps = gyroNormDps,
            gyroUncalBiasNormDps = gyroUncalBiasNormDps,
            accX = accRaw[0],
            accY = accRaw[1],
            accZ = accRaw[2],
            magX = magRaw[0],
            magY = magRaw[1],
            magZ = magRaw[2],
            magTotalUt = magTotalUt,
            magHardIronNormUt = magHardIronNormUt,
            magAccuracy = magAccuracy,
            rotVecAccuracy = rotVecAccuracy,
            gyroAccuracy = gyroAccuracy,
            gyroTrace = gList,
            magTotalTrace = mList,
            rotVecTrace = rList,
            geoMagTrace = gmList
        )
    }

    private fun pushTrace(deque: ArrayDeque<Float>, value: Float) {
        if (deque.size >= MAX_TRACE) deque.removeFirst()
        deque.addLast(value)
    }

    private fun maybeEmitUi(snap: SensorSnapshot) {
        val now = System.currentTimeMillis()
        if (now - lastUiEmitMs >= 33L) {
            lastUiEmitMs = now
            _state.value = snap
        }
    }

    companion object {
        private const val MAX_TRACE = 120

        fun accuracyLabel(acc: Int): String = when (acc) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH (±15°)"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MED (±30°)"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW (±45°)"
            else -> "UNRELIABLE"
        }
    }
}
