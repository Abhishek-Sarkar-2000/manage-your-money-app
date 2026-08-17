package com.manageyourmoney.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.manageyourmoney.app.data.local.entity.MonthEntity
import com.manageyourmoney.app.data.local.entity.PaymentMode
import com.manageyourmoney.app.data.local.entity.StartingBalanceMode
import com.manageyourmoney.app.data.local.entity.TransactionEntity
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.data.repository.MoneyRepository
import com.manageyourmoney.app.domain.model.LedgerRow
import com.manageyourmoney.app.domain.model.MonthTotals
import com.manageyourmoney.app.domain.model.PersonAmount
import com.manageyourmoney.app.domain.usecase.ComputeMonthTotalsUseCase
import com.manageyourmoney.app.domain.usecase.ComputeTagTotalsUseCase
import com.manageyourmoney.app.domain.usecase.EmiRowsForMonthUseCase
import com.manageyourmoney.app.domain.usecase.GetAllSpendTagsUseCase
import com.manageyourmoney.app.domain.util.toDomain
import com.manageyourmoney.app.ui.navigation.MoneyRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonthDetailUiState(
    val monthKey: String = "",
    val isLoading: Boolean = true,
    val startingBalanceMode: StartingBalanceMode = StartingBalanceMode.MANUAL,
    val startingBalance: Double = 0.0,
    val rows: List<LedgerRow> = emptyList(),
    val totals: MonthTotals = MonthTotals(),
    val tagTotals: List<PersonAmount> = emptyList(),
    val availableTags: List<String> = emptyList(),
)

/**
 * Month Detail screen ViewModel — the Compose equivalent of `render()`'s month-detail
 * branch: loads one month's entries + synthesized EMI rows, keeps `computeMonthTotals()`
 * and the tag breakdown live as the entry list changes, and exposes the starting-balance
 * mode toggle from `MonthEntity`.
 */
@HiltViewModel
class MonthDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MoneyRepository,
    private val emiRowsForMonth: EmiRowsForMonthUseCase,
    private val computeMonthTotals: ComputeMonthTotalsUseCase,
    private val computeTagTotals: ComputeTagTotalsUseCase,
    private val getAllSpendTags: GetAllSpendTagsUseCase,
) : ViewModel() {

    private val monthKey: String = savedStateHandle.toRoute<MoneyRoute.MonthDetail>().monthKey

    private val _uiState = MutableStateFlow(MonthDetailUiState(monthKey = monthKey))
    val uiState: StateFlow<MonthDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureMonthIndexed(monthKey)
            _uiState.value = _uiState.value.copy(availableTags = getAllSpendTags())
        }
        observeEntries()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            repository.observeMonthEntries(monthKey).collectLatest { withLent ->
                val entries = withLent.map { it.toDomain() }
                recomputeAndEmit(entries)
            }
        }
    }

    private suspend fun recomputeAndEmit(entries: List<LedgerRow.Entry>) {
        val emiRows = emiRowsForMonth.forMonth(monthKey)
        val allRows: List<LedgerRow> = entries + emiRows
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            rows = allRows.sortedByDescending { it.date },
            totals = computeMonthTotals(allRows),
            tagTotals = computeTagTotals(entries),
        )
    }

    fun setStartingBalanceMode(mode: StartingBalanceMode) {
        viewModelScope.launch {
            repository.upsertMonth(MonthEntity(monthKey, mode, _uiState.value.startingBalance))
            _uiState.value = _uiState.value.copy(startingBalanceMode = mode)
        }
    }

    fun setStartingBalance(amount: Double) {
        viewModelScope.launch {
            repository.upsertMonth(MonthEntity(monthKey, _uiState.value.startingBalanceMode, amount))
            _uiState.value = _uiState.value.copy(startingBalance = amount)
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    fun addIncome(description: String, amount: Double, date: String, category: String) {
        viewModelScope.launch {
            repository.upsertTransaction(
                TransactionEntity(
                    id = repository.newId(), monthKey = monthKey, type = TransactionType.INCOME,
                    description = description, amount = amount, date = date, category = category,
                )
            )
        }
    }

    fun addSpend(description: String, amount: Double, date: String, paymentMode: PaymentMode, cardId: String?, tag: String) {
        viewModelScope.launch {
            repository.upsertTransaction(
                TransactionEntity(
                    id = repository.newId(), monthKey = monthKey, type = TransactionType.SPEND,
                    description = description, amount = amount, date = date,
                    paymentMode = paymentMode, cardId = if (paymentMode == PaymentMode.CARD) cardId else null, tag = tag,
                )
            )
        }
    }

    fun addCardCharge(description: String, amount: Double, date: String, cardId: String, tag: String) {
        viewModelScope.launch {
            repository.upsertTransaction(
                TransactionEntity(
                    id = repository.newId(), monthKey = monthKey, type = TransactionType.CARDCHARGE,
                    description = description, amount = amount, date = date, cardId = cardId, tag = tag,
                )
            )
        }
    }

    fun addInvestment(description: String, amount: Double, date: String) {
        viewModelScope.launch {
            repository.upsertTransaction(
                TransactionEntity(
                    id = repository.newId(), monthKey = monthKey, type = TransactionType.INVESTMENT,
                    description = description, amount = amount, date = date,
                )
            )
        }
    }

    fun addOwed(person: String, amount: Double, date: String) {
        viewModelScope.launch {
            repository.upsertTransaction(
                TransactionEntity(
                    id = repository.newId(), monthKey = monthKey, type = TransactionType.OWED,
                    description = person, amount = amount, date = date, settled = false,
                )
            )
        }
    }

    fun toggleOwedSettled(id: String, settled: Boolean) {
        viewModelScope.launch { repository.setOwedSettled(id, settled) }
    }

    fun deleteEmiForThisMonth(seriesId: String) {
        viewModelScope.launch {
            repository.deleteEmiForMonth(monthKey, seriesId)
            recomputeAndEmit(_uiState.value.rows.filterIsInstance<LedgerRow.Entry>())
        }
    }
}
