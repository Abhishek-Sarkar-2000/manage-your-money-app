package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.SplitGroupDao
import com.manageyourmoney.app.data.local.entity.SPLIT_YOU
import com.manageyourmoney.app.di.DefaultDispatcher
import com.manageyourmoney.app.domain.model.PersonAmount
import com.manageyourmoney.app.domain.model.SettlementCard
import com.manageyourmoney.app.domain.model.SplitPageData
import com.manageyourmoney.app.domain.util.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Direct port of `computeSplitPageData()` (index.html:1016-1030): loads every split
 * group, flattens all their settlement cards (settled + virtual outstanding) into one
 * list, and buckets the unsettled ones by direction relative to [SPLIT_YOU].
 */
class ComputeSplitPageDataUseCase @Inject constructor(
    private val splitGroupDao: SplitGroupDao,
    private val computeGroupSettlementView: ComputeGroupSettlementViewUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): SplitPageData = withContext(dispatcher) {
        val groups = splitGroupDao.getAllGroups().map { it.toDomain() }

        val allCards = mutableListOf<SettlementCard>()
        for (g in groups) {
            val view = computeGroupSettlementView(g)
            for (c in view.cards) {
                allCards.add(c.copy(groupId = g.id, groupDesc = g.description))
            }
        }

        val owedByYou = LinkedHashMap<String, Double>()
        val owedToYou = LinkedHashMap<String, Double>()
        for (c in allCards) {
            if (c.settled) continue
            if (c.from == SPLIT_YOU) owedByYou[c.to] = (owedByYou[c.to] ?: 0.0) + c.amount
            if (c.to == SPLIT_YOU) owedToYou[c.from] = (owedToYou[c.from] ?: 0.0) + c.amount
        }

        SplitPageData(groups, allCards, owedByYou, owedToYou)
    }
}

/** Direct port of `computeGlobalSplitOwedByYou()` (index.html:1031-1036). */
class ComputeGlobalSplitOwedByYouUseCase @Inject constructor(
    private val computeSplitPageData: ComputeSplitPageDataUseCase,
) {
    data class Result(val total: Double, val list: List<PersonAmount>)

    suspend operator fun invoke(): Result {
        val data = computeSplitPageData()
        val list = data.owedByYou.entries
            .map { (person, amount) -> PersonAmount(person, amount) }
            .sortedByDescending { it.amount }
        return Result(total = list.sumOf { it.amount }, list = list)
    }
}
