package com.ireddragonicy.qiblaku.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ireddragonicy.qiblaku.location.LocationSnapshot
import com.ireddragonicy.qiblaku.math.QiblaCalculator
import com.ireddragonicy.qiblaku.math.QiblaTheoretical
import com.ireddragonicy.qiblaku.sensor.SensorSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CampusPreset(val id: String, val label: String, val shortLabel: String) {
    KAMPUS_1("K1_UAD", "Masjid Kampus 1 UAD", "K1"),
    KAMPUS_2("K2_UAD", "Masjid Kampus 2 UAD", "K2"),
    KAMPUS_3("K3_UAD", "Masjid Kampus 3 UAD", "K3"),
    KAMPUS_4("K4_UAD", "Masjid Kampus 4 UAD", "K4"),
    KAMPUS_5("K5_UAD", "Masjid Kampus 5 UAD", "K5")
}

enum class PosePreset(val id: String, val label: String) {
    SHAF_UTAMA("Shaf_Utama", "Shaf Utama (Wajib)"),
    SERAMBI("Serambi_Luar", "Serambi (Opsional)"),
    HALAMAN("Halaman", "Halaman (Opsional)")
}

data class SessionSummary(
    val id: String,
    val createdAtIso: String,
    val campusId: String,
    val campusLabel: String,
    val poseId: String,
    val lat: Double,
    val lon: Double,
    val altM: Double,
    val gpsAccM: Float,
    val declinationDeg: Double,
    val wmmFieldUt: Double,
    val wgs84QiblaDeg: Double,
    val wgs84DistanceKm: Double,
    val masaFormulaDeg: Double,
    val nuFormulaDeg: Double,
    val durationSec: Double,
    val sampleCount: Int,
    val rotVecTrueMeanDeg: Double,
    val rotVecTrueStdDeg: Double,
    val rotVecMagMeanDeg: Double,
    val geoMagTrueMeanDeg: Double,
    val geoMagTrueStdDeg: Double,
    val gameYawStdDeg: Double,
    val lpf45TrueMeanDeg: Double,
    val lpf15TrueMeanDeg: Double,
    val deviationVsWgs84Deg: Double,
    val gyroMeanDps: Double,
    val magTotalMeanUt: Double,
    val magAnomalyUt: Double,
    val pitchMeanDeg: Double,
    val rollMeanDeg: Double,
    val rawCsvFileName: String,
    val rawInternalPath: String,
    val masaObservedDeg: Double? = null,
    val nuObservedDeg: Double? = null,
    val notes: String = ""
)

data class RecordingState(
    val isRecording: Boolean = false,
    val targetDurationSec: Int? = 15,
    val elapsedSec: Double = 0.0,
    val sampleCount: Int = 0,
    val activeFileName: String = "",
    val lastSavedMessage: String? = null
)

class SessionRecorder(private val context: Context) {
    private val logsDir: File = File(context.filesDir, "logs").apply { mkdirs() }
    private val sessionsJsonFile: File = File(context.filesDir, "sessions.json")

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionSummary>>(loadSessions())
    val sessions: StateFlow<List<SessionSummary>> = _sessions.asStateFlow()

    private var activeWriter: BufferedWriter? = null
    private var activeFile: File? = null
    private var activeCampus: CampusPreset = CampusPreset.KAMPUS_1
    private var activePose: PosePreset = PosePreset.SHAF_UTAMA
    private var activeLoc: LocationSnapshot? = null
    private var activeTheory: QiblaTheoretical? = null
    private var startMs: Long = 0L
    private var startIso: String = ""
    private var targetDurationSec: Int? = 15

    // Accumulators
    private val rotTrueList = mutableListOf<Double>()
    private val rotMagList = mutableListOf<Double>()
    private val geoTrueList = mutableListOf<Double>()
    private val gameYawList = mutableListOf<Double>()
    private val lpf45TrueList = mutableListOf<Double>()
    private val lpf15TrueList = mutableListOf<Double>()
    private var gyroSum = 0.0
    private var magSum = 0.0
    private var pitchSum = 0.0
    private var rollSum = 0.0
    private var sampleCount = 0

