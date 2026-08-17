package com.manageyourmoney.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manageyourmoney.app.domain.format.CurrencyFormatter
import com.manageyourmoney.app.domain.model.DailyBalancePoint
import kotlin.math.abs
import kotlin.math.max

/**
 * Direct Canvas port of `lineChart()` / `dailyBalanceChart()` (index.html:1233-1244,
 * 2198-2244) plus their shared `yAxisGrid()` helper (index.html:522-558): a filled
 * running-balance line with a dashed horizontal grid, short-scale (K/L/Cr) y-axis
 * labels, and a dot on every point.
 *
 * The two web functions differed only in axis padding strategy — [dailyBalanceChart]
 * pads the value range by 18% so small month-to-month moves are visible, while
 * [lineChart] (the per-month detail chart) does not. Toggle with [padValueRange].
 *
 * @param tickLabels optional explicit x-axis labels at specific point indices — pass
 *        the result of choosing "every 7th day" (1-month view) or "one per calendar
 *        month" (3/6-month view), mirroring `dailyBalanceChart()`'s `tickIdxs` logic.
 *        Pass an empty map for [lineChart]'s behavior (no x-axis labels at all).
 */
@Composable
fun RunningBalanceLineChart(
    points: List<DailyBalancePoint>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    padValueRange: Boolean = true,
    tickLabels: Map<Int, String> = emptyMap(),
) {
    if (points.isEmpty()) {
        EmptyChartMessage(emptyMessage, modifier)
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(900f / 220f),
        ) {
            val padL = 78.dp.toPx()
            val padR = 12.dp.toPx()
            val padT = 12.dp.toPx()
            val padB = if (tickLabels.isNotEmpty()) 26.dp.toPx() else 12.dp.toPx()
            val w = size.width
            val h = size.height

            val values = points.map { it.balance }
            val rawMin = values.min()
            val rawMax = values.max()
            val (minV, maxV) = if (padValueRange) {
                val span = (rawMax - rawMin).let { if (it != 0.0) it else max(abs(rawMax) * 0.1, 1000.0) }
                val pad = span * 0.18
                (rawMin - pad) to (rawMax + pad)
            } else {
                rawMin to rawMax
            }
            val range = (maxV - minV).let { if (it != 0.0) it else 1.0 }

            val stepX = if (points.size > 1) (w - padL - padR) / (points.size - 1) else 0f
            fun xAt(i: Int) = if (points.size > 1) padL + i * stepX else (padL + w - padR) / 2f
            fun yAt(balance: Double) = (h - padB - ((balance - minV) / range) * (h - padT - padB)).toFloat()

            // ---- y-axis grid + short-scale labels (port of yAxisGrid()) ----
            val tickCount = 8
            val maxAbs = max(abs(maxV), abs(minV))
            val (div, suffix) = when {
                maxAbs >= 1e7 -> 1e7 to "Cr"
                maxAbs >= 1e5 -> 1e5 to "L"
                maxAbs >= 1e3 -> 1e3 to "K"
                else -> 1.0 to ""
            }
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            for (i in 0..tickCount) {
                val v = minV + (range * i / tickCount)
                val y = yAt(v)
                drawLine(
                    color = gridColor,
                    start = Offset(padL, y),
                    end = Offset(w - padR, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect,
                )
                val scaled = v / div
                val rounded = kotlin.math.round(scaled * 100) / 100
                val sign = if (v < 0 && abs(rounded) > 0.0001) "-" else ""
                val label = "$sign\u20B9${formatTrim(abs(rounded))}$suffix"
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    padL - 8.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.RIGHT
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                    },
                )
            }

            // ---- area fill + line + dots (port of the SVG path/area/dots) ----
            val path = androidx.compose.ui.graphics.Path()
            val coords = points.indices.map { i -> Offset(xAt(i), yAt(points[i].balance)) }
            coords.forEachIndexed { i, c ->
                if (i == 0) path.moveTo(c.x, c.y) else path.lineTo(c.x, c.y)
            }
            val areaPath = androidx.compose.ui.graphics.Path().apply {
                addPath(path)
                lineTo(coords.last().x, h - padB)
                lineTo(coords.first().x, h - padB)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0f)),
                    startY = padT,
                    endY = h - padB,
                ),
            )
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            val dotIndices = if (tickLabels.isNotEmpty()) tickLabels.keys else coords.indices.toSet()
            coords.forEachIndexed { i, c ->
                if (i in dotIndices) {
                    drawCircle(color = lineColor, radius = (if (tickLabels.isEmpty()) 3.dp else 2.5.dp).toPx(), center = c)
                    drawCircle(color = surfaceColor, radius = 1.2.dp.toPx(), center = c)
                }
            }

            // ---- x-axis tick labels (dailyBalanceChart only) ----
            for ((i, label) in tickLabels) {
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    xAt(i),
                    h - 6.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 9.sp.toPx()
                        isAntiAlias = true
                    },
                )
            }
        }

        Text(
            text = "Latest balance: ${CurrencyFormatter.full(points.last().balance)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}

/** Mirrors `parseFloat(scaledV.toFixed(2))` then dropping trailing zeros — "1.50" -> "1.5", "2.00" -> "2". */
private fun formatTrim(v: Double): String {
    val rounded = kotlin.math.round(v * 100) / 100
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString().trimEnd('0').trimEnd('.')
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
