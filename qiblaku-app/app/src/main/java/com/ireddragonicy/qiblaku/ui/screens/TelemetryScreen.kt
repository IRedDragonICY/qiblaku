package com.ireddragonicy.qiblaku.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ireddragonicy.qiblaku.math.QiblaCalculator
import com.ireddragonicy.qiblaku.math.QiblaTheoretical
import com.ireddragonicy.qiblaku.sensor.SensorRepository
import com.ireddragonicy.qiblaku.sensor.SensorSnapshot
import com.ireddragonicy.qiblaku.ui.components.AppIcons
import com.ireddragonicy.qiblaku.ui.components.SparklineChart
import com.ireddragonicy.qiblaku.ui.theme.CardBorder
import com.ireddragonicy.qiblaku.ui.theme.CardDark
import com.ireddragonicy.qiblaku.ui.theme.Coral
import com.ireddragonicy.qiblaku.ui.theme.Cyan
import com.ireddragonicy.qiblaku.ui.theme.EmeraldBright
import com.ireddragonicy.qiblaku.ui.theme.Gold
import com.ireddragonicy.qiblaku.ui.theme.TextMuted
import com.ireddragonicy.qiblaku.ui.theme.TextPrimary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun TelemetryScreen(
    sensor: SensorSnapshot,
    theory: QiblaTheoretical?,
    modifier: Modifier = Modifier
) {
    val decl = theory?.declinationDeg ?: 0.0
    val rotVecTrue = if (sensor.rotVecMagDeg.isFinite()) {
        QiblaCalculator.wrap360(sensor.rotVecMagDeg + decl)
    } else Double.NaN
    val geoMagTrue = if (sensor.geoMagOnlyMagDeg.isFinite()) {
        QiblaCalculator.wrap360(sensor.geoMagOnlyMagDeg + decl)
    } else Double.NaN
    val magAnomaly = if (theory != null) sensor.magTotalUt - theory.wmmExpectedFieldUt else 0.0
    val accNorm = sqrt(
        (sensor.accX * sensor.accX + sensor.accY * sensor.accY + sensor.accZ * sensor.accZ).toDouble()
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Gyroscope Card
        TelemetryCard(
            title = "Gyroscope (Kecepatan Sudut)",
            icon = AppIcons.Sensors,
            badge = if (sensor.hasGyroscope) SensorRepository.accuracyLabel(sensor.gyroAccuracy) else "TIDAK ADA",
            badgeColor = if (sensor.hasGyroscope) EmeraldBright else Coral
        ) {
            Text(
                text = String.format(
                    Locale.US,
                    "gx:%+6.3f  gy:%+6.3f  gz:%+6.3f rad/s\n" +
                        "Norma |ω|: %.3f °/s  •  Bias uncal: %s",
                    sensor.gyroX,
                    sensor.gyroY,
                    sensor.gyroZ,
                    sensor.gyroNormDps,
                    if (sensor.gyroUncalBiasNormDps.isFinite()) {
                        String.format(Locale.US, "%.4f °/s", sensor.gyroUncalBiasNormDps)
                    } else "n/a"
                ),
                color = Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Grafik |ω| (°/s) — pastikan mendekati 0 saat HP diam di shaf:", color = TextMuted, fontSize = 10.sp)
            SparklineChart(
                values = sensor.gyroTrace,
                lineColor = Cyan
            )
        }

        // Magnetometer Card
        val anomalyColor = when {
            theory == null -> TextMuted
            abs(magAnomaly) > 15.0 -> Coral
            abs(magAnomaly) > 7.0 -> Gold
            else -> EmeraldBright
        }
        TelemetryCard(
            title = "Magnetometer (Medan Magnet & Anomali)",
            icon = AppIcons.Explore,
            badge = SensorRepository.accuracyLabel(sensor.magAccuracy),
            badgeColor = if (sensor.magAccuracy >= 2) EmeraldBright else Coral
        ) {
            Text(
                text = String.format(
                    Locale.US,
                    "mx:%+6.2f  my:%+6.2f  mz:%+6.2f µT\n" +
                        "Total |B|: %.2f µT  •  WMM: %s\n" +
                        "Anomali Δ|B|: %s  •  Hard-Iron: %s",
                    sensor.magX,
                    sensor.magY,
                    sensor.magZ,
                    sensor.magTotalUt,
                    theory?.let { String.format(Locale.US, "%.2f µT", it.wmmExpectedFieldUt) } ?: "--",
                    theory?.let { String.format(Locale.US, "%+.2f µT", magAnomaly) } ?: "--",
                    if (sensor.magHardIronNormUt.isFinite()) {
                        String.format(Locale.US, "%.1f µT", sensor.magHardIronNormUt)
                    } else "n/a"
                ),
                color = anomalyColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Grafik Total |B| (µT) — lonjakan menandakan besi tulangan/speaker dekat HP:", color = TextMuted, fontSize = 10.sp)
            SparklineChart(
                values = sensor.magTotalTrace,
                lineColor = Gold
            )
        }

        // Sensor Fusion Comparison Card (With Gyro vs Without Gyro)
        TelemetryCard(
            title = "Fusi Sensor: Dengan Gyro vs Tanpa Gyro",
            icon = AppIcons.ShowChart,
            badge = "9-DoF vs 6-DoF",
            badgeColor = EmeraldBright
        ) {
            Text(
                text = String.format(
                    Locale.US,
                    "RotVec (Accel+Mag+Gyro) : True=%s  Mag=%s  EstErr=%s\n" +
                        "GeoMag (Tanpa Gyro)     : True=%s  Mag=%s\n" +
                        "GameRotVec (Gyro+Accel) : RelYaw=%s  (bebas gangguan magnet)\n" +
                        "MASA fallback LPF α=0.45: Mag=%s\n" +
                        "NU   fallback LPF α=0.15: Mag=%s",
                    fmtDeg(rotVecTrue),
                    fmtDeg(sensor.rotVecMagDeg),
                    if (sensor.rotVecEstErrDeg.isFinite()) String.format(Locale.US, "±%.2f°", sensor.rotVecEstErrDeg) else "n/a",
                    fmtDeg(geoMagTrue),
                    fmtDeg(sensor.geoMagOnlyMagDeg),
                    fmtDeg(sensor.gameYawDeg),
                    fmtDeg(sensor.lpf45MagDeg),
                    fmtDeg(sensor.lpf15MagDeg)
                ),
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hijau = Rotation Vector (dengan Gyro)  |  Kuning = GeoMag (tanpa Gyro)",
                color = TextMuted,
                fontSize = 10.sp
            )
            SparklineChart(
                values = sensor.rotVecTrace,
                lineColor = EmeraldBright,
                secondaryValues = sensor.geoMagTrace,
                secondaryColor = Gold
            )
        }

        // Accelerometer & Orientation Card
        TelemetryCard(
            title = "Accelerometer & Kemiringan HP",
            icon = AppIcons.Tune,
            badge = if (abs(sensor.pitchDeg) <= 2.5 && abs(sensor.rollDeg) <= 2.5) "RATA" else "MIRING",
            badgeColor = if (abs(sensor.pitchDeg) <= 2.5 && abs(sensor.rollDeg) <= 2.5) EmeraldBright else Gold
        ) {
            Text(
                text = String.format(
                    Locale.US,
                    "ax: %+6.3f m/s²   ay: %+6.3f m/s²   az: %+6.3f m/s²   (|a|=%.3f)\n" +
                        "Pitch: %+6.2f°   •   Roll: %+6.2f°",
                    sensor.accX,
                    sensor.accY,
                    sensor.accZ,
                    accNorm,
                    sensor.pitchDeg,
                    sensor.rollDeg
                ),
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    icon: ImageVector,
    badge: String,
    badgeColor: Color,
    content: @Composable () -> Unit
) {
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
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = badge,
                    color = badgeColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

private fun fmtDeg(v: Double): String =
    if (v.isFinite()) String.format(Locale.US, "%6.2f°", v) else "   --  "
