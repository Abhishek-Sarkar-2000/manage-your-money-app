package com.manageyourmoney.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manageyourmoney.app.data.local.entity.SPLIT_YOU
import com.manageyourmoney.app.data.repository.MoneyRepository
import com.manageyourmoney.app.domain.model.SplitPageData
import com.manageyourmoney.app.domain.usecase.ComputeSplitPageDataUseCase
import com.manageyourmoney.app.domain.usecase.SettleAllInGroupUseCase
import com.manageyourmoney.app.domain.usecase.ToggleSplitSettlementUseCase
import com.manageyourmoney.app.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitMoneyUiState(
    val isLoading: Boolean = true,
    val data: SplitPageData? = null,
)

/**
 * Split Money screen ViewModel — the Compose equivalent of `render()`'s split-money
 * branch: loads [ComputeSplitPageDataUseCase]'s aggregate view, and wires the settle
 * toggle / settle-all / new-group actions through their respective use cases.
 */
@HiltViewModel
class SplitMoneyViewModel @Inject constructor(
    private val repository: MoneyRepository,
    private val computeSplitPageData: ComputeSplitPageDataUseCase,
    private val toggleSplitSettlement: ToggleSplitSettlementUseCase,
    private val settleAllInGroup: SettleAllInGroupUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitMoneyUiState())
    val uiState: StateFlow<SplitMoneyUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val data = computeSplitPageData()
            _uiState.value = SplitMoneyUiState(isLoading = false, data = data)
        }
    }

    fun toggleSettlement(groupId: String, groupDesc: String, transferId: String, from: String, to: String, amount: Double, settle: Boolean) {
        viewModelScope.launch {
            toggleSplitSettlement(groupId, transferId, from, to, amount, groupDesc, settle)
            refresh()
        }
    }

    fun settleAll(groupId: String) {
        viewModelScope.launch {
            val group = _uiState.value.data?.groups?.firstOrNull { it.id == groupId } ?: return@launch
            settleAllInGroup(group)
            refresh()
        }
    }

    fun createGroup(description: String, otherPeople: List<String>, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val people = listOf(SPLIT_YOU) + otherPeople.filter { it.isNotBlank() }
            val id = repository.createSplitGroup(description, people)
            refresh()
            onCreated(id)
        }
    }

    fun addSpend(groupId: String, payee: String, amount: Double, description: String, shares: Map<String, Double>) {
        viewModelScope.launch {
            repository.addSplitSpend(groupId, payee, amount, description, DateUtils.todayStr(), shares)
            refresh()
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            repository.deleteSplitGroup(groupId)
            refresh()
        }
    }
}
