package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.entity.PaymentMode
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.domain.model.LedgerRow
import com.manageyourmoney.app.domain.model.MonthTotals
import javax.inject.Inject

/**
 * Direct, pure port of `computeMonthTotals(entries)` (index.html:770-784). Takes the
 * combined real-entries + synthesized-EMI-rows list for one month, exactly like the
 * JS call site `computeMonthTotals(data.entries.concat(emiRows))`.
 */
class ComputeMonthTotalsUseCase @Inject constructor() {
    operator fun invoke(rows: List<LedgerRow>): MonthTotals {
        var income = 0.0
        var cashSpend = 0.0
        var cardPaymentSpend = 0.0
        var cardCharge = 0.0
        var invest = 0.0
        var emi = 0.0

        for (row in rows) {
            when (row) {
                is LedgerRow.EmiInstallment -> emi += row.amount
                is LedgerRow.Entry -> when (row.type) {
                    TransactionType.INCOME -> income += row.amount
                    TransactionType.SPEND -> {
                        if (row.paymentMode == PaymentMode.CARD) cardPaymentSpend += row.amount
                        else cashSpend += row.amount
                    }
                    TransactionType.CARDCHARGE -> cardCharge += row.amount
                    TransactionType.INVESTMENT -> invest += row.amount
                    TransactionType.OWED -> Unit // owed entries don't touch month totals, same as the JS switch falling through
                }
            }
        }
        return MonthTotals(
            income = income,
            cashSpend = cashSpend,
            cardPaymentSpend = cardPaymentSpend,
            cardCharge = cardCharge,
            invest = invest,
            emi = emi,
        )
    }
}