    @Synchronized
    fun startRecording(
        campus: CampusPreset,
        pose: PosePreset,
        loc: LocationSnapshot,
        theory: QiblaTheoretical,
        durationSec: Int?
    ): Boolean {
        if (_recordingState.value.isRecording || !loc.hasFix) return false

        val now = Date()
        val fileTs = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now)
        startIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(now)
        val fileName = "raw_${campus.id}_${pose.id}_$fileTs.csv"
        val file = File(logsDir, fileName)

        activeWriter = BufferedWriter(FileWriter(file, false)).apply {
            write(
                "timestamp_ms,elapsed_s,campus,pose,lat,lon,alt_m,gps_acc_m," +
                    "declination_deg,wmm_field_ut,wgs84_qibla_deg,masa_formula_deg,nu_formula_deg," +
                    "rotvec_mag_deg,rotvec_true_deg,rotvec_est_err_deg," +
                    "geomag_only_mag_deg,geomag_only_true_deg,game_yaw_deg," +
                    "lpf45_mag_deg,lpf15_mag_deg,pitch_deg,roll_deg," +
                    "gyro_x,gyro_y,gyro_z,gyro_norm_dps," +
                    "acc_x,acc_y,acc_z,mag_x,mag_y,mag_z,mag_total_ut,mag_anomaly_ut," +
                    "mag_accuracy,rotvec_accuracy\n"
            )
        }

        activeFile = file
        activeCampus = campus
        activePose = pose
        activeLoc = loc
        activeTheory = theory
        startMs = System.currentTimeMillis()
        targetDurationSec = durationSec

        rotTrueList.clear()
        rotMagList.clear()
        geoTrueList.clear()
        gameYawList.clear()
        lpf45TrueList.clear()
        lpf15TrueList.clear()
        gyroSum = 0.0
        magSum = 0.0
        pitchSum = 0.0
        rollSum = 0.0
        sampleCount = 0

