package com.manageyourmoney.app.ui.screens.month

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manageyourmoney.app.data.local.entity.PaymentMode
import com.manageyourmoney.app.data.local.entity.StartingBalanceMode
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.domain.format.CurrencyFormatter
import com.manageyourmoney.app.domain.model.LedgerRow
import com.manageyourmoney.app.domain.util.DateUtils
import com.manageyourmoney.app.ui.components.charts.BarChart
import com.manageyourmoney.app.ui.components.charts.ChartSegment
import com.manageyourmoney.app.ui.components.charts.DonutChart
import com.manageyourmoney.app.ui.theme.MoneyTheme
import com.manageyourmoney.app.ui.viewmodel.MonthDetailViewModel

/**
 * Compose port of the web app's month-detail view: header with starting-balance mode
 * toggle, a donut chart of the month's spend breakdown (cash / card / EMI / invest —
 * mirrors the "Where it went" donut), the tag bar chart, and the chronological entry
 * list with tap-to-delete, plus a bottom-sheet "add entry" form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDetailScreen(
    onBack: () -> Unit,
    viewModel: MonthDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(DateUtils.monthKeyLabel(state.monthKey), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add entry") },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp, innerPadding.calculateTopPadding(), 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                StartingBalanceCard(
                    mode = state.startingBalanceMode,
                    balance = state.startingBalance,
                    onModeChange = viewModel::setStartingBalanceMode,
                    onBalanceChange = viewModel::setStartingBalance,
                )
            }

            item {
                Column {
                    Text("Where it went", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    val semantic = MoneyTheme.semanticColors
                    val segments = listOf(
                        ChartSegment("Cash spend", state.totals.cashSpend, semantic.debit),
                        ChartSegment("Card spend", state.totals.cardPaymentSpend, MaterialTheme.colorScheme.primary),
                        ChartSegment("EMI", state.totals.emi, semantic.amber),
                        ChartSegment("Investment", state.totals.invest, semantic.credit),
                    )
                    DonutChart(segments = segments, emptyMessage = "No spending logged yet this month.")
                }
            }

            if (state.tagTotals.isNotEmpty()) {
                item {
                    Column {
                        Text("By tag", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        val semantic = MoneyTheme.semanticColors
                        val segments = state.tagTotals.mapIndexed { i, t ->
                            ChartSegment(t.person, t.amount, semantic.chartPalette[i % semantic.chartPalette.size])
                        }
                        BarChart(segments = segments, emptyMessage = "No tagged spends yet.", maxBarHeight = 140.dp, shortValues = true)
                    }
                }
            }

            item { Text("Entries", style = MaterialTheme.typography.titleLarge) }

            items(state.rows, key = { it.id }) { row ->
                EntryRow(row = row, onDelete = { if (row is LedgerRow.Entry) viewModel.deleteEntry(row.id) })
            }
        }

        if (showAddSheet) {
            AddEntrySheet(
                availableTags = state.availableTags,
                onDismiss = { showAddSheet = false },
                onAddIncome = { d, a, dt, c -> viewModel.addIncome(d, a, dt, c); showAddSheet = false },
                onAddSpendCash = { d, a, dt, t -> viewModel.addSpend(d, a, dt, PaymentMode.CASH, null, t); showAddSheet = false },
                onAddInvestment = { d, a, dt -> viewModel.addInvestment(d, a, dt); showAddSheet = false },
                onAddOwed = { p, a, dt -> viewModel.addOwed(p, a, dt); showAddSheet = false },
            )
        }
    }
}

@Composable
private fun StartingBalanceCard(
    mode: StartingBalanceMode,
    balance: Double,
    onModeChange: (StartingBalanceMode) -> Unit,
    onBalanceChange: (Double) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Starting balance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow {
                StartingBalanceMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { onModeChange(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, StartingBalanceMode.entries.size),
                    ) { Text(if (m == StartingBalanceMode.AUTO) "Auto (carry forward)" else "Manual") }
                }
            }
            if (mode == StartingBalanceMode.MANUAL) {
                Spacer(Modifier.height(8.dp))
                var text by remember(balance) { mutableStateOf(if (balance == 0.0) "" else balance.toString()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; it.toDoubleOrNull()?.let(onBalanceChange) },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EntryRow(row: LedgerRow, onDelete: () -> Unit) {
    val (title, subtitle, amountColor) = when (row) {
        is LedgerRow.Entry -> Triple(
            row.description,
            "${row.type.name.lowercase().replaceFirstChar { it.uppercase() }} \u00B7 ${row.date}",
            if (row.type == TransactionType.INCOME) MoneyTheme.semanticColors.credit else MoneyTheme.semanticColors.debit,
        )
        is LedgerRow.EmiInstallment -> Triple(
            row.description,
            "EMI ${row.installment}/${row.totalMonths} \u00B7 ${row.date}",
            MoneyTheme.semanticColors.amber,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(CurrencyFormatter.full(row.amount), style = MaterialTheme.typography.labelLarge, color = amountColor)
        if (row is LedgerRow.Entry) {
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
        }
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntrySheet(
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onAddIncome: (String, Double, String, String) -> Unit,
    onAddSpendCash: (String, Double, String, String) -> Unit,
    onAddInvestment: (String, Double, String) -> Unit,
    onAddOwed: (String, Double, String) -> Unit,
) {
    var kind by remember { mutableStateOf(TransactionType.SPEND) }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val date = remember { DateUtils.todayStr() }
    var tag by remember { mutableStateOf(availableTags.firstOrNull() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("Add entry", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            val kinds = listOf(TransactionType.SPEND, TransactionType.INCOME, TransactionType.INVESTMENT, TransactionType.OWED)
            SingleChoiceSegmentedButtonRow {
                kinds.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = kind == t,
                        onClick = { kind = t },
                        shape = SegmentedButtonDefaults.itemShape(i, kinds.size),
                    ) { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (kind == TransactionType.OWED) "Person" else "Description") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            if (kind == TransactionType.SPEND && availableTags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Tag", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableTags.take(6).forEach { t ->
                        FilterChip(selected = tag == t, onClick = { tag = t }, label = { Text(t) })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    when (kind) {
                        TransactionType.INCOME -> onAddIncome(description, amount, date, "")
                        TransactionType.SPEND -> onAddSpendCash(description, amount, date, tag)
                        TransactionType.INVESTMENT -> onAddInvestment(description, amount, date)
                        TransactionType.OWED -> onAddOwed(description, amount, date)
                        TransactionType.CARDCHARGE -> Unit
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
