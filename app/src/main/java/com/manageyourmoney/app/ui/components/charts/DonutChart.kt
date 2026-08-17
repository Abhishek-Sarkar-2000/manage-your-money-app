package com.manageyourmoney.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manageyourmoney.app.domain.format.CurrencyFormatter

/**
 * Direct Canvas port of `donutChart(segments)` (index.html:2146-2167). The web version
 * used a CSS `conic-gradient` on a plain div; Compose has no conic-gradient brush, so
 * this draws the same visual with a ring of [androidx.compose.ui.graphics.drawscope.DrawScope.drawArc]
 * calls (one per positive-value segment, in original order, sweeping clockwise from 12
 * o'clock — matching the CSS `conic-gradient`'s default start angle) around a hollow
 * center where the total is overlaid as text.
 *
 * @param emptyMessage shown instead of the chart when every segment is zero/negative,
 *        mirrors `if(total<=0) return '<div class="empty-chart">...'`.
 */
@Composable
fun DonutChart(
    segments: List<ChartSegment>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    val total = segments.sumOf { it.value }
    if (total <= 0.0) {
        EmptyChartMessage(emptyMessage, modifier)
        return
    }
    val positive = segments.filter { it.value > 0 }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier.width(140.dp).aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            val strokeWidthDp = 22.dp
            Canvas(modifier = Modifier.size(140.dp)) {
                val stroke = Stroke(width = strokeWidthDp.toPx())
                val inset = stroke.width / 2f
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                var startAngle = -90f // 12 o'clock, matching conic-gradient's default 0deg start
                for (seg in positive) {
                    val sweep = (seg.value / total * 360.0).toFloat()
                    drawArc(
                        color = seg.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    CurrencyFormatter.short(total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (seg in positive) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(seg.color, CircleShape))
                    Text(seg.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(CurrencyFormatter.full(seg.value), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
internal fun EmptyChartMessage(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
