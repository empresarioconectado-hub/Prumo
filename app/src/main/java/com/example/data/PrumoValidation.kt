package com.example.data

import com.example.model.BusinessSettings
import com.example.model.CashEntry
import com.example.model.CashEntryStatus
import com.example.model.CashEntryType
import com.example.model.FixedExpense
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem

/**
 * Domain-level input constraints. UI validation is helpful feedback, not a trust boundary.
 */
internal object PrumoValidation {
    const val MAX_MONEY_CENTS = 100_000_000_000_000L
    const val MAX_STOCK_UNITS = 1_000_000_000L
    private const val MAX_UNIT_VALUE_CENTS = 100_000_000L
    private const val MAX_LEAD_TIME_DAYS = 36_500L
    private const val MAX_DAILY_SALES = 1_000_000.0

    fun cashEntry(entry: CashEntry) {
        require(entry.description.isNotBlank() && entry.description.length <= 120) { "Informe uma descrição de até 120 caracteres." }
        require(entry.amountCents in 1L..MAX_MONEY_CENTS) { "O valor do lançamento está fora do limite permitido." }
        require(entry.type in CashEntryType.entries.map { it.name }) { "Tipo de lançamento inválido." }
        require(entry.status in CashEntryStatus.entries.map { it.name }) { "Status do lançamento inválido." }
        require(entry.category.isNotBlank() && entry.account.isNotBlank() && entry.paymentMethod.isNotBlank()) {
            "Categoria, conta e forma de pagamento são obrigatórias."
        }
        require(entry.dateMillis > 0L) { "A data do lançamento é inválida." }
    }

    fun stockItem(item: StockItem) {
        require(item.sku.isNotBlank() && item.sku.length <= 40) { "Informe um SKU de até 40 caracteres." }
        require(item.name.isNotBlank() && item.name.length <= 120) { "Informe o nome do produto." }
        require(item.category.isNotBlank() && item.unit.isNotBlank()) { "Categoria e unidade são obrigatórias." }
        require(item.quantity in 0L..MAX_STOCK_UNITS && item.initialQuantity in 0L..MAX_STOCK_UNITS &&
            item.minStock in 0L..MAX_STOCK_UNITS && item.soldUnitsCount in 0L..MAX_STOCK_UNITS &&
            item.lossUnitsCount in 0L..MAX_STOCK_UNITS) { "Quantidade de estoque fora do limite permitido." }
        require(item.leadTimeDays in 0L..MAX_LEAD_TIME_DAYS &&
            item.avgDailySales.isFinite() && item.avgDailySales in 0.0..MAX_DAILY_SALES) {
            "Parâmetros de reposição inválidos."
        }
        require(item.unitCostCents in 0L..MAX_UNIT_VALUE_CENTS &&
            item.unitPriceCents in 0L..MAX_UNIT_VALUE_CENTS) { "Custo ou preço unitário fora do limite permitido." }
        Math.multiplyExact(item.quantity, item.unitCostCents)
        Math.multiplyExact(item.quantity, item.unitPriceCents)
        Math.multiplyExact(item.reorderPoint, 2L)
    }

    fun sale(sale: SaleRecord) {
        require(sale.description.isNotBlank() && sale.description.length <= 120) { "Informe a descrição da venda." }
        require(sale.quantity in 1L..MAX_STOCK_UNITS) { "Quantidade da venda inválida." }
        require(sale.grossAmountCents in 1L..MAX_MONEY_CENTS &&
            sale.costAmountCents in 0L..MAX_MONEY_CENTS) { "Valores da venda fora do limite permitido." }
        require(sale.taxPercent.isFinite() && sale.taxPercent in 0.0..100.0 &&
            sale.commissionPercent.isFinite() && sale.commissionPercent in 0.0..100.0) { "Percentuais inválidos." }
        require(sale.saleDateMillis > 0L) { "A data da venda é inválida." }
        // Force exact arithmetic now so a value cannot fail later when a report is rendered.
        sale.taxCents
        sale.commissionCents
        sale.contributionMarginCents
    }

    fun seller(seller: SellerProfile) {
        require(seller.name.isNotBlank() && seller.name.length <= 100) { "Informe o nome do vendedor." }
        require(seller.monthlyGoalCents in 0L..MAX_MONEY_CENTS) { "Meta mensal fora do limite permitido." }
        require(seller.commissionPercent.isFinite() && seller.commissionPercent in 0.0..100.0) {
            "Comissão deve estar entre 0% e 100%."
        }
    }

    fun fixedExpense(expense: FixedExpense) {
        require(expense.description.isNotBlank() && expense.description.length <= 120) { "Informe a descrição da despesa." }
        require(expense.category.isNotBlank()) { "Selecione uma categoria para a despesa." }
        require(expense.amountCents in 1L..MAX_MONEY_CENTS) { "Valor da despesa fora do limite permitido." }
        require(expense.dateMillis > 0L) { "A data da despesa é inválida." }
    }

    fun settings(settings: BusinessSettings) {
        require(settings.businessName.isNotBlank() && settings.businessName.length <= 120) { "Informe o nome do negócio." }
        require(settings.initialBalanceCents in -MAX_MONEY_CENTS..MAX_MONEY_CENTS) { "Saldo inicial fora do limite permitido." }
        require(settings.minCashAlertCents in 0L..MAX_MONEY_CENTS) { "Alerta mínimo fora do limite permitido." }
        require(settings.taxRatePercent.isFinite() && settings.taxRatePercent in 0.0..100.0) { "Alíquota inválida." }
    }
}
