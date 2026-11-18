package com.ireddragonicy.qiblaku.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ireddragonicy.qiblaku.data.CampusPreset
import com.ireddragonicy.qiblaku.data.PosePreset
import com.ireddragonicy.qiblaku.data.RecordingState
import com.ireddragonicy.qiblaku.location.LocationSnapshot
import com.ireddragonicy.qiblaku.math.QiblaCalculator
import com.ireddragonicy.qiblaku.math.QiblaTheoretical
import com.ireddragonicy.qiblaku.sensor.SensorRepository
import com.ireddragonicy.qiblaku.sensor.SensorSnapshot
import com.ireddragonicy.qiblaku.ui.components.AppIcons
import com.ireddragonicy.qiblaku.ui.components.CompassDialCanvas
import com.ireddragonicy.qiblaku.ui.theme.CardBorder
import com.ireddragonicy.qiblaku.ui.theme.CardDark
import com.ireddragonicy.qiblaku.ui.theme.Coral
import com.ireddragonicy.qiblaku.ui.theme.Cyan
import com.ireddragonicy.qiblaku.ui.theme.Emerald
import com.ireddragonicy.qiblaku.ui.theme.EmeraldBright
import com.ireddragonicy.qiblaku.ui.theme.Gold
import com.ireddragonicy.qiblaku.ui.theme.TextMuted
import com.ireddragonicy.qiblaku.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeasureScreen(
    selectedCampus: CampusPreset,
    onSelectCampus: (CampusPreset) -> Unit,
    selectedPose: PosePreset,
    onSelectPose: (PosePreset) -> Unit,
    selectedDurationSec: Int?,
    onSelectDurationSec: (Int?) -> Unit,
    location: LocationSnapshot,
    theory: QiblaTheoretical?,
    sensor: SensorSnapshot,
    recordingState: RecordingState,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onToggleGpsLock: () -> Unit,
    onSetManualLocation: (Double, Double, Double) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualDialog by remember { mutableStateOf(false) }

    val decl = theory?.declinationDeg ?: 0.0
    val rotVecTrue = if (sensor.rotVecMagDeg.isFinite()) {
        QiblaCalculator.wrap360(sensor.rotVecMagDeg + decl)
    } else Double.NaN
    val geoMagTrue = if (sensor.geoMagOnlyMagDeg.isFinite()) {
        QiblaCalculator.wrap360(sensor.geoMagOnlyMagDeg + decl)
    } else Double.NaN
    val lpf45True = if (sensor.lpf45MagDeg.isFinite()) {
        QiblaCalculator.wrap360(sensor.lpf45MagDeg + decl)
    } else Double.NaN
    val lpf15True = if (sensor.lpf15MagDeg.isFinite()) {
        QiblaCalculator.wrap360(sensor.lpf15MagDeg + decl)
    } else Double.NaN

    val isFlat = abs(sensor.pitchDeg) <= 2.5 && abs(sensor.rollDeg) <= 2.5
    val isTiltedTooMuch = abs(sensor.pitchDeg) > 45.0 || abs(sensor.rollDeg) > 45.0
    val magAnomalyUt = if (theory != null) sensor.magTotalUt - theory.wmmExpectedFieldUt else 0.0
    val hasMagAnomaly = theory != null && abs(magAnomalyUt) > 10.0

    val deviationDeg = if (rotVecTrue.isFinite() && theory != null) {
        QiblaCalculator.signedDeltaDeg(rotVecTrue, theory.wgs84BearingDeg)
    } else null

    val nowLabel = remember(sensor.timestampNs / 1_000_000_000L) {
        SimpleDateFormat("dd MMM yyyy • HH:mm:ss", Locale.forLanguageTag("id-ID")).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Photo Watermark Header (Campus + Timestamp)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedCampus.label,
                        color = EmeraldBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = nowLabel,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CampusPreset.entries.forEach { c ->
                        FilterChip(
                            selected = selectedCampus == c,
                            onClick = { if (!recordingState.isRecording) onSelectCampus(c) },
                            label = { Text(c.shortLabel, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald.copy(alpha = 0.25f),
                                selectedLabelColor = EmeraldBright
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PosePreset.entries.forEach { p ->
                        FilterChip(
                            selected = selectedPose == p,
                            onClick = { if (!recordingState.isRecording) onSelectPose(p) },
                            label = { Text(p.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan.copy(alpha = 0.22f),
                                selectedLabelColor = Cyan
                            )
                        )
                    }
                }
            }
        }

        // Main Compass + Readout Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, if (isFlat) Emerald.copy(alpha = 0.5f) else CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Arahkan sisi ATAS HP (+Y) tegak lurus garis shaf / ke Mihrab",
                    color = Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                CompassDialCanvas(
                    headingTrueDeg = rotVecTrue,
                    qiblaTargetDeg = theory?.wgs84BearingDeg,
                    pitchDeg = sensor.pitchDeg,
                    rollDeg = sensor.rollDeg,
                    isFlat = isFlat,
                    size = 228.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status badges (Level + Mag Accuracy + Anomaly)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusPill(
                        text = if (isFlat) "RATA (${fmt1(sensor.pitchDeg)}°, ${fmt1(sensor.rollDeg)}°)"
                        else "MIRING (${fmt1(sensor.pitchDeg)}°, ${fmt1(sensor.rollDeg)}°)",
                        color = if (isFlat) EmeraldBright else Gold
                    )
                    StatusPill(
                        text = "Mag: ${SensorRepository.accuracyLabel(sensor.magAccuracy)}",
                        color = if (sensor.magAccuracy >= 2) EmeraldBright else Coral
                    )
                    if (hasMagAnomaly) {
                        StatusPill(
                            text = "Anomali B: ${fmtSigned(magAnomalyUt)} µT",
                            color = Coral
                        )
                    }
                }

                if (isTiltedTooMuch) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PERINGATAN: Letakkan HP rata di lantai / sajadah untuk akurasi optimal!",
                        color = Coral,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                // Big numbers row: Measured True North vs Target WGS84 vs Deviation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricBox(
                        title = "HADAP HP (TRUE)",
                        value = if (rotVecTrue.isFinite()) "${fmt2(rotVecTrue)}°" else "--",
                        subtitle = "Mag: ${if (sensor.rotVecMagDeg.isFinite()) "${fmt2(sensor.rotVecMagDeg)}°" else "--"}",
                        valueColor = Cyan
                    )
                    MetricBox(
                        title = "KIBLAT WGS84",
                        value = theory?.let { "${fmt2(it.wgs84BearingDeg)}°" } ?: "--",
                        subtitle = theory?.let { "Jarak: ${fmt0(it.wgs84DistanceKm)} km" } ?: "Tunggu GPS",
                        valueColor = Gold
                    )
                    MetricBox(
                        title = "DEVIASI (Δ)",
                        value = deviationDeg?.let { "${fmtSigned2(it)}°" } ?: "--",
                        subtitle = deviationDeg?.let {
                            when {
                                abs(it) < 0.5 -> "Tepat Kiblat"
                                it > 0 -> "Kanan / Timur"
                                else -> "Kiri / Barat"
                            }
                        } ?: "-",
                        valueColor = when {
                            deviationDeg == null -> TextMuted
                            abs(deviationDeg) <= 1.0 -> EmeraldBright
                            abs(deviationDeg) <= 3.0 -> Gold
                            else -> Coral
                        }
                    )
                }
            }
        }

        // Recording Control Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(
                1.dp,
                if (recordingState.isRecording) Coral else CardBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rekam Telemetry CSV (50 Hz)",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(15 to "15s", 30 to "30s", null to "Manual").forEach { (dur, lbl) ->
                            FilterChip(
                                selected = selectedDurationSec == dur,
                                onClick = { if (!recordingState.isRecording) onSelectDurationSec(dur) },
                                label = { Text(lbl, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (recordingState.isRecording) {
                    val targetLbl = recordingState.targetDurationSec?.let { "/ ${it}s" } ?: "(Manual)"
                    Text(
                        text = "MEREKAM ${recordingState.activeFileName}\n" +
                            "Waktu: ${fmt1(recordingState.elapsedSec)}s $targetLbl  •  Sampel: ${recordingState.sampleCount}",
                        color = Coral,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStopRecord,
                        colors = ButtonDefaults.buttonColors(containerColor = Coral),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = AppIcons.Stop,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP & SIMPAN CSV", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onStartRecord,
                        enabled = location.hasFix,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = AppIcons.RecordDot,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (location.hasFix) {
                                "MULAI REKAM (${selectedCampus.shortLabel} • ${selectedPose.id})"
                            } else {
                                "MENUNGGU GPS FIX UNTUK REKAM"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                recordingState.lastSavedMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$msg\nFolder HP: Documents/QiblaKu/",
                        color = EmeraldBright,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // GPS & Geomagnetic Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS & Medan Geomagnetik (WMM)",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    if (!hasLocationPermission) {
                        TextButton(onClick = onRequestLocationPermission) {
                            Text("Izinkan GPS", color = Gold)
                        }
                    }
                }

                if (location.hasFix) {
                    Text(
                        text = "Lat: ${fmt6(location.latitude!!)}  Lon: ${fmt6(location.longitude!!)}\n" +
                            "Alt: ${fmt1(location.altitudeM)} m  •  Akurasi: ±${location.accuracyM?.let { fmt1(it.toDouble()) } ?: "?"} m (${location.provider})" +
                            if (location.isLocked) " [LOCKED]" else " [LIVE]",
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Belum ada GPS fix. Berdiri sebentar di halaman masjid atau ketuk Input Manual.",
                        color = Gold,
                        fontSize = 12.sp
                    )
                }

                if (theory != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Deklinasi Magnetik: ${fmtSigned2(theory.declinationDeg)}°  •  " +
                            "WMM |B|: ${fmt1(theory.wmmExpectedFieldUt)} µT\n" +
                            "Sensor |B| terukur: ${fmt1(sensor.magTotalUt)} µT  •  " +
                            "Anomali Δ|B|: ${fmtSigned(magAnomalyUt)} µT",
                        color = if (hasMagAnomaly) Coral else TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onToggleGpsLock,
                        enabled = location.hasFix,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (location.isLocked) AppIcons.LockOpen else AppIcons.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (location.isLocked) "Unlock GPS" else "Lock GPS", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showManualDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Input Manual", maxLines = 1)
                    }
                }
            }
        }

        // Live Formula & Sensor Pipeline Comparison Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Perbandingan Algoritma Live (True North)",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Rumus Kiblat Teoretis (dari koordinat saat ini):",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                CompareRow(
                    "WGS84 Ellipsoid (Vincenty)",
                    theory?.let { "${fmt4(it.wgs84BearingDeg)}°" } ?: "--",
                    Gold
                )
                CompareRow(
                    "MASA (Spherical 21.4225, 39.8262)",
                    theory?.let { "${fmt4(it.masaSphericalDeg)}°" } ?: "--",
                    TextPrimary
                )
                CompareRow(
                    "NU Online (Adhan 21.4225241, 39.8261818)",
                    theory?.let { "${fmt4(it.nuSphericalDeg)}°" } ?: "--",
                    TextPrimary
                )
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Pipeline Sensor Kompas (Dengan vs Tanpa Gyro):",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                CompareRow(
                    "QiblaKu 9-DoF RotVec + Declination",
                    if (rotVecTrue.isFinite()) "${fmt2(rotVecTrue)}°" else "--",
                    EmeraldBright
                )
                CompareRow(
                    "Tanpa Gyro (GeoMag RotVec + Decl)",
                    if (geoMagTrue.isFinite()) "${fmt2(geoMagTrue)}°" else "--",
                    Cyan
                )
                CompareRow(
                    "Emulasi Fallback MASA (LPF α=0.45)",
                    if (lpf45True.isFinite()) "${fmt2(lpf45True)}°" else "--",
                    TextMuted
                )
                CompareRow(
                    "Emulasi Fallback NU (LPF α=0.15)",
                    if (lpf15True.isFinite()) "${fmt2(lpf15True)}°" else "--",
                    TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (showManualDialog) {
        ManualLocationDialog(
            initialLat = location.latitude,
            initialLon = location.longitude,
            initialAlt = location.altitudeM,
            onDismiss = { showManualDialog = false },
            onSave = { lat, lon, alt ->
                onSetManualLocation(lat, lon, alt)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            color = valueColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun CompareRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ManualLocationDialog(
    initialLat: Double?,
    initialLon: Double?,
    initialAlt: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double) -> Unit
) {
    var latText by remember { mutableStateOf(initialLat?.toString() ?: "") }
    var lonText by remember { mutableStateOf(initialLon?.toString() ?: "") }
    var altText by remember { mutableStateOf(initialAlt.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Koordinat Manual (WGS84)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude (mis. -7.833xxx)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it },
                    label = { Text("Longitude (mis. 110.383xxx)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = altText,
                    onValueChange = { altText = it },
                    label = { Text("Altitude (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                errorMsg?.let { Text(it, color = Coral, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val lat = latText.trim().replace(',', '.').toDoubleOrNull()
                val lon = lonText.trim().replace(',', '.').toDoubleOrNull()
                val alt = altText.trim().replace(',', '.').toDoubleOrNull() ?: 113.0
                if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                    errorMsg = "Masukkan latitude (-90..90) dan longitude (-180..180) yang valid."
                } else {
                    onSave(lat, lon, alt)
                }
            }) {
                Text("Simpan & Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun fmt0(v: Double) = String.format(Locale.US, "%.0f", v)
private fun fmt1(v: Double) = String.format(Locale.US, "%.1f", v)
private fun fmt2(v: Double) = String.format(Locale.US, "%.2f", v)
private fun fmt4(v: Double) = String.format(Locale.US, "%.4f", v)
private fun fmt6(v: Double) = String.format(Locale.US, "%.6f", v)
private fun fmtSigned(v: Double) = String.format(Locale.US, "%+.1f", v)
private fun fmtSigned2(v: Double) = String.format(Locale.US, "%+.2f", v)
