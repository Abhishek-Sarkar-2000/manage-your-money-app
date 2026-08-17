package com.manageyourmoney.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manageyourmoney.app.domain.format.CurrencyFormatter
import com.manageyourmoney.app.domain.model.MonthBreakdownRow
import com.manageyourmoney.app.domain.util.DateUtils
import com.manageyourmoney.app.ui.components.charts.RunningBalanceLineChart
import com.manageyourmoney.app.ui.theme.MoneyTheme
import com.manageyourmoney.app.ui.viewmodel.BalanceChartRange
import com.manageyourmoney.app.ui.viewmodel.HomeViewModel

/**
 * Compose port of the web app's Home view (`State.view === 'home'`): the current-month
 * hero card, horizontally-scrolling stat cards (amount left, owed, invested, card
 * dues — mirrors the `.hstats` scroll track), the running-balance chart with its
 * 1M/3M/6M range pills, and the chronological month list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMonth: (String) -> Unit,
    onOpenSplitMoney: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Manage Your Money", style = MaterialTheme.typography.headlineSmall) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.addNextMonth(onOpenMonth) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add month") },
            )
        },
    ) { innerPadding ->
        if (state.isLoading && state.stats == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val stats = state.stats
        LazyColumn(
            contentPadding = PaddingValues(16.dp, innerPadding.calculateTopPadding(), 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    "Amount left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    CurrencyFormatter.full(stats?.amountLeft ?: 0.0),
                    style = MaterialTheme.typography.displayLarge,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard("Owed to you", CurrencyFormatter.short(stats?.owed?.total ?: 0.0), MaterialTheme.colorScheme.primaryContainer)
                    StatCard("Invested", CurrencyFormatter.short(stats?.invested?.total ?: 0.0), MoneyTheme.semanticColors.creditContainer)
                    StatCard("Card dues", CurrencyFormatter.short(stats?.cardDues?.total ?: 0.0), MoneyTheme.semanticColors.debitContainer)
                    StatCard("Months tracked", (stats?.breakdown?.size ?: 0).toString(), MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Balance trend", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BalanceChartRange.entries.forEach { range ->
                                FilterChip(
                                    selected = state.chartRange == range,
                                    onClick = { viewModel.setChartRange(range) },
                                    label = { Text(range.label) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    RunningBalanceLineChart(
                        points = state.windowedSeries,
                        emptyMessage = "Add a month to see your balance trend here.",
                        padValueRange = true,
                        tickLabels = state.chartTickLabels,
                    )
                }
            }

            item {
                Text("Months", style = MaterialTheme.typography.titleLarge)
            }

            items(stats?.breakdown?.asReversed() ?: emptyList(), key = { it.monthKey }) { row ->
                MonthRow(row = row, onClick = { onOpenMonth(row.monthKey) })
            }

            item {
                androidx.compose.material3.OutlinedButton(onClick = onOpenSplitMoney, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Split Money")
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, containerColor: Color) {
    Box(
        modifier = Modifier
            .widthIn(min = 140.dp)
            .background(containerColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MonthRow(row: MonthBreakdownRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .then(Modifier.clickable(onClick = onClick))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(DateUtils.monthKeyLabel(row.monthKey), style = MaterialTheme.typography.titleMedium)
            Text(
                "Starting ${CurrencyFormatter.short(row.starting)} \u00B7 Ending ${CurrencyFormatter.short(row.ending)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Open ${DateUtils.monthKeyLabel(row.monthKey)}")
    }
}
