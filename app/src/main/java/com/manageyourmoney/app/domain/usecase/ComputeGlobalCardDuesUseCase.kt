package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.CreditCardDao
import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.entity.PaymentMode
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.di.DefaultDispatcher
import com.manageyourmoney.app.domain.model.CardDueItem
import com.manageyourmoney.app.domain.model.CardDuesSummary
import com.manageyourmoney.app.domain.util.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Direct port of `computeGlobalCardDues()` (index.html:920-939): a card's outstanding
 * dues = every `cardcharge` billed to it, minus every card-mode `spend` actually paid
 * against it (a payment reduces the due).
 */
class ComputeGlobalCardDuesUseCase @Inject constructor(
    private val monthDao: MonthDao,
    private val transactionDao: TransactionDao,
    private val creditCardDao: CreditCardDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): CardDuesSummary = withContext(dispatcher) {
        val cards = creditCardDao.getAllCards().associateBy { it.id }
        val duesByCardId = HashMap<String, Double>()
        for (id in cards.keys) duesByCardId[id] = 0.0

        for (k in monthDao.getMonthKeysSorted()) {
            val entries = transactionDao.getMonthEntries(k).map { it.toDomain() }
            for (e in entries) {
                if (e.type == TransactionType.CARDCHARGE && e.cardId != null) {
                    duesByCardId[e.cardId] = (duesByCardId[e.cardId] ?: 0.0) + e.amount
                }
                if (e.type == TransactionType.SPEND && e.paymentMode == PaymentMode.CARD && e.cardId != null) {
                    duesByCardId[e.cardId] = (duesByCardId[e.cardId] ?: 0.0) - e.amount
                }
            }
        }

        val list = duesByCardId.entries
            .mapNotNull { (cardId, dues) -> cards[cardId]?.let { CardDueItem(cardId, it.name, dues) } }
        CardDuesSummary(total = list.sumOf { it.dues }, list = list)
    }
}
