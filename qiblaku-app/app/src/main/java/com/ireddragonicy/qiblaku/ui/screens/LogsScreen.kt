package com.ireddragonicy.qiblaku.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ireddragonicy.qiblaku.data.SessionSummary
import com.ireddragonicy.qiblaku.ui.components.AppIcons
import com.ireddragonicy.qiblaku.ui.theme.BgDark
import com.ireddragonicy.qiblaku.ui.theme.CardBorder
import com.ireddragonicy.qiblaku.ui.theme.CardDark
import com.ireddragonicy.qiblaku.ui.theme.Coral
import com.ireddragonicy.qiblaku.ui.theme.Cyan
import com.ireddragonicy.qiblaku.ui.theme.Emerald
import com.ireddragonicy.qiblaku.ui.theme.EmeraldBright
import com.ireddragonicy.qiblaku.ui.theme.Gold
import com.ireddragonicy.qiblaku.ui.theme.TextMuted
import com.ireddragonicy.qiblaku.ui.theme.TextPrimary
import java.io.File
import java.util.Locale
import kotlin.math.abs

@Composable
fun LogsScreen(
    sessions: List<SessionSummary>,
    onShareFile: (File, String) -> Unit,
    onExportAllSummary: () -> File,
    onUpdateComparison: (String, Double?, Double?, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSession by remember { mutableStateOf<SessionSummary?>(null) }
    var deletingSession by remember { mutableStateOf<SessionSummary?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.ListAlt,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rekap Kampus 1–5 UAD (${sessions.size} sesi)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tersimpan otomatis ke Documents/QiblaKu/ (CSV siap adb pull)",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Button(
                    onClick = {
                        val file = onExportAllSummary()
                        onShareFile(file, "Share Summary CSV QiblaKu")
                    },
                    enabled = sessions.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT / SHARE SUMMARY CSV", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (sessions.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.ListAlt,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Belum ada sesi rekaman tersimpan",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Buka tab Ukur, pilih Kampus UAD, pastikan HP rata dan GPS fix, lalu tekan MULAI REKAM.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sessions, key = { it.id }) { s ->
                    SessionCard(
                        session = s,
                        onEdit = { editingSession = s },
                        onShareRaw = {
                            onShareFile(File(s.rawInternalPath), "Share ${s.rawCsvFileName}")
                        },
                        onDelete = { deletingSession = s }
                    )
                }
            }
        }
    }

    editingSession?.let { target ->
        EditComparisonDialog(
            session = target,
            onDismiss = { editingSession = null },
            onSave = { masaObs, nuObs, notes ->
                onUpdateComparison(target.id, masaObs, nuObs, notes)
                editingSession = null
            }
        )
    }

    deletingSession?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSession = null },
            title = { Text("Hapus Sesi Rekaman?") },
            text = {
                Text(
                    "Sesi ${target.campusLabel} (${target.poseId}) dan file ${target.rawCsvFileName} " +
                        "akan dihapus dari aplikasi serta Documents/QiblaKu/.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSession(target.id)
                        deletingSession = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSession = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: SessionSummary,
    onEdit: () -> Unit,
    onShareRaw: () -> Unit,
    onDelete: () -> Unit
) {
    val devColor = when {
        abs(session.deviationVsWgs84Deg) <= 1.0 -> EmeraldBright
        abs(session.deviationVsWgs84Deg) <= 3.0 -> Gold
        else -> Coral
    }
    val anomColor = when {
        abs(session.magAnomalyUt) > 15.0 -> Coral
        abs(session.magAnomalyUt) > 7.0 -> Gold
        else -> EmeraldBright
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row: Campus + pose badge on left, sample count on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = session.campusLabel,
                        color = EmeraldBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Cyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = session.poseId.replace('_', ' '),
                                color = Cyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = session.createdAtIso.replace('T', ' '),
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    color = BgDark,
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%d smp • %.1fs", session.sampleCount, session.durationSec),
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }

            // Primary 3-column angle metric strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgDark.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCell(
                        label = "QIBLAKU 9-DoF",
                        value = String.format(Locale.US, "%.2f°", session.rotVecTrueMeanDeg),
                        subValue = String.format(Locale.US, "Mag %.1f°", session.rotVecMagMeanDeg),
                        valueColor = Cyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCell(
                        label = "WGS84 TARGET",
                        value = String.format(Locale.US, "%.2f°", session.wgs84QiblaDeg),
                        subValue = String.format(Locale.US, "Bola %.2f°", session.masaFormulaDeg),
                        valueColor = Gold,
                        modifier = Modifier.weight(1f)
                    )
                    StatCell(
                        label = "DEVIASI",
                        value = String.format(Locale.US, "%+.2f°", session.deviationVsWgs84Deg),
                        subValue = String.format(Locale.US, "±%.2f° std", session.rotVecTrueStdDeg),
                        valueColor = devColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Clean 2x2 IMU telemetry grid (never wraps awkwardly)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailPair(
                        label = "Tanpa Gyro",
                        value = String.format(Locale.US, "%.1f° ±%.2f°", session.geoMagTrueMeanDeg, session.geoMagTrueStdDeg),
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    DetailPair(
                        label = "Gyro |ω|",
                        value = String.format(Locale.US, "%.3f °/s", session.gyroMeanDps),
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailPair(
                        label = "Medan |B|",
                        value = String.format(Locale.US, "%.1f (%+.1f) µT", session.magTotalMeanUt, session.magAnomalyUt),
                        valueColor = anomColor,
                        modifier = Modifier.weight(1f)
                    )
                    DetailPair(
                        label = "Pitch / Roll",
                        value = String.format(Locale.US, "%+.1f° / %+.1f°", session.pitchMeanDeg, session.rollMeanDeg),
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // GPS row with flat pin icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = AppIcons.LocationOn,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%.6f, %.6f  •  Akurasi ±%.0f m",
                        session.lat,
                        session.lon,
                        session.gpsAccM
                    ),
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Comparison block
            Surface(
                color = if (session.masaObservedDeg != null || session.nuObservedDeg != null || session.notes.isNotBlank()) {
                    Cyan.copy(alpha = 0.08f)
                } else {
                    BgDark.copy(alpha = 0.4f)
                },
                border = BorderStroke(
                    1.dp,
                    if (session.masaObservedDeg != null || session.nuObservedDeg != null) Cyan.copy(alpha = 0.25f) else CardBorder
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MASA: ${session.masaObservedDeg?.let { String.format(Locale.US, "%.1f°", it) } ?: "Belum diisi"}",
                            color = if (session.masaObservedDeg != null) Cyan else TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "NU Online: ${session.nuObservedDeg?.let { String.format(Locale.US, "%.1f°", it) } ?: "Belum diisi"}",
                            color = if (session.nuObservedDeg != null) Cyan else TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    if (session.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Catatan: ${session.notes}",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = CardBorder)

            // Clean non-wrapping action row with Flat Material Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MASA / NU", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onShareRaw,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Share,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share CSV", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.DeleteOutline,
                        contentDescription = "Hapus sesi",
                        tint = Coral,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    subValue: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1
        )
        Text(
            text = subValue,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun DetailPair(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.sp,
            maxLines = 1
        )
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun EditComparisonDialog(
    session: SessionSummary,
    onDismiss: () -> Unit,
    onSave: (Double?, Double?, String) -> Unit
) {
    var masaText by remember { mutableStateOf(session.masaObservedDeg?.toString() ?: "") }
    var nuText by remember { mutableStateOf(session.nuObservedDeg?.toString() ?: "") }
    var notesText by remember { mutableStateOf(session.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Pembanding (${session.campusId})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = masaText,
                    onValueChange = { masaText = it },
                    label = { Text("Hasil MASA (°) di titik ini") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = nuText,
                    onValueChange = { nuText = it },
                    label = { Text("Hasil NU Online (°) di titik ini") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Catatan lapangan (mis. dekat pilar/kipas)") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    masaText.trim().replace(',', '.').toDoubleOrNull(),
                    nuText.trim().replace(',', '.').toDoubleOrNull(),
                    notesText.trim()
                )
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
