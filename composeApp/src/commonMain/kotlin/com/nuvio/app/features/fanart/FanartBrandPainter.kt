package com.nuvio.app.features.fanart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
fun rememberFanartBrandPainter(): Painter {
    val vector = remember {
        ImageVector.Builder(
            name = "FanartTvLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Dark rounded background tile (#161A22)
            addPath(
                pathData = PathBuilder().apply {
                    moveTo(5f, 2f)
                    lineTo(19f, 2f)
                    arcTo(3f, 3f, 0f, false, true, 22f, 5f)
                    lineTo(22f, 19f)
                    arcTo(3f, 3f, 0f, false, true, 19f, 22f)
                    lineTo(5f, 22f)
                    arcTo(3f, 3f, 0f, false, true, 2f, 19f)
                    lineTo(2f, 5f)
                    arcTo(3f, 3f, 0f, false, true, 5f, 2f)
                    close()
                }.nodes,
                fill = SolidColor(Color(0xFF161A22)),
            )
            // Fan blade 1: Top-Left Cyan (#00B4D8)
            addPath(
                pathData = PathBuilder().apply {
                    moveTo(12f, 12f)
                    lineTo(6f, 6.5f)
                    arcTo(7.5f, 7.5f, 0f, false, true, 12f, 4.5f)
                    close()
                }.nodes,
                fill = SolidColor(Color(0xFF00B4D8)),
            )
            // Fan blade 2: Top-Right Coral Pink (#FF4D6D)
            addPath(
                pathData = PathBuilder().apply {
                    moveTo(12f, 12f)
                    lineTo(17.5f, 6.5f)
                    arcTo(7.5f, 7.5f, 0f, false, true, 19.5f, 12f)
                    close()
                }.nodes,
                fill = SolidColor(Color(0xFFFF4D6D)),
            )
            // Fan blade 3: Bottom-Right Amber Orange (#FFB703)
            addPath(
                pathData = PathBuilder().apply {
                    moveTo(12f, 12f)
                    lineTo(17.5f, 17.5f)
                    arcTo(7.5f, 7.5f, 0f, false, true, 12f, 19.5f)
                    close()
                }.nodes,
                fill = SolidColor(Color(0xFFFFB703)),
            )
            // Fan blade 4: Bottom-Left Emerald Green (#06D6A0)
            addPath(
                pathData = PathBuilder().apply {
                    moveTo(12f, 12f)
                    lineTo(6.5f, 17.5f)
                    arcTo(7.5f, 7.5f, 0f, false, true, 4.5f, 12f)
                    close()
                }.nodes,
                fill = SolidColor(Color(0xFF06D6A0)),
            )
            // Center Aperture Ring in Dark Tile
            addPath(
                pathData = PathBuilder().apply {
                    moveTo(12f, 9.5f)
                    arcTo(2.5f, 2.5f, 0f, true, true, 12f, 14.5f)
                    arcTo(2.5f, 2.5f, 0f, true, true, 12f, 9.5f)
                    close()
                }.nodes,
                fill = SolidColor(Color(0xFF161A22)),
            )
            // Center Core in Crisp White
            addPath(
                pathData = PathBuilder().apply {
                    moveTo(12f, 10.75f)
                    arcTo(1.25f, 1.25f, 0f, true, true, 12f, 13.25f)
                    arcTo(1.25f, 1.25f, 0f, true, true, 12f, 10.75f)
                    close()
                }.nodes,
                fill = SolidColor(Color.White),
            )
        }.build()
    }
    return rememberVectorPainter(vector)
}
