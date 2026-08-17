package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.domain.model.LedgerRow
import com.manageyourmoney.app.domain.model.PersonAmount
import javax.inject.Inject

/**
 * Direct port of the aggregation half of `tagsBarChart(entries)` (index.html:2178-2186)
 * — the actual bar rendering is [com.manageyourmoney.app.ui.components.charts.BarChart],
 * this just produces the sorted (label, value) pairs it needs. Reuses [PersonAmount]'s
 * shape (`{label/person, amount}`) rather than adding a near-identical data class.
 */
class ComputeTagTotalsUseCase @Inject constructor() {
    operator fun invoke(rows: List<LedgerRow.Entry>): List<PersonAmount> {
        val totals = LinkedHashMap<String, Double>()
        for (e in rows) {
            if (e.type == TransactionType.SPEND || e.type == TransactionType.CARDCHARGE) {
                val tag = e.tag?.trim().takeUnless { it.isNullOrEmpty() } ?: "Untagged"
                totals[tag] = (totals[tag] ?: 0.0) + e.amount
            }
        }
        return totals.entries
            .map { (label, value) -> PersonAmount(label, value) }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
    }
}
