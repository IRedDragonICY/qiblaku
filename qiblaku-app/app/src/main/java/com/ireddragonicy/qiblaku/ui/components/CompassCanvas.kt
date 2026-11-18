package com.ireddragonicy.qiblaku.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ireddragonicy.qiblaku.ui.theme.CardBorder
import com.ireddragonicy.qiblaku.ui.theme.Coral
import com.ireddragonicy.qiblaku.ui.theme.Cyan
import com.ireddragonicy.qiblaku.ui.theme.Emerald
import com.ireddragonicy.qiblaku.ui.theme.EmeraldBright
import com.ireddragonicy.qiblaku.ui.theme.Gold
import com.ireddragonicy.qiblaku.ui.theme.TextMuted
import com.ireddragonicy.qiblaku.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassDialCanvas(
    headingTrueDeg: Double,
    qiblaTargetDeg: Double?,
    pitchDeg: Double,
    rollDeg: Double,
    isFlat: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 250.dp
) {
    val validHeading = if (headingTrueDeg.isFinite()) headingTrueDeg.toFloat() else 0f

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = this.size.minDimension / 2f - 18f

        // Outer ring
        drawCircle(
            color = CardBorder,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color(0xFF0E181C),
            radius = radius - 2f,
            center = Offset(cx, cy)
        )

        // Fixed top-edge phone alignment marker (+Y axis)
        val markerPath = Path().apply {
            moveTo(cx, cy - radius - 14f)
            lineTo(cx - 10f, cy - radius + 2f)
            lineTo(cx + 10f, cy - radius + 2f)
            close()
        }
        drawPath(markerPath, color = Cyan)
        drawLine(
            color = Cyan.copy(alpha = 0.35f),
            start = Offset(cx, cy - radius + 4f),
            end = Offset(cx, cy - 34f),
            strokeWidth = 2.5f
        )

        // Rotate the compass rose opposite to phone heading so North aligns with real North
        rotate(degrees = -validHeading, pivot = Offset(cx, cy)) {
            for (deg in 0 until 360 step 5) {
                val rad = Math.toRadians((deg - 90).toDouble())
                val isMajor = deg % 30 == 0
                val isMed = deg % 15 == 0
                val tickLen = when {
                    isMajor -> 16f
                    isMed -> 10f
                    else -> 5f
                }
                val startR = radius - tickLen
                val color = when {
                    deg == 0 -> Coral
                    isMajor -> TextPrimary
                    else -> TextMuted.copy(alpha = 0.55f)
                }
                drawLine(
                    color = color,
                    start = Offset(
                        cx + (startR * cos(rad)).toFloat(),
                        cy + (startR * sin(rad)).toFloat()
                    ),
                    end = Offset(
                        cx + (radius * cos(rad)).toFloat(),
                        cy + (radius * sin(rad)).toFloat()
                    ),
                    strokeWidth = if (isMajor) 3f else 1.5f,
                    cap = StrokeCap.Round
                )
            }

            // Cardinal labels
            val paint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val labelR = radius - 34f
            val cardinals = listOf(
                0 to ("U" to android.graphics.Color.parseColor("#EF4444")),
                90 to ("T" to android.graphics.Color.parseColor("#E2E8F0")),
                180 to ("S" to android.graphics.Color.parseColor("#E2E8F0")),
                270 to ("B" to android.graphics.Color.parseColor("#E2E8F0"))
            )
            for ((deg, pair) in cardinals) {
                val rad = Math.toRadians((deg - 90).toDouble())
                val lx = cx + (labelR * cos(rad)).toFloat()
                val ly = cy + (labelR * sin(rad)).toFloat() + 10f
                paint.color = pair.second
                drawContext.canvas.nativeCanvas.drawText(pair.first, lx, ly, paint)
            }

            // Qibla target arrow on the rotating rose
            if (qiblaTargetDeg != null && qiblaTargetDeg.isFinite()) {
                val qRad = Math.toRadians(qiblaTargetDeg - 90.0)
                val qx = cx + ((radius - 12f) * cos(qRad)).toFloat()
                val qy = cy + ((radius - 12f) * sin(qRad)).toFloat()

                drawLine(
                    color = Gold,
                    start = Offset(cx, cy),
                    end = Offset(qx, qy),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Gold,
                    radius = 10f,
                    center = Offset(qx, qy)
                )
            }
        }

        // Center 2D Spirit Level (Bubble Level)
        val levelR = 36f
        val levelColor = if (isFlat) EmeraldBright else Gold
        drawCircle(
            color = Color(0xFF0A1114),
            radius = levelR,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = levelColor,
            radius = levelR,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = levelColor.copy(alpha = 0.45f),
            radius = 12f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
        // Crosshairs
        drawLine(
            color = levelColor.copy(alpha = 0.35f),
            start = Offset(cx - levelR, cy),
            end = Offset(cx + levelR, cy),
            strokeWidth = 1.2f
        )
        drawLine(
            color = levelColor.copy(alpha = 0.35f),
            start = Offset(cx, cy - levelR),
            end = Offset(cx, cy + levelR),
            strokeWidth = 1.2f
        )

        // Bubble position clamped inside level circle (max 15 deg full deflection)
        val bx = ((rollDeg / 15.0).coerceIn(-1.0, 1.0) * (levelR - 9f)).toFloat()
        val by = ((-pitchDeg / 15.0).coerceIn(-1.0, 1.0) * (levelR - 9f)).toFloat()
        drawCircle(
            color = if (isFlat) Emerald else Coral,
            radius = 8f,
            center = Offset(cx + bx, cy + by)
        )
    }
}

@Composable
fun SparklineChart(
    values: List<Float>,
    lineColor: Color,
    secondaryValues: List<Float>? = null,
    secondaryColor: Color = Gold,
    fixedMin: Float? = null,
    fixedMax: Float? = null,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        drawRect(color = Color(0xFF0B1317))
        val all = if (secondaryValues != null) values + secondaryValues else values
        if (all.size < 2) return@Canvas

        var minV = fixedMin ?: all.minOrNull() ?: 0f
        var maxV = fixedMax ?: all.maxOrNull() ?: 1f
        if (maxV - minV < 1e-4f) {
            minV -= 0.5f
            maxV += 0.5f
        }
        val span = maxV - minV
        val w = size.width
        val h = size.height
        val pad = 6f

        fun buildPath(data: List<Float>): Path {
            val p = Path()
            val n = data.size
            data.forEachIndexed { idx, v ->
                val x = (idx.toFloat() / (n - 1).coerceAtLeast(1)) * w
                val norm = ((v - minV) / span).coerceIn(0f, 1f)
                val y = (h - pad) - norm * (h - 2f * pad)
                if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            return p
        }

        if (secondaryValues != null && secondaryValues.size >= 2) {
            drawPath(
                path = buildPath(secondaryValues),
                color = secondaryColor.copy(alpha = 0.75f),
                style = Stroke(width = 2.5f)
            )
        }
        if (values.size >= 2) {
            drawPath(
                path = buildPath(values),
                color = lineColor,
                style = Stroke(width = 3f)
            )
        }
    }
}
