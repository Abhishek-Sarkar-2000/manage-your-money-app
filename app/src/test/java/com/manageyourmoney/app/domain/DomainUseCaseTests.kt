package com.manageyourmoney.app.domain

import com.manageyourmoney.app.data.local.entity.PaymentMode
import com.manageyourmoney.app.data.local.entity.TransactionType
import com.manageyourmoney.app.domain.format.CurrencyFormatter
import com.manageyourmoney.app.domain.model.LedgerRow
import com.manageyourmoney.app.domain.usecase.ComputeMonthTotalsUseCase
import com.manageyourmoney.app.domain.usecase.GreedySettleUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {
    @Test fun `full formats with Indian grouping`() {
        assertEquals("\u20B912,34,567.00", CurrencyFormatter.full(1234567.0))
    }

    @Test fun `short scales to lakh`() {
        assertEquals("\u20B91.5L", CurrencyFormatter.short(150000.0))
    }

    @Test fun `short scales to crore`() {
        assertEquals("\u20B91Cr", CurrencyFormatter.short(10000000.0))
    }

    @Test fun `short drops trailing zero decimal`() {
        assertEquals("\u20B92K", CurrencyFormatter.short(2000.0))
    }

    @Test fun `full handles negative amounts`() {
        assertEquals("-\u20B9500.00", CurrencyFormatter.full(-500.0))
    }
}

class GreedySettleUseCaseTest {
    private val useCase = GreedySettleUseCase()

    @Test fun `three-way debt collapses to minimal transfers`() {
        // A owes 100, split between B (creditor +60) and C (creditor +40)
        val net = mapOf("A" to -100.0, "B" to 60.0, "C" to 40.0)
        val transfers = useCase(net)
        assertEquals(2, transfers.size)
        assertEquals(100.0, transfers.sumOf { it.amount }, 0.001)
        assertEquals("A", transfers[0].from) // largest creditor (B, 60) matched first
    }

    @Test fun `balanced group produces no transfers`() {
        val net = mapOf("A" to 0.0, "B" to 0.0)
        assertEquals(0, useCase(net).size)
    }

    @Test fun `sub-cent residue is ignored`() {
        val net = mapOf("A" to -0.003, "B" to 0.003)
        assertEquals(0, useCase(net).size)
    }
}

class ComputeMonthTotalsUseCaseTest {
    private val useCase = ComputeMonthTotalsUseCase()

    @Test fun `separates cash and card spend`() {
        val rows = listOf(
            LedgerRow.Entry("1", "2026-08", TransactionType.SPEND, "Groceries", 500.0, "2026-08-01", paymentMode = PaymentMode.CASH),
            LedgerRow.Entry("2", "2026-08", TransactionType.SPEND, "Flight", 3000.0, "2026-08-02", paymentMode = PaymentMode.CARD),
            LedgerRow.Entry("3", "2026-08", TransactionType.INCOME, "Salary", 50000.0, "2026-08-01"),
            LedgerRow.EmiInstallment("emi-1", "2026-08-01", "Laptop EMI", 2000.0, "series-1", 3, 12),
        )
        val totals = useCase(rows)
        assertEquals(500.0, totals.cashSpend, 0.001)
        assertEquals(3000.0, totals.cardPaymentSpend, 0.001)
        assertEquals(50000.0, totals.income, 0.001)
        assertEquals(2000.0, totals.emi, 0.001)
        assertEquals(500.0 + 3000.0 + 2000.0, totals.cashOutflow, 0.001)
    }
}
