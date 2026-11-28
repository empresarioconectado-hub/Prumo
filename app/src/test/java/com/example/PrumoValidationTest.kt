package com.example.data

import com.example.model.CashEntry
import com.example.model.CashEntryType
import com.example.model.FixedExpense
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem
import org.junit.Assert.assertThrows
import org.junit.Test

class PrumoValidationTest {
    @Test
    fun cashEntryRejectsZeroAndNegativeAmounts() {
        val base = CashEntry(
            description = "Fornecedor", amountCents = 0L, type = CashEntryType.EXPENSE.name,
            category = "Fornecedores", account = "Conta Principal", paymentMethod = "Pix",
            status = "PAID", dateMillis = 1L
        )
        assertThrows(IllegalArgumentException::class.java) { PrumoValidation.cashEntry(base) }
        assertThrows(IllegalArgumentException::class.java) {
            PrumoValidation.cashEntry(base.copy(amountCents = -1L))
        }
    }

    @Test
    fun stockItemRejectsValuesThatCouldOverflowDerivedMetrics() {
        val base = StockItem(
            sku = "SKU-1", name = "Produto", category = "Mercadorias",
            quantity = Long.MAX_VALUE, minStock = 1L, leadTimeDays = 1L,
            avgDailySales = 1.0, unitCostCents = 100L, unitPriceCents = 200L
        )
        assertThrows(IllegalArgumentException::class.java) { PrumoValidation.stockItem(base) }
    }

    @Test
    fun saleRejectsNonFinitePercentages() {
        val sale = SaleRecord(
            description = "Venda", clientName = "Cliente", sellerName = "Vendedor",
            grossAmountCents = 10_000L, costAmountCents = 4_000L,
            taxPercent = Double.NaN, commissionPercent = 2.0, saleDateMillis = 1L
        )
        assertThrows(IllegalArgumentException::class.java) { PrumoValidation.sale(sale) }
    }

    @Test
    fun sellerRejectsCommissionOutsideBusinessRange() {
        val seller = SellerProfile(name = "Vendedor", monthlyGoalCents = 10_000L, commissionPercent = 101.0)
        assertThrows(IllegalArgumentException::class.java) { PrumoValidation.seller(seller) }
    }

    @Test
    fun fixedExpenseRejectsEmptyDescriptionAndZeroValue() {
        val expense = FixedExpense(description = "", category = "Aluguel", amountCents = 0L, dateMillis = 1L)
        assertThrows(IllegalArgumentException::class.java) { PrumoValidation.fixedExpense(expense) }
    }
}
