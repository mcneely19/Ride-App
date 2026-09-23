package com.onewheel.ridetracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onewheel.ridetracker.data.Ride
import com.onewheel.ridetracker.ui.theme.LocalRideColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * A small scatter chart: x = [xOf] (avg speed or ambient temp), y = Wh/mi.
 * Axis is intentionally inverted so lower Wh/mi (more efficient) sits near
 * the top and higher Wh/mi sits near the bottom, matching the web tracker.
 */
@Composable
fun EfficiencyScatterChart(
    rides: List<Ride>,
    xOf: (Ride) -> Double,
    xDomain: ClosedFloatingPointRange<Double>,
    xTicks: List<Double>,
    xUnitLabel: String,
    showTrendlines: Boolean,
    colorOf: (String) -> Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalRideColors.current
    val accentColor = colorOf

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val padL = 34.dp.toPx()
        val padR = 10.dp.toPx()
        val padT = 20.dp.toPx() // extra headroom for the "Wh/mi" axis label
        val padB = 26.dp.toPx()
        val plotW = size.width - padL - padR
        val plotH = size.height - padT - padB

        // Y-axis unit — drawn once, top-left, rather than repeated on every gridline.
        drawContext.canvas.nativeCanvas.drawText(
            "Wh/mi",
            padL,
            12.dp.toPx(),
            android.graphics.Paint().apply {
                color = colors.textFaint.toArgbInt()
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }
        )

        if (rides.isEmpty()) return@Canvas

        val yVals = rides.map { it.whPerMi }
        val yMin = floor((yVals.min() / 5.0)) * 5 - 2
        val yMax = ceil((yVals.max() / 5.0)) * 5 + 2
        val xMin = xDomain.start
        val xMax = xDomain.endInclusive

        fun px(x: Double) = (padL + (x - xMin) / (xMax - xMin) * plotW).toFloat()
        fun py(y: Double) = (padT + (y - yMin) / (yMax - yMin) * plotH).toFloat()

        val yStep = max(5.0, ceil((yMax - yMin) / 5 / 5) * 5)
        var yTick = ceil(yMin / yStep) * yStep
        while (yTick <= yMax) {
            drawLine(colors.border, Offset(padL, py(yTick)), Offset(size.width - padR, py(yTick)), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                yTick.toInt().toString(),
                padL - 8.dp.toPx(),
                py(yTick) + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = colors.textFaint.toArgbInt()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
            yTick += yStep
        }

        xTicks.forEach { t ->
            drawLine(
                colors.border.copy(alpha = 0.5f),
                Offset(px(t), padT), Offset(px(t), size.height - padB), strokeWidth = 1f
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${t.toInt()}$xUnitLabel",
                px(t),
                size.height - padB + 16.dp.toPx(),
                android.graphics.Paint().apply {
                    color = colors.textFaint.toArgbInt()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }

        drawLine(colors.border, Offset(padL, size.height - padB), Offset(size.width - padR, size.height - padB), strokeWidth = 1.3f)

        if (showTrendlines) {
            rides.groupBy { it.board }.forEach { (board, boardRides) ->
                if (boardRides.size < 2) return@forEach
                val pts = boardRides.map { xOf(it) to it.whPerMi }
                val n = pts.size
                val sx = pts.sumOf { it.first }
                val sy = pts.sumOf { it.second }
                val sxx = pts.sumOf { it.first * it.first }
                val sxy = pts.sumOf { it.first * it.second }
                val denom = n * sxx - sx * sx
                if (kotlin.math.abs(denom) < 1e-9) return@forEach
                val m = (n * sxy - sx * sy) / denom
                val b = (sy - m * sx) / n
                val x0 = pts.minOf { it.first }
                val x1 = pts.maxOf { it.first }
                drawLine(
                    color = accentColor(board).copy(alpha = 0.55f),
                    start = Offset(px(x0), py(m * x0 + b)),
                    end = Offset(px(x1), py(m * x1 + b)),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
            }
        }

        rides.forEach { r ->
            drawCircle(
                color = accentColor(r.board).copy(alpha = 0.85f),
                radius = 5.dp.toPx(),
                center = Offset(px(xOf(r)), py(r.whPerMi))
            )
        }
    }
}

private fun Color.toArgbInt(): Int = this.toArgb()
