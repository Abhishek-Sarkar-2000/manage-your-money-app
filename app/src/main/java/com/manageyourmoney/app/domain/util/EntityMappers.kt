package com.manageyourmoney.app.domain.util

import com.manageyourmoney.app.data.local.dao.SplitGroupWithDetails
import com.manageyourmoney.app.data.local.dao.SplitSpendWithShares
import com.manageyourmoney.app.data.local.dao.TransactionWithLentShares
import com.manageyourmoney.app.domain.model.LedgerRow
import com.manageyourmoney.app.domain.model.LentShare
import com.manageyourmoney.app.domain.model.SplitGroup
import com.manageyourmoney.app.domain.model.SplitSettlement
import com.manageyourmoney.app.domain.model.SplitSpend

fun TransactionWithLentShares.toDomain(): LedgerRow.Entry = LedgerRow.Entry(
    id = transaction.id,
    monthKey = transaction.monthKey,
    type = transaction.type,
    description = transaction.description,
    amount = transaction.amount,
    date = transaction.date,
    paymentMode = transaction.paymentMode,
    cardId = transaction.cardId,
    tag = transaction.tag,
    category = transaction.category,
    settled = transaction.settled,
    lent = lentShares.map { LentShare(it.id, it.person, it.amount, it.settled) },
    fromSplitSettlementId = transaction.fromSplitSettlementId,
)

fun SplitSpendWithShares.toDomain(): SplitSpend = SplitSpend(
    id = spend.id,
    payee = spend.payee,
    amount = spend.amount,
    description = spend.description,
    date = spend.date,
    shares = shares.associate { it.person to it.amount },
)

fun SplitGroupWithDetails.toDomain(): SplitGroup = SplitGroup(
    id = group.id,
    description = group.description,
    createdAt = group.createdAt,
    // Ensures "YOU" is always present even for a freshly created group with no other
    // members yet, mirroring `loadSplit()`'s `people.length ? people : [SPLIT_YOU]` guard —
    // here SPLIT_YOU is always inserted at group-creation time (see CreateSplitGroupUseCase),
    // so this is just restoring insertion order.
    people = people.sortedBy { it.sortOrder }.map { it.name },
    spends = spends.map { it.toDomain() },
    settlements = settlements.map {
        SplitSettlement(it.id, it.from, it.to, it.amount, it.settled, it.ledgerEntryId, it.monthKey)
    },
)
