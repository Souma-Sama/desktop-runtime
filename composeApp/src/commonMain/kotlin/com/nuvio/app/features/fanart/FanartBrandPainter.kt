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
            name = "FanartLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Rounded background in Fanart cyan/blue (#2DAAE1)
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
                fill = SolidColor(Color(0xFF2DAAE1)),
            )
            // Stylized 'f' logo mark in pure white
            addPath(
                pathData = PathBuilder().apply {
                    moveTo(14.5f, 6.5f)
                    curveTo(13.2f, 6.5f, 12f, 7.5f, 12f, 9f)
                    lineTo(12f, 10.5f)
                    lineTo(9.5f, 10.5f)
                    lineTo(9.5f, 13f)
                    lineTo(12f, 13f)
                    lineTo(12f, 18.5f)
                    lineTo(15f, 18.5f)
                    lineTo(15f, 13f)
                    lineTo(17.5f, 13f)
                    lineTo(17.5f, 10.5f)
                    lineTo(15f, 10.5f)
                    lineTo(15f, 9.2f)
                    curveTo(15f, 8.6f, 15.4f, 8.3f, 16f, 8.3f)
                    lineTo(17.5f, 8.3f)
                    lineTo(17.5f, 6.5f)
                    close()
                }.nodes,
                fill = SolidColor(Color.White),
            )
        }.build()
    }
    return rememberVectorPainter(vector)
}
