package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.domain.model.GroupSettlementView
import com.manageyourmoney.app.domain.model.SettlementCard
import com.manageyourmoney.app.domain.model.SplitGroup
import javax.inject.Inject

/**
 * Direct port of `computeGroupSettlementView(group)` (index.html:1001-1015): combines
 * already-settled records (as persisted) with freshly-computed outstanding transfers
 * (virtual — not saved until the user taps "settle").
 */
class ComputeGroupSettlementViewUseCase @Inject constructor(
    private val computeGroupNet: ComputeGroupNetUseCase,
    private val computeGroupPaid: ComputeGroupPaidUseCase,
    private val applySettledAdjustments: ApplySettledAdjustmentsUseCase,
    private val greedySettle: GreedySettleUseCase,
) {
    operator fun invoke(group: SplitGroup): GroupSettlementView {
        val rawNet = computeGroupNet(group)
        val paid = computeGroupPaid(group)
        val adjustedNet = applySettledAdjustments(rawNet, group.settlements)
        val outstanding = greedySettle(adjustedNet)

        val cards = mutableListOf<SettlementCard>()
        for (st in group.settlements) {
            if (!st.settled) continue
            cards.add(SettlementCard(st.id, st.from, st.to, st.amount, settled = true, ledgerEntryId = st.ledgerEntryId, monthKey = st.monthKey))
        }
        for (t in outstanding) {
            cards.add(SettlementCard(id = "virtual-${t.from}-${t.to}", from = t.from, to = t.to, amount = t.amount, settled = false))
        }
        return GroupSettlementView(rawNet, paid, cards)
    }
}
