package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.di.DefaultDispatcher
import com.manageyourmoney.app.domain.model.GlobalStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Direct port of `computeGlobalStats()` (index.html:941-947): the Home screen's single
 * entry point, fanning the four independent computations out in parallel exactly like
 * the JS's `Promise.all([...])`.
 */
class ComputeGlobalStatsUseCase @Inject constructor(
    private val computeGlobalOwed: ComputeGlobalOwedUseCase,
    private val computeGlobalInvestments: ComputeGlobalInvestmentsUseCase,
    private val computeGlobalCardDues: ComputeGlobalCardDuesUseCase,
    private val computeMonthlyBreakdown: ComputeMonthlyBreakdownUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): GlobalStats = withContext(dispatcher) {
        coroutineScope {
            val owedDeferred = async { computeGlobalOwed() }
            val investedDeferred = async { computeGlobalInvestments() }
            val cardDuesDeferred = async { computeGlobalCardDues() }
            val breakdownDeferred = async { computeMonthlyBreakdown() }

            val breakdown = breakdownDeferred.await()
            val amountLeft = breakdown.lastOrNull()?.ending ?: 0.0

            GlobalStats(
                owed = owedDeferred.await(),
                invested = investedDeferred.await(),
                cardDues = cardDuesDeferred.await(),
                breakdown = breakdown,
                amountLeft = amountLeft,
            )
        }
    }
}
