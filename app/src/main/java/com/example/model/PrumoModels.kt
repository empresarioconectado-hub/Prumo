package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.max

private fun Long.percentOf(percent: Double): Long =
    BigDecimal.valueOf(this).multiply(BigDecimal.valueOf(percent))
        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValueExact()

enum class CashEntryType(val label: String) {
    INCOME("Entrada"), EXPENSE("Saída");
    companion object {
        fun fromRaw(raw: String): CashEntryType = entries.firstOrNull { it.name == raw } ?: INCOME
    }
}
enum class CashEntryStatus(val label: String) {
    PAID("Pago"), PENDING("Pendente");
    companion object {
        fun fromRaw(raw: String): CashEntryStatus = entries.firstOrNull { it.name == raw } ?: PAID
    }
}
enum class StockStatus(val label: String) { NORMAL("NORMAL"), REORDER("REPOR AGORA"), OUT_OF_STOCK("CRÍTICO - ZERADO") }
enum class StockMovementType(val label: String) {
    PURCHASE("Entrada - Compra"), SALE("Saída - Venda"), LOSS("Ajuste - Perda"), ADJUSTMENT("Ajuste de estoque")
}

@Entity(tableName = "cash_entries")
data class CashEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val description: String,
    val amountCents: Long,
    val type: String,
    val category: String,
    val account: String,
    val paymentMethod: String,
    val status: String,
    val dateMillis: Long,
    val saleId: Long? = null
) {
    val entryType: CashEntryType get() = CashEntryType.fromRaw(type)
    val entryStatus: CashEntryStatus get() = CashEntryStatus.fromRaw(status)
    val isPartnerWithdrawal: Boolean get() = entryType == CashEntryType.EXPENSE &&
        (category.contains("Pró-labore", true) || category.contains("Retirada", true))
}

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sku: String,
    val name: String,
    val category: String,
    val unit: String = "un",
    val quantity: Long,
    val minStock: Long,
    val leadTimeDays: Long,
    val avgDailySales: Double,
    val unitCostCents: Long,
    val unitPriceCents: Long,
    val soldUnitsCount: Long = 0L,
    val lossUnitsCount: Long = 0L,
    @ColumnInfo(defaultValue = "0") val initialQuantity: Long = quantity
) {
    val reorderPoint: Long get() = ceil(avgDailySales * leadTimeDays).toLong() + minStock
    val status: StockStatus get() = when {
        quantity <= 0L -> StockStatus.OUT_OF_STOCK
        quantity <= reorderPoint -> StockStatus.REORDER
        else -> StockStatus.NORMAL
    }
    val stockCostValueCents: Long get() = max(0L, quantity) * unitCostCents
    val potentialSaleValueCents: Long get() = max(0L, quantity) * unitPriceCents
    val turnoverRatio: Double get() {
        val average = (initialQuantity.toDouble() + quantity.toDouble()) / 2.0
        return if (average > 0.0) soldUnitsCount.toDouble() / average else 0.0
    }
    val suggestedPurchaseQty: Long get() {
        if (status == StockStatus.NORMAL) return 0L
        val target = max(reorderPoint * 2L, minStock + 5L)
        return max(1L, target - max(0L, quantity))
    }
    val estimatedPurchaseCostCents: Long get() = suggestedPurchaseQty * unitCostCents
}

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val stockItemId: Long,
    val sku: String,
    val productName: String,
    val movementType: String,
    val quantity: Long,
    val unitCostCents: Long,
    val referenceNote: String,
    val dateMillis: Long
)

@Entity(tableName = "sale_records")
data class SaleRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val description: String,
    val clientName: String,
    val sellerName: String,
    val quantity: Long = 1L,
    val grossAmountCents: Long,
    val costAmountCents: Long,
    val taxPercent: Double,
    val commissionPercent: Double,
    val saleDateMillis: Long,
    val stockItemId: Long? = null,
    @ColumnInfo(defaultValue = "'Pix'") val paymentMethod: String = "Pix",
    @ColumnInfo(defaultValue = "'Conta Principal'") val account: String = "Conta Principal",
    @ColumnInfo(defaultValue = "'PAID'") val paymentStatus: String = "PAID"
) {
    val taxCents: Long get() = grossAmountCents.percentOf(taxPercent)
    val commissionCents: Long get() = grossAmountCents.percentOf(commissionPercent)
    val contributionMarginCents: Long get() = grossAmountCents - taxCents - costAmountCents - commissionCents
}

@Entity(tableName = "seller_profiles")
data class SellerProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val monthlyGoalCents: Long,
    val commissionPercent: Double
)

@Entity(tableName = "fixed_expenses")
data class FixedExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val description: String,
    val category: String,
    val amountCents: Long,
    val dateMillis: Long
)

data class BusinessSettings(
    val businessName: String = "Minha Empresa",
    val initialBalanceCents: Long = 0L,
    val minCashAlertCents: Long = 500_000L,
    val taxRatePercent: Double = 6.0
)
data class DailyCashPoint(
    val dayOffset: Int, val dateMillis: Long, val expectedInflowCents: Long,
    val expectedOutflowCents: Long, val projectedBalanceCents: Long, val isLowCash: Boolean
)
data class DreSummary(
    val grossRevenueCents: Long = 0L, val salesTaxesCents: Long = 0L,
    val netRevenueCents: Long = 0L, val costOfGoodsCents: Long = 0L,
    val commissionsCents: Long = 0L, val contributionMarginCents: Long = 0L,
    val fixedExpensesCents: Long = 0L, val fixedExpensesByCategory: Map<String, Long> = emptyMap(),
    val operatingProfitEbitdaCents: Long = 0L, val netProfitCents: Long = 0L
) {
    val contributionMarginPercent: Double get() = if (grossRevenueCents > 0L)
        BigDecimal.valueOf(contributionMarginCents).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(grossRevenueCents), 4, RoundingMode.HALF_UP).toDouble() else 0.0
    val netProfitPercent: Double get() = if (grossRevenueCents > 0L)
        BigDecimal.valueOf(netProfitCents).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(grossRevenueCents), 4, RoundingMode.HALF_UP).toDouble() else 0.0
}
data class SellerRankingItem(
    val sellerId: Long?, val sellerName: String, val totalSalesCents: Long,
    val goalCents: Long, val commissionPercent: Double, val estimatedCommissionCents: Long,
    val salesCount: Int
) {
    val goalProgress: Float get() = if (goalCents > 0L)
        BigDecimal.valueOf(totalSalesCents).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(goalCents), 4, RoundingMode.HALF_UP).toFloat() / 100f else 0f
    val isGoalMet: Boolean get() = goalCents > 0L && totalSalesCents >= goalCents
}
object PrumoConstants {
    val cashAccounts = listOf("Conta Principal", "Conta Reserva", "Caixa Loja")
    val paymentMethods = listOf("Pix", "Cartão", "Boleto", "Dinheiro", "Transferência")
    val incomeCategories = listOf("Vendas à vista", "Recebimento cartão", "Serviços", "Aporte / Outros")
    val expenseCategories = listOf("Fornecedores", "Pró-labore / Retirada", "Impostos", "Despesa operacional", "Outros")
    val stockUnits = listOf("un", "kg", "cx", "pct", "lt")
    val stockCategories = listOf("Bebidas & Alimentos", "Embalagens", "Mercadorias", "Insumos", "Serviços / Kits")
    val fixedExpenseCategories = listOf("Aluguel", "Energia/Internet", "Folha de Pagamento", "Contador", "Tráfego Pago/Marketing", "Sistemas", "Outras")
}
