package com.ireddragonicy.qiblaku.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Crisp 24dp Flat Material vector icons using official SVG path data
 * so the app has zero bloated icon-pack dependencies or Unicode emoji fallbacks.
 */
object AppIcons {
    val Explore: ImageVector by lazy {
        materialIcon(
            "Explore",
            "M12,10.9c-0.61,0 -1.1,0.49 -1.1,1.1s0.49,1.1 1.1,1.1c0.61,0 1.1,-0.49 1.1,-1.1S12.61,10.9 12,10.9z" +
                "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2z" +
                "M14.19,14.19L6,18l3.81,-8.19L18,6L14.19,14.19z"
        )
    }

    val ShowChart: ImageVector by lazy {
        materialIcon(
            "ShowChart",
            "M3.5,18.49l6,-6.01l4,4L22,6.92l-1.41,-1.41l-7.09,7.97l-4,-4L2,16.99z"
        )
    }

    val ListAlt: ImageVector by lazy {
        materialIcon(
            "ListAlt",
            "M19,5v14H5V5h14m1.1,-2H3.9C3.4,3 3,3.4 3,3.9v16.2c0,0.4 0.4,0.9 0.9,0.9h16.2c0.4,0 0.9,-0.5 0.9,-0.9V3.9C21,3.4 20.5,3 20.1,3z" +
                "M11,7h6v2h-6V7zM11,11h6v2h-6v-2zM11,15h6v2h-6v-2z" +
                "M7,7h2v2H7V7zM7,11h2v2H7v-2zM7,15h2v2H7v-2z"
        )
    }

    val Download: ImageVector by lazy {
        materialIcon(
            "Download",
            "M5,20h14v-2H5V20zM19,9h-4V3H9v6H5l7,7L19,9z"
        )
    }

    val Share: ImageVector by lazy {
        materialIcon(
            "Share",
            "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11" +
                "c0.54,0.5 1.25,0.81 2.04,0.81c1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3s-3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81" +
                "C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16" +
                "c-0.05,0.21 -0.08,0.43 -0.08,0.65c0,1.61 1.31,2.92 2.92,2.92s2.92,-1.31 2.92,-2.92C20.92,17.39 19.61,16.08 18,16.08z"
        )
    }

    val Edit: ImageVector by lazy {
        materialIcon(
            "Edit",
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25z" +
                "M20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83l3.75,3.75L20.71,7.04z"
        )
    }

    val DeleteOutline: ImageVector by lazy {
        materialIcon(
            "DeleteOutline",
            "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6V19zM8,9h8v10H8V9zM15.5,4l-1,-1h-5l-1,1H5v2h14V4H15.5z"
        )
    }

    val LocationOn: ImageVector by lazy {
        materialIcon(
            "LocationOn",
            "M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13C19,5.13 15.87,2 12,2z" +
                "M12,11.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5s2.5,1.12 2.5,2.5S13.38,11.5 12,11.5z"
        )
    }

    val Lock: ImageVector by lazy {
        materialIcon(
            "Lock",
            "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10C20,8.9 19.1,8 18,8z" +
                "M12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2s2,0.9 2,2S13.1,17 12,17zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1s3.1,1.39 3.1,3.1V8z"
        )
    }

    val LockOpen: ImageVector by lazy {
        materialIcon(
            "LockOpen",
            "M12,17c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2s-2,0.9 -2,2S10.9,17 12,17z" +
                "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5c-2.28,0 -4.27,1.54 -4.84,3.75c-0.14,0.54 0.18,1.08 0.72,1.22" +
                "c0.53,0.14 1.08,-0.18 1.22,-0.72C9.44,3.93 10.63,3 12,3c1.65,0 3,1.35 3,3v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12" +
                "c1.1,0 2,-0.9 2,-2V10C20,8.9 19.1,8 18,8zM18,20H6V10h12V20z"
        )
    }

    val RecordDot: ImageVector by lazy {
        materialIcon(
            "RecordDot",
            "M12,5c-3.87,0 -7,3.13 -7,7s3.13,7 7,7s7,-3.13 7,-7S15.87,5 12,5z"
        )
    }

    val Stop: ImageVector by lazy {
        materialIcon(
            "Stop",
            "M6,6h12v12H6z"
        )
    }

    val Sensors: ImageVector by lazy {
        materialIcon(
            "Sensors",
            "M7.76,16.24C6.67,15.16 6,13.66 6,12s0.67,-3.16 1.76,-4.24l1.42,1.42C8.45,9.9 8,10.9 8,12c0,1.1 0.45,2.1 1.17,2.83L7.76,16.24z" +
                "M16.24,16.24l-1.42,-1.42C15.55,14.1 16,13.1 16,12c0,-1.1 -0.45,-2.1 -1.17,-2.83l1.42,-1.42C17.33,8.84 18,10.34 18,12S17.33,15.16 16.24,16.24z" +
                "M12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2s2,-0.9 2,-2S13.1,10 12,10z" +
                "M4.93,19.07C3.12,17.26 2,14.76 2,12s1.12,-5.26 2.93,-7.07l1.41,1.41C4.9,7.79 4,9.79 4,12c0,2.21 0.9,4.21 2.34,5.66L4.93,19.07z" +
                "M19.07,4.93C20.88,6.74 22,9.24 22,12s-1.12,5.26 -2.93,7.07l-1.41,-1.41C19.1,16.21 20,14.21 20,12c0,-2.21 -0.9,-4.21 -2.34,-5.66L19.07,4.93z"
        )
    }

    val Tune: ImageVector by lazy {
        materialIcon(
            "Tune",
            "M3,17v2h6v-2H3zM3,5v2h10V5H3zM13,21v-2h8v-2h-8v-2h-2v6H13zM7,9v2H3v2h4v2h2V9H7zM21,13v-2H11v2H21zM15,9h2V7h4V5h-4V3h-2V9z"
        )
    }

    val CheckCircle: ImageVector by lazy {
        materialIcon(
            "CheckCircle",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2z" +
                "M12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8s8,3.59 8,8S16.41,20 12,20z" +
                "M16.59,7.58L10,14.17l-2.59,-2.58L6,13l4,4l8,-8L16.59,7.58z"
        )
    }

    val Warning: ImageVector by lazy {
        materialIcon(
            "Warning",
            "M12,5.99L19.53,19H4.47L12,5.99M12,2L1,21h22L12,2L12,2zM13,16h-2v2h2V16zM13,10h-2v4h2V10z"
        )
    }

    private fun materialIcon(name: String, pathData: String): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }.build()
    }
}
