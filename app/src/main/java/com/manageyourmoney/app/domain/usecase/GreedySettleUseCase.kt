package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.domain.model.SplitGroup
import com.manageyourmoney.app.domain.model.SplitSettlement
import com.manageyourmoney.app.domain.model.Transfer
import kotlin.math.round
import javax.inject.Inject

/** Rounds to 2 decimal places the same way the JS `Math.round(v*100)/100` does. */
private fun round2(v: Double): Double = round(v * 100) / 100

/** Direct port of `computeGroupPaid(group)` (index.html:950-957) — how much each
 *  person actually fronted across the group's spends. */
class ComputeGroupPaidUseCase @Inject constructor() {
    operator fun invoke(group: SplitGroup): Map<String, Double> {
        val paid = LinkedHashMap<String, Double>()
        for (p in group.people) paid[p] = 0.0
        for (s in group.spends) paid[s.payee] = (paid[s.payee] ?: 0.0) + s.amount
        return paid
    }
}

/** Direct port of `computeGroupNet(group)` (index.html:958-968) — each person's raw net
 *  position: what they fronted, minus what they owe across every spend's shares. */
class ComputeGroupNetUseCase @Inject constructor() {
    operator fun invoke(group: SplitGroup): Map<String, Double> {
        val net = LinkedHashMap<String, Double>()
        for (p in group.people) net[p] = 0.0
        for (s in group.spends) {
            net[s.payee] = (net[s.payee] ?: 0.0) + s.amount
            for ((p, amt) in s.shares) {
                net[p] = (net[p] ?: 0.0) - amt
            }
        }
        return net
    }
}

/** Direct port of `applySettledAdjustments(net, settlements)` (index.html:969-977) —
 *  folds already-settled transfers back into net so they don't get re-suggested. */
class ApplySettledAdjustmentsUseCase @Inject constructor() {
    operator fun invoke(net: Map<String, Double>, settlements: List<SplitSettlement>): Map<String, Double> {
        val adjusted = LinkedHashMap(net)
        for (st in settlements) {
            if (!st.settled) continue
            adjusted[st.from] = (adjusted[st.from] ?: 0.0) + st.amount
            adjusted[st.to] = (adjusted[st.to] ?: 0.0) - st.amount
        }
        return adjusted
    }
}

/**
 * Direct port of `greedySettle(net)` (index.html:978-998): a greedy debt-minimization
 * matcher — largest creditor paired against largest debtor, repeat — NOT a
 * globally-optimal min-transaction solver, deliberately mirroring the original's
 * trade-off of simplicity over minimality.
 */
class GreedySettleUseCase @Inject constructor() {
    private data class Mutable(val person: String, var amt: Double)

    operator fun invoke(net: Map<String, Double>): List<Transfer> {
        val creditors = mutableListOf<Mutable>()
        val debtors = mutableListOf<Mutable>()
        for ((p, v) in net) {
            val r = round2(v)
            if (r > 0.004) creditors.add(Mutable(p, r))
            else if (r < -0.004) debtors.add(Mutable(p, -r))
        }
        creditors.sortByDescending { it.amt }
        debtors.sortByDescending { it.amt }

        val transfers = mutableListOf<Transfer>()
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val d = debtors[i]
            val c = creditors[j]
            val amt = round2(minOf(d.amt, c.amt))
            if (amt > 0.004) transfers.add(Transfer(d.person, c.person, amt))
            d.amt -= amt
            c.amt -= amt
            if (d.amt <= 0.004) i++
            if (c.amt <= 0.004) j++
        }
        return transfers
    }
}