        _recordingState.value = RecordingState(
            isRecording = true,
            targetDurationSec = durationSec,
            elapsedSec = 0.0,
            sampleCount = 0,
            activeFileName = fileName,
            lastSavedMessage = null
        )
        return true
    }

    @Synchronized
    fun onSensorSample(snap: SensorSnapshot): Boolean {
        if (!_recordingState.value.isRecording) return false
        val writer = activeWriter ?: return false
        val loc = activeLoc ?: return false
        val theory = activeTheory ?: return false

        val nowMs = System.currentTimeMillis()
        val elapsedS = (nowMs - startMs) / 1000.0
        val decl = theory.declinationDeg

        val rotTrue = if (snap.rotVecMagDeg.isFinite()) {
            QiblaCalculator.wrap360(snap.rotVecMagDeg + decl)
        } else Double.NaN
        val geoTrue = if (snap.geoMagOnlyMagDeg.isFinite()) {
            QiblaCalculator.wrap360(snap.geoMagOnlyMagDeg + decl)
        } else Double.NaN
        val lpf45True = if (snap.lpf45MagDeg.isFinite()) {
            QiblaCalculator.wrap360(snap.lpf45MagDeg + decl)
        } else Double.NaN
        val lpf15True = if (snap.lpf15MagDeg.isFinite()) {
            QiblaCalculator.wrap360(snap.lpf15MagDeg + decl)
        } else Double.NaN

        val anomalyUt = snap.magTotalUt - theory.wmmExpectedFieldUt

        writer.write(
            String.format(
                Locale.US,
                "%d,%.3f,%s,%s,%.7f,%.7f,%.2f,%.2f," +
                    "%.4f,%.3f,%.5f,%.5f,%.5f," +
                    "%.4f,%.4f,%.3f," +
                    "%.4f,%.4f,%.4f," +
                    "%.4f,%.4f,%.3f,%.3f," +
                    "%.5f,%.5f,%.5f,%.4f," +
                    "%.4f,%.4f,%.4f,%.3f,%.3f,%.3f,%.3f,%.3f," +
                    "%d,%d\n",
                nowMs,
                elapsedS,
                activeCampus.id,
                activePose.id,
                loc.latitude ?: 0.0,
                loc.longitude ?: 0.0,
                loc.altitudeM,
                loc.accuracyM ?: -1f,
                decl,
                theory.wmmExpectedFieldUt,
                theory.wgs84BearingDeg,
                theory.masaSphericalDeg,
                theory.nuSphericalDeg,
                snap.rotVecMagDeg,
                rotTrue,
                snap.rotVecEstErrDeg,
                snap.geoMagOnlyMagDeg,
                geoTrue,
                snap.gameYawDeg,
                snap.lpf45MagDeg,
                snap.lpf15MagDeg,
                snap.pitchDeg,
                snap.rollDeg,
                snap.gyroX,
                snap.gyroY,
                snap.gyroZ,
                snap.gyroNormDps,
                snap.accX,
                snap.accY,
                snap.accZ,
                snap.magX,
                snap.magY,
                snap.magZ,
                snap.magTotalUt,
                anomalyUt,
                snap.magAccuracy,
                snap.rotVecAccuracy
            )
        )

        if (rotTrue.isFinite()) rotTrueList.add(rotTrue)
        if (snap.rotVecMagDeg.isFinite()) rotMagList.add(snap.rotVecMagDeg)
        if (geoTrue.isFinite()) geoTrueList.add(geoTrue)
        if (snap.gameYawDeg.isFinite()) gameYawList.add(snap.gameYawDeg)
        if (lpf45True.isFinite()) lpf45TrueList.add(lpf45True)
        if (lpf15True.isFinite()) lpf15TrueList.add(lpf15True)
        gyroSum += snap.gyroNormDps
        magSum += snap.magTotalUt
        pitchSum += snap.pitchDeg
        rollSum += snap.rollDeg
        sampleCount++

        _recordingState.value = _recordingState.value.copy(
            elapsedSec = elapsedS,
            sampleCount = sampleCount
        )

        val target = targetDurationSec
        if (target != null && elapsedS >= target) {
            stopRecording()
            return true
        }
        return false
    }

    @Synchronized
    fun stopRecording(): SessionSummary? {
        if (!_recordingState.value.isRecording) return null
        val file = activeFile
        val loc = activeLoc
        val theory = activeTheory

        try {
            activeWriter?.flush()
            activeWriter?.close()
        } catch (_: Exception) {
        }
        activeWriter = null

        if (file == null || loc == null || theory == null || sampleCount == 0) {
            _recordingState.value = RecordingState(
                isRecording = false,
                targetDurationSec = targetDurationSec,
                lastSavedMessage = "Rekaman dibatalkan (0 sampel)"
            )
            return null
        }

        val elapsedS = (System.currentTimeMillis() - startMs) / 1000.0
        val rotTrueStats = QiblaCalculator.computeCircularStats(rotTrueList)
        val rotMagStats = QiblaCalculator.computeCircularStats(rotMagList)
        val geoStats = QiblaCalculator.computeCircularStats(geoTrueList)
        val gameStats = QiblaCalculator.computeCircularStats(gameYawList)
        val lpf45Stats = QiblaCalculator.computeCircularStats(lpf45TrueList)
        val lpf15Stats = QiblaCalculator.computeCircularStats(lpf15TrueList)

        val magMeanUt = magSum / sampleCount
        val deviation = QiblaCalculator.signedDeltaDeg(
            rotTrueStats.meanDeg,
            theory.wgs84BearingDeg
        )

        val summary = SessionSummary(
            id = UUID.randomUUID().toString(),
            createdAtIso = startIso,
            campusId = activeCampus.id,
            campusLabel = activeCampus.label,
            poseId = activePose.id,
            lat = loc.latitude ?: 0.0,
            lon = loc.longitude ?: 0.0,
            altM = loc.altitudeM,
            gpsAccM = loc.accuracyM ?: -1f,
            declinationDeg = theory.declinationDeg,
            wmmFieldUt = theory.wmmExpectedFieldUt,
            wgs84QiblaDeg = theory.wgs84BearingDeg,
            wgs84DistanceKm = theory.wgs84DistanceKm,
            masaFormulaDeg = theory.masaSphericalDeg,
            nuFormulaDeg = theory.nuSphericalDeg,
            durationSec = elapsedS,
            sampleCount = sampleCount,
            rotVecTrueMeanDeg = rotTrueStats.meanDeg,
            rotVecTrueStdDeg = rotTrueStats.stdDeg,
            rotVecMagMeanDeg = rotMagStats.meanDeg,
            geoMagTrueMeanDeg = geoStats.meanDeg,
            geoMagTrueStdDeg = geoStats.stdDeg,
            gameYawStdDeg = gameStats.stdDeg,
            lpf45TrueMeanDeg = lpf45Stats.meanDeg,
            lpf15TrueMeanDeg = lpf15Stats.meanDeg,
            deviationVsWgs84Deg = deviation,
            gyroMeanDps = gyroSum / sampleCount,
            magTotalMeanUt = magMeanUt,
            magAnomalyUt = magMeanUt - theory.wmmExpectedFieldUt,
            pitchMeanDeg = pitchSum / sampleCount,
            rollMeanDeg = rollSum / sampleCount,
            rawCsvFileName = file.name,
            rawInternalPath = file.absolutePath
        )

        exportFileToPublicDocuments(file, file.name, "text/csv")

        val updated = listOf(summary) + _sessions.value
        _sessions.value = updated
        persistSessions(updated)
        exportSummaryCsv(updated)

        _recordingState.value = RecordingState(
            isRecording = false,
            targetDurationSec = targetDurationSec,
            lastSavedMessage = "Tersimpan: ${file.name} ($sampleCount sampel)"
        )
        return summary
    }

    fun updateComparison(
        sessionId: String,
        masaObservedDeg: Double?,
        nuObservedDeg: Double?,
        notes: String
    ) {
        val updated = _sessions.value.map { s ->
            if (s.id == sessionId) {
                s.copy(
                    masaObservedDeg = masaObservedDeg,
                    nuObservedDeg = nuObservedDeg,
                    notes = notes
                )
            } else s
        }
        _sessions.value = updated
        persistSessions(updated)
        exportSummaryCsv(updated)
    }

    fun deleteSession(sessionId: String) {
        val target = _sessions.value.firstOrNull { it.id == sessionId }
        if (target != null) {
            runCatching { File(target.rawInternalPath).delete() }
            deletePublicDocument(target.rawCsvFileName)
        }
        val updated = _sessions.value.filterNot { it.id == sessionId }
        _sessions.value = updated
        persistSessions(updated)
        exportSummaryCsv(updated)
    }

    private fun deletePublicDocument(displayName: String) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val relPath = "${Environment.DIRECTORY_DOCUMENTS}/QiblaKu"
                resolver.query(
                    MediaStore.Files.getContentUri("external"),
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                    arrayOf(displayName, "$relPath/"),
                    null
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val uri = MediaStore.Files.getContentUri("external", id)
                        resolver.delete(uri, null, null)
                    }
                }
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "QiblaKu"
                )
                File(dir, displayName).delete()
            }
        }
    }

    fun getSummaryCsvFile(): File {
        val f = File(logsDir, "summary_all_sessions.csv")
        if (!f.exists()) exportSummaryCsv(_sessions.value)
        return f
    }

    private fun exportSummaryCsv(list: List<SessionSummary>) {
        val file = File(logsDir, "summary_all_sessions.csv")
        BufferedWriter(FileWriter(file, false)).use { w ->
            w.write(
                "created_at,campus_id,campus_label,pose,lat,lon,alt_m,gps_acc_m," +
                    "declination_deg,wmm_field_ut,wgs84_qibla_deg,wgs84_dist_km," +
                    "masa_formula_deg,nu_formula_deg,duration_s,samples," +
                    "rotvec_true_mean_deg,rotvec_true_std_deg,rotvec_mag_mean_deg," +
                    "geomag_true_mean_deg,geomag_true_std_deg,game_yaw_std_deg," +
                    "lpf45_true_mean_deg,lpf15_true_mean_deg,deviation_vs_wgs84_deg," +
                    "gyro_mean_dps,mag_total_mean_ut,mag_anomaly_ut,pitch_mean_deg,roll_mean_deg," +
                    "masa_observed_deg,nu_observed_deg,notes,raw_csv\n"
            )
            for (s in list) {
                val safeNote = s.notes
                    .replace("\"", "'")
                    .replace(",", ";")
                    .replace("\r", " ")
                    .replace("\n", " ")
                w.write(
                    String.format(
                        Locale.US,
                        "%s,%s,\"%s\",%s,%.7f,%.7f,%.2f,%.2f," +
                            "%.4f,%.3f,%.5f,%.3f," +
                            "%.5f,%.5f,%.2f,%d," +
                            "%.4f,%.4f,%.4f," +
                            "%.4f,%.4f,%.4f," +
                            "%.4f,%.4f,%.4f," +
                            "%.4f,%.3f,%.3f,%.3f,%.3f," +
                            "%s,%s,\"%s\",%s\n",
                        s.createdAtIso,
                        s.campusId,
                        s.campusLabel,
                        s.poseId,
                        s.lat,
                        s.lon,
                        s.altM,
                        s.gpsAccM,
                        s.declinationDeg,
                        s.wmmFieldUt,
                        s.wgs84QiblaDeg,
                        s.wgs84DistanceKm,
                        s.masaFormulaDeg,
                        s.nuFormulaDeg,
                        s.durationSec,
                        s.sampleCount,
                        s.rotVecTrueMeanDeg,
                        s.rotVecTrueStdDeg,
                        s.rotVecMagMeanDeg,
                        s.geoMagTrueMeanDeg,
                        s.geoMagTrueStdDeg,
                        s.gameYawStdDeg,
                        s.lpf45TrueMeanDeg,
                        s.lpf15TrueMeanDeg,
                        s.deviationVsWgs84Deg,
                        s.gyroMeanDps,
                        s.magTotalMeanUt,
                        s.magAnomalyUt,
                        s.pitchMeanDeg,
                        s.rollMeanDeg,
                        s.masaObservedDeg?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        s.nuObservedDeg?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        safeNote,
                        s.rawCsvFileName
                    )
                )
            }
        }
        exportFileToPublicDocuments(file, "summary_all_sessions.csv", "text/csv")
    }

    private fun exportFileToPublicDocuments(source: File, displayName: String, mimeType: String) {
        if (!source.exists()) return
        try {
            deletePublicDocument(displayName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val relPath = "${Environment.DIRECTORY_DOCUMENTS}/QiblaKu"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                }
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        source.inputStream().use { inp -> inp.copyTo(out) }
                    }
                }
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "QiblaKu"
                )
                dir.mkdirs()
                source.copyTo(File(dir, displayName), overwrite = true)
            }
        } catch (_: Exception) {
            // Internal copy in filesDir/logs/ always remains available
        }
    }

    private fun persistSessions(list: List<SessionSummary>) {
        val arr = JSONArray()
        for (s in list) {
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("createdAtIso", s.createdAtIso)
                    put("campusId", s.campusId)
                    put("campusLabel", s.campusLabel)
                    put("poseId", s.poseId)
                    put("lat", s.lat)
                    put("lon", s.lon)
                    put("altM", s.altM)
                    put("gpsAccM", s.gpsAccM.toDouble())
                    put("declinationDeg", s.declinationDeg)
                    put("wmmFieldUt", s.wmmFieldUt)
                    put("wgs84QiblaDeg", s.wgs84QiblaDeg)
                    put("wgs84DistanceKm", s.wgs84DistanceKm)
                    put("masaFormulaDeg", s.masaFormulaDeg)
                    put("nuFormulaDeg", s.nuFormulaDeg)
                    put("durationSec", s.durationSec)
                    put("sampleCount", s.sampleCount)
                    put("rotVecTrueMeanDeg", s.rotVecTrueMeanDeg)
                    put("rotVecTrueStdDeg", s.rotVecTrueStdDeg)
                    put("rotVecMagMeanDeg", s.rotVecMagMeanDeg)
                    put("geoMagTrueMeanDeg", s.geoMagTrueMeanDeg)
                    put("geoMagTrueStdDeg", s.geoMagTrueStdDeg)
                    put("gameYawStdDeg", s.gameYawStdDeg)
                    put("lpf45TrueMeanDeg", s.lpf45TrueMeanDeg)
                    put("lpf15TrueMeanDeg", s.lpf15TrueMeanDeg)
                    put("deviationVsWgs84Deg", s.deviationVsWgs84Deg)
                    put("gyroMeanDps", s.gyroMeanDps)
                    put("magTotalMeanUt", s.magTotalMeanUt)
                    put("magAnomalyUt", s.magAnomalyUt)
                    put("pitchMeanDeg", s.pitchMeanDeg)
                    put("rollMeanDeg", s.rollMeanDeg)
                    put("rawCsvFileName", s.rawCsvFileName)
                    put("rawInternalPath", s.rawInternalPath)
                    if (s.masaObservedDeg != null) put("masaObservedDeg", s.masaObservedDeg)
                    if (s.nuObservedDeg != null) put("nuObservedDeg", s.nuObservedDeg)
                    put("notes", s.notes)
                }
            )
        }
        sessionsJsonFile.writeText(arr.toString(2))
    }

    private fun loadSessions(): List<SessionSummary> {
        if (!sessionsJsonFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(sessionsJsonFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        SessionSummary(
                            id = o.getString("id"),
                            createdAtIso = o.getString("createdAtIso"),
                            campusId = o.getString("campusId"),
                            campusLabel = o.getString("campusLabel"),
                            poseId = o.getString("poseId"),
                            lat = o.getDouble("lat"),
                            lon = o.getDouble("lon"),
                            altM = o.getDouble("altM"),
                            gpsAccM = o.getDouble("gpsAccM").toFloat(),
                            declinationDeg = o.getDouble("declinationDeg"),
                            wmmFieldUt = o.getDouble("wmmFieldUt"),
                            wgs84QiblaDeg = o.getDouble("wgs84QiblaDeg"),
                            wgs84DistanceKm = o.getDouble("wgs84DistanceKm"),
                            masaFormulaDeg = o.getDouble("masaFormulaDeg"),
                            nuFormulaDeg = o.getDouble("nuFormulaDeg"),
                            durationSec = o.getDouble("durationSec"),
                            sampleCount = o.getInt("sampleCount"),
                            rotVecTrueMeanDeg = o.getDouble("rotVecTrueMeanDeg"),
                            rotVecTrueStdDeg = o.getDouble("rotVecTrueStdDeg"),
                            rotVecMagMeanDeg = o.getDouble("rotVecMagMeanDeg"),
                            geoMagTrueMeanDeg = o.getDouble("geoMagTrueMeanDeg"),
                            geoMagTrueStdDeg = o.getDouble("geoMagTrueStdDeg"),
                            gameYawStdDeg = o.getDouble("gameYawStdDeg"),
                            lpf45TrueMeanDeg = o.getDouble("lpf45TrueMeanDeg"),
                            lpf15TrueMeanDeg = o.getDouble("lpf15TrueMeanDeg"),
                            deviationVsWgs84Deg = o.getDouble("deviationVsWgs84Deg"),
                            gyroMeanDps = o.getDouble("gyroMeanDps"),
                            magTotalMeanUt = o.getDouble("magTotalMeanUt"),
                            magAnomalyUt = o.getDouble("magAnomalyUt"),
                            pitchMeanDeg = o.getDouble("pitchMeanDeg"),
                            rollMeanDeg = o.getDouble("rollMeanDeg"),
                            rawCsvFileName = o.getString("rawCsvFileName"),
                            rawInternalPath = o.getString("rawInternalPath"),
                            masaObservedDeg = if (o.has("masaObservedDeg") && !o.isNull("masaObservedDeg")) {
                                o.getDouble("masaObservedDeg")
                            } else null,
                            nuObservedDeg = if (o.has("nuObservedDeg") && !o.isNull("nuObservedDeg")) {
                                o.getDouble("nuObservedDeg")
                            } else null,
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }
}
