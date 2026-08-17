package com.manageyourmoney.app.ui.components.charts

import androidx.compose.ui.graphics.Color

/** One slice of a [DonutChart] / one bar of a [BarChart] — label, value, and the color
 *  to render it in. Mirrors the `{label, value, color}` segment shape both
 *  `donutChart()` and `barChart()` took in the web app. */
data class ChartSegment(
    val label: String,
    val value: Double,
    val color: Color,
)
