package com.nuvio.app.features.fanart

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

val FanartLogoVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "FanartTvIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 479.14f,
        viewportHeight = 479.14f,
    ).apply {
        group(
            name = "fanart_group",
            translationX = -8.9f,
            translationY = -13.82f,
        ) {
            // Circle Background (#22b6e0)
            addPath(
                pathData = PathParser().parsePathString(
                    "M248.47,13.82C380.78,13.82,488,121.07,488,253.39S380.78,493,248.47,493,8.9,385.7,8.9,253.39,116.16,13.82,248.47,13.82Z"
                ).toNodes(),
                fill = SolidColor(Color(0xFF22B6E0)),
                pathFillType = PathFillType.EvenOdd,
            )
            // Main Dark Geometric Aperture / Fold (#21252e)
            addPath(
                pathData = PathParser().parsePathString(
                    "M257.92,31.55l-5.37-17.63c-1.36,0-2.71-.1-4.08-.1a242.43,242.43,0,0,0-26.68,1.51L265,149.84,144.74,246.92,22.75,173A239,239,0,0,0,14,204.23l16,9.41a215.92,215.92,0,0,0-3.79,39.75c0,122.78,99.53,222.31,222.31,222.31s222.31-99.53,222.31-222.31C470.78,133.8,376.28,36.54,257.92,31.55Zm6,19.62c102.63,7.75,184.05,91.31,187.67,195L297.51,161.56ZM47.48,224l74.26,43.73L56.45,320.06a202.94,202.94,0,0,1-11.24-66.67A198.2,198.2,0,0,1,47.48,224Zm201,232.69c-86.13,0-159.7-53.6-189.3-129.25l69.41-55.68L332,391.5l28-24.59L300.75,172.18l150.94,82.94C450.76,366.58,360.15,456.65,248.47,456.65Z"
                ).toNodes(),
                fill = SolidColor(Color(0xFF21252E)),
                pathFillType = PathFillType.EvenOdd,
            )
            // Accent Fold 1 (#228aaa)
            addPath(
                pathData = PathParser().parsePathString(
                    "M168.2,261.12l164.51,99.64L272.3,177.29Z"
                ).toNodes(),
                fill = SolidColor(Color(0xFF228AAA)),
                pathFillType = PathFillType.EvenOdd,
            )
            // Accent Fold 2 (#228aaa)
            addPath(
                pathData = PathParser().parsePathString(
                    "M47.48,224a198.2,198.2,0,0,0-2.27,29.43,202.94,202.94,0,0,0,11.24,66.67l65.29-52.37Z"
                ).toNodes(),
                fill = SolidColor(Color(0xFF228AAA)),
            )
            // Accent Fold 3 (#228aaa)
            addPath(
                pathData = PathParser().parsePathString(
                    "M451.57,246.2c-3.62-103.72-85-187.28-187.67-195l33.61,110.39Z"
                ).toNodes(),
                fill = SolidColor(Color(0xFF228AAA)),
            )
        }
    }.build()
}

@Composable
fun rememberFanartBrandPainter(): Painter = rememberVectorPainter(FanartLogoVector)
