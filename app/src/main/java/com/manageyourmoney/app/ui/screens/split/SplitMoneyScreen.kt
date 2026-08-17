package com.manageyourmoney.app.ui.screens.split

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manageyourmoney.app.data.local.entity.SPLIT_YOU
import com.manageyourmoney.app.domain.format.CurrencyFormatter
import com.manageyourmoney.app.domain.model.SettlementCard
import com.manageyourmoney.app.domain.model.SplitGroup
import com.manageyourmoney.app.ui.theme.MoneyTheme
import com.manageyourmoney.app.ui.viewmodel.SplitMoneyViewModel

/**
 * Compose port of the web app's Split Money view: per-person "who you owe" / "who owes
 * you" summary at the top (mirrors `computeSplitPageData().owedByYou/owedToYou`), then
 * every group with its settlement cards from `computeGroupSettlementView()` — settled
 * ones shown as done, outstanding (virtual) ones with a "Settle" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitMoneyScreen(
    onBack: () -> Unit,
    viewModel: SplitMoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showNewGroupSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Money", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewGroupSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New group") },
            )
        },
    ) { innerPadding ->
        val data = state.data
        if (state.isLoading && data == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp, innerPadding.calculateTopPadding(), 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "You owe",
                        amount = data?.owedByYou?.values?.sum() ?: 0.0,
                        color = MoneyTheme.semanticColors.debit,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCard(
                        title = "Owed to you",
                        amount = data?.owedToYou?.values?.sum() ?: 0.0,
                        color = MoneyTheme.semanticColors.credit,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (data == null || data.groups.isEmpty()) {
                item {
                    Text(
                        "No split groups yet. Tap \u201CNew group\u201D to start splitting expenses with friends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(data.groups, key = { it.id }) { group ->
                    val cards = data.allCards.filter { it.groupId == group.id }
                    GroupCard(
                        group = group,
                        cards = cards,
                        onToggleSettle = { c, settle ->
                            viewModel.toggleSettlement(group.id, group.description, c.id, c.from, c.to, c.amount, settle)
                        },
                        onSettleAll = { viewModel.settleAll(group.id) },
                    )
                }
            }
        }

        if (showNewGroupSheet) {
            NewGroupSheet(
                onDismiss = { showNewGroupSheet = false },
                onCreate = { desc, people -> viewModel.createGroup(desc, people) { showNewGroupSheet = false } },
            )
        }
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(CurrencyFormatter.full(amount), style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

@Composable
private fun GroupCard(
    group: SplitGroup,
    cards: List<SettlementCard>,
    onToggleSettle: (SettlementCard, Boolean) -> Unit,
    onSettleAll: () -> Unit,
) {
    val outstanding = cards.filter { !it.settled }
    val settled = cards.filter { it.settled }

    Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(group.description, style = MaterialTheme.typography.titleMedium)
                Text("${group.people.size} people", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))

            if (outstanding.isEmpty() && settled.isEmpty()) {
                Text("Fully settled, or no spends logged yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            outstanding.forEach { c ->
                SettlementRow(card = c, onToggle = { onToggleSettle(c, true) }, actionLabel = "Settle")
            }
            settled.forEach { c ->
                SettlementRow(card = c, onToggle = { onToggleSettle(c, false) }, actionLabel = "Undo", isSettled = true)
            }

            if (outstanding.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSettleAll) { Text("Settle all") }
            }
        }
    }
}

@Composable
private fun SettlementRow(card: SettlementCard, onToggle: () -> Unit, actionLabel: String, isSettled: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${label(card.from)} \u2192 ${label(card.to)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                CurrencyFormatter.full(card.amount),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSettled) MoneyTheme.semanticColors.credit else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (isSettled) {
            AssistChip(onClick = onToggle, label = { Text(actionLabel) }, leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) })
        } else {
            Button(onClick = onToggle) { Text(actionLabel) }
        }
    }
}

private fun label(person: String) = if (person == SPLIT_YOU) "You" else person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewGroupSheet(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var description by remember { mutableStateOf("") }
    var peopleText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("New split group", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What's this for? (e.g. Goa trip)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = peopleText,
                onValueChange = { peopleText = it },
                label = { Text("Other people (comma-separated)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val people = peopleText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (description.isNotBlank()) onCreate(description, people)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create group") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
