package com.manageyourmoney.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.manageyourmoney.app.domain.format.CurrencyFormatter

/**
 * Direct port of `barChart(pairs)` (index.html:2168-2177) and `tagsBarChart(entries)`'s
 * rendering half (index.html:2178-2197) — both were the same visual shape (a scrollable
 * row of columns, each a value label, a proportional-height colored bar, and a
 * bottom label), just fed different data. The tag-totals aggregation itself belongs in
 * the domain layer (a ViewModel groups entries by tag before calling this), not here.
 *
 * @param maxBarHeight mirrors the JS's fixed max pixel heights (130px for [barChart],
 *        140px for [tagsBarChart]) — pass 130.dp/140.dp respectively.
 * @param shortValues mirrors [tagsBarChart] using `fmtINRShort` for the value label
 *        while [barChart] used the full `fmtINR` — false reproduces [barChart]'s behavior.
 */
@Composable
fun BarChart(
    segments: List<ChartSegment>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    maxBarHeight: Dp = 130.dp,
    shortValues: Boolean = false,
) {
    if (segments.isEmpty()) {
        EmptyChartMessage(emptyMessage, modifier)
        return
    }
    val max = (segments.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        for (seg in segments) {
            Column(
                modifier = Modifier.widthIn(min = 56.dp, max = 84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (shortValues) CurrencyFormatter.short(seg.value) else CurrencyFormatter.full(seg.value),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                val barHeight = (seg.value / max * maxBarHeight.value).coerceAtLeast(4.0).dp
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(barHeight)
                        .background(seg.color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    seg.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
