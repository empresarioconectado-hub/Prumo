package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PrumoRepository
import com.example.model.BusinessSettings
import com.example.model.CashEntry
import com.example.model.CashEntryStatus
import com.example.model.CashEntryType
import com.example.model.DailyCashPoint
import com.example.model.DreSummary
import com.example.model.FixedExpense
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.SellerRankingItem
import com.example.model.StockItem
import com.example.model.StockMovement
import com.example.model.StockMovementType
import com.example.model.StockStatus
import java.util.Calendar
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthYearFilter(
    val year: Int,
    val month: Int // 0-based (Calendar.JANUARY .. Calendar.DECEMBER)
) {
    fun shifted(deltaMonths: Int): MonthYearFilter {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        return MonthYearFilter(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }

    fun matches(millis: Long): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
    }

    fun formattedLabel(): String {
        val months = listOf(
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
        )
        return "${months.getOrElse(month) { "Mês" }}/$year"
    }

    companion object {
        fun current(): MonthYearFilter {
            val c = Calendar.getInstance()
            return MonthYearFilter(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
        }
    }
}

data class PrumoDashboardData(
    val settings: BusinessSettings = BusinessSettings(),
    val selectedMonth: MonthYearFilter = MonthYearFilter.current(),
    val allCashEntries: List<CashEntry> = emptyList(),
    val monthCashEntries: List<CashEntry> = emptyList(),
    val stockItems: List<StockItem> = emptyList(),\n    val stockMovements: List<StockMovement> = emptyList(),
    val monthSales: List<SaleRecord> = emptyList(),
    val sellers: List<SellerProfile> = emptyList(),
    val monthFixedExpenses: List<FixedExpense> = emptyList(),
    // Executive & Cash metrics
    val realizedBalanceCents: Long = 0L,          // Saldo Atual Realizado (inicial + pagos)
    val monthPredictedEndBalanceCents: Long = 0L, // Saldo Final Previsto do Mês (inicial + pagos + pendentes do mês)
    val monthPaidIncomeCents: Long = 0L,
    val monthPaidExpenseCents: Long = 0L,
    val monthPendingIncomeCents: Long = 0L,       // A Receber (Pendente)
    val monthPendingExpenseCents: Long = 0L,      // A Pagar (Pendente)
    val monthPartnerWithdrawalsCents: Long = 0L,  // Retiradas de Sócios (Pró-labore)
    val cashProjection30Days: List<DailyCashPoint> = emptyList(),
    val minProjected30dCents: Long = 0L,
    val isLowCashAlert: Boolean = false,
    // Stock metrics
    val totalStockCostCents: Long = 0L,
    val totalStockPotentialSaleCents: Long = 0L,
    val criticalOrReorderItems: List<StockItem> = emptyList(),
    val estimatedRestockCostCents: Long = 0L,
    // DRE & Ranking
    val dreMonth: DreSummary = DreSummary(),
    val sellerRanking: List<SellerRankingItem> = emptyList()
) {
    val hasAnyData: Boolean
        get() = allCashEntries.isNotEmpty() || stockItems.isNotEmpty() ||
            monthSales.isNotEmpty() || sellers.isNotEmpty() || monthFixedExpenses.isNotEmpty()
}

private data class RawCollections(
    val settings: BusinessSettings,
    val cash: List<CashEntry>,
    val stock: List<StockItem>,\n    val movements: List<StockMovement>,
    val sales: List<SaleRecord>,
    val sellers: List<SellerProfile>
)

class PrumoViewModel(
    private val repository: PrumoRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(MonthYearFilter.current())
    val selectedMonth: StateFlow<MonthYearFilter> = _selectedMonth.asStateFlow()

    private val _feedbackEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedbackEvents: SharedFlow<String> = _feedbackEvents.asSharedFlow()

    private val rawCollectionsFlow = combine(
        repository.settingsFlow,
        repository.cashEntriesFlow,
        repository.stockItemsFlow,
        repository.stockMovementsFlow,
        repository.salesFlow,
        repository.sellersFlow
    ) { settings, cash, stock, movements, sales, sellers ->
        RawCollections(settings, cash, stock, movements, sales, sellers)
    }

    val dashboardState: StateFlow<PrumoDashboardData> = combine(
        rawCollectionsFlow,
        repository.fixedExpensesFlow,
        _selectedMonth
    ) { raw, fixedExpenses, monthFilter ->
        buildDashboardData(raw, fixedExpenses, monthFilter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PrumoDashboardData()
    )

    fun shiftMonth(delta: Int) {
        _selectedMonth.value = _selectedMonth.value.shifted(delta)
    }

    private fun buildDashboardData(
        raw: RawCollections,
        fixedExpenses: List<FixedExpense>,
        monthFilter: MonthYearFilter
    ): PrumoDashboardData {
        val settings = raw.settings
        val allCash = raw.cash
        val monthCash = allCash.filter { monthFilter.matches(it.dateMillis) }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, monthFilter.year)
            set(Calendar.MONTH, monthFilter.month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val paidBeforeMonth = allCash.filter {
            it.dateMillis < monthStart && it.entryStatus == CashEntryStatus.PAID
        }
        val monthInitialBalance = settings.initialBalanceCents +
            paidBeforeMonth.filter { it.entryType == CashEntryType.INCOME }.sumOf { it.amountCents } -
            paidBeforeMonth.filter { it.entryType == CashEntryType.EXPENSE }.sumOf { it.amountCents }

        val allPaidIncome = allCash.filter { it.entryType == CashEntryType.INCOME && it.entryStatus == CashEntryStatus.PAID }
            .sumOf { it.amountCents }
        val allPaidExpense = allCash.filter { it.entryType == CashEntryType.EXPENSE && it.entryStatus == CashEntryStatus.PAID }
            .sumOf { it.amountCents }
        val realizedBalance = settings.initialBalanceCents + allPaidIncome - allPaidExpense

        val monthPaidIncome = monthCash.filter { it.entryType == CashEntryType.INCOME && it.entryStatus == CashEntryStatus.PAID }
            .sumOf { it.amountCents }
        val monthPaidExpense = monthCash.filter { it.entryType == CashEntryType.EXPENSE && it.entryStatus == CashEntryStatus.PAID }
            .sumOf { it.amountCents }
        val monthPendingIncome = monthCash.filter { it.entryType == CashEntryType.INCOME && it.entryStatus == CashEntryStatus.PENDING }
            .sumOf { it.amountCents }
        val monthPendingExpense = monthCash.filter { it.entryType == CashEntryType.EXPENSE && it.entryStatus == CashEntryStatus.PENDING }
            .sumOf { it.amountCents }
        val monthPartnerWithdrawals = monthCash.filter { it.isPartnerWithdrawal }
            .sumOf { it.amountCents }

        val monthPredictedEndBalance = monthInitialBalance + monthPaidIncome + monthPendingIncome - monthPaidExpense - monthPendingExpense

        // 30-day day-by-day simulator/projection from today
        val dayMs = 24L * 60L * 60L * 1000L
        val todayStart = startOfDayMillis(System.currentTimeMillis())
        val pendingAll = allCash.filter { it.entryStatus == CashEntryStatus.PENDING }

        var runningBalance = realizedBalance
        val projection30 = (0..29).map { offset ->
            val dayStart = todayStart + offset * dayMs
            val dayEnd = dayStart + dayMs - 1L
            val dayItems = pendingAll.filter { it.dateMillis in dayStart..dayEnd }
            val dayIn = dayItems.filter { it.entryType == CashEntryType.INCOME }.sumOf { it.amountCents }
            val dayOut = dayItems.filter { it.entryType == CashEntryType.EXPENSE }.sumOf { it.amountCents }
            // Include overdue pending items on day 0
            val overdueDelta = if (offset == 0) {
                val overdue = pendingAll.filter { it.dateMillis < todayStart }
                val overdueIn = overdue.filter { it.entryType == CashEntryType.INCOME }.sumOf { it.amountCents }
                val overdueOut = overdue.filter { it.entryType == CashEntryType.EXPENSE }.sumOf { it.amountCents }
                overdueIn - overdueOut
            } else 0L

            runningBalance += (dayIn - dayOut + overdueDelta)
            DailyCashPoint(
                dayOffset = offset,
                dateMillis = dayStart,
                expectedInflowCents = dayIn,
                expectedOutflowCents = dayOut,
                projectedBalanceCents = runningBalance,
                isLowCash = runningBalance < settings.minCashAlertCents
            )
        }

        val minProj = projection30.minOfOrNull { it.projectedBalanceCents } ?: realizedBalance
        val isLowCashAlert = realizedBalance < settings.minCashAlertCents || minProj < settings.minCashAlertCents

        // Stock calculations (critical first: OUT_OF_STOCK -> REORDER -> NORMAL)
        val sortedStock = raw.stock.sortedWith(
            compareBy<StockItem> {
                when (it.status) {
                    StockStatus.OUT_OF_STOCK -> 0
                    StockStatus.REORDER -> 1
                    StockStatus.NORMAL -> 2
                }
            }.thenBy { it.name.lowercase() }
        )
        val criticalOrReorder = sortedStock.filter { it.status != StockStatus.NORMAL }
        val totalStockCost = sortedStock.sumOf { it.stockCostValueCents }
        val totalStockPotentialSale = sortedStock.sumOf { it.potentialSaleValueCents }
        val restockCost = criticalOrReorder.sumOf { it.estimatedPurchaseCostCents }

        // Sales, Fixed Expenses & DRE for selected month
        val monthSales = raw.sales.filter { monthFilter.matches(it.saleDateMillis) }
        val monthFixed = fixedExpenses.filter { monthFilter.matches(it.dateMillis) }

        val grossRevenue = monthSales.sumOf { it.grossAmountCents }
        val salesTaxes = monthSales.sumOf { it.taxCents }
        val netRevenue = grossRevenue - salesTaxes
        val cmv = monthSales.sumOf { it.costAmountCents }
        val commissions = monthSales.sumOf { it.commissionCents }
        val contributionMargin = netRevenue - cmv - commissions

        val fixedTotal = monthFixed.sumOf { it.amountCents }
        val fixedByCategory = monthFixed.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amountCents } }
        val ebitda = contributionMargin - fixedTotal

        val dreMonth = DreSummary(
            grossRevenueCents = grossRevenue,
            salesTaxesCents = salesTaxes,
            netRevenueCents = netRevenue,
            costOfGoodsCents = cmv,
            commissionsCents = commissions,
            contributionMarginCents = contributionMargin,
            fixedExpensesCents = fixedTotal,
            fixedExpensesByCategory = fixedByCategory,
            operatingProfitEbitdaCents = ebitda,
            netProfitCents = ebitda
        )

        // Seller ranking
        val salesBySeller = monthSales.groupBy { it.sellerName.trim() }
        val allSellerNames = (raw.sellers.map { it.name.trim() } + salesBySeller.keys)
            .distinct()
            .filter { it.isNotBlank() }

        val ranking = allSellerNames.map { sellerName ->
            val profile = raw.sellers.firstOrNull { it.name.equals(sellerName, ignoreCase = true) }
            val sellerSales = salesBySeller[sellerName].orEmpty()
            val totalSold = sellerSales.sumOf { it.grossAmountCents }
            val goal = profile?.monthlyGoalCents ?: 1_000_000L
            val commRate = profile?.commissionPercent ?: sellerSales.firstOrNull()?.commissionPercent ?: 5.0
            val estCommission = if (profile != null) {
                BigDecimal.valueOf(totalSold).multiply(BigDecimal.valueOf(profile.commissionPercent))\n                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValueExact()
            } else {
                sellerSales.sumOf { it.commissionCents }
            }
            SellerRankingItem(
                sellerId = profile?.id,
                sellerName = sellerName,
                totalSalesCents = totalSold,
                goalCents = goal,
                commissionPercent = commRate,
                estimatedCommissionCents = estCommission,
                salesCount = sellerSales.size
            )
        }.sortedByDescending { it.totalSalesCents }

        return PrumoDashboardData(
            settings = settings,
            selectedMonth = monthFilter,
            allCashEntries = allCash,
            monthCashEntries = monthCash,
            stockItems = sortedStock,\n            stockMovements = raw.movements,
            monthSales = monthSales,
            sellers = raw.sellers,
            monthFixedExpenses = monthFixed,
            realizedBalanceCents = realizedBalance,
            monthPredictedEndBalanceCents = monthPredictedEndBalance,\n            monthInitialBalanceCents = monthInitialBalance,
            monthPaidIncomeCents = monthPaidIncome,
            monthPaidExpenseCents = monthPaidExpense,
            monthPendingIncomeCents = monthPendingIncome,
            monthPendingExpenseCents = monthPendingExpense,
            monthPartnerWithdrawalsCents = monthPartnerWithdrawals,
            cashProjection30Days = projection30,
            minProjected30dCents = minProj,
            isLowCashAlert = isLowCashAlert,
            totalStockCostCents = totalStockCost,
            totalStockPotentialSaleCents = totalStockPotentialSale,
            criticalOrReorderItems = criticalOrReorder,
            estimatedRestockCostCents = restockCost,
            dreMonth = dreMonth,
            sellerRanking = ranking
        )
    }

    private fun startOfDayMillis(millis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    fun saveSettings(businessName: String, initialBalanceCents: Long, minCashAlertCents: Long, taxRatePercent: Double) {
        viewModelScope.launch {
            repository.updateSettings(businessName, initialBalanceCents, minCashAlertCents, taxRatePercent)
            _feedbackEvents.tryEmit("Parâmetros atualizados.")
        }
    }

    fun saveCashEntry(entry: CashEntry) {
        viewModelScope.launch {
            repository.saveCashEntry(entry)
            _feedbackEvents.tryEmit(if (entry.id == 0L) "Lançamento salvo no caixa." else "Lançamento atualizado.")
        }
    }

    fun markAsPaid(entry: CashEntry) {
        viewModelScope.launch {
            val next = if (entry.entryStatus == CashEntryStatus.PENDING) CashEntryStatus.PAID else CashEntryStatus.PENDING
            repository.markCashEntryStatus(entry.id, next)
            _feedbackEvents.tryEmit("Status alterado para ${next.label}.")
        }
    }

    fun deleteCashEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteCashEntry(id)
            _feedbackEvents.tryEmit("Lançamento removido.")
        }
    }

    fun saveStockItem(item: StockItem) {
        viewModelScope.launch {
            repository.saveStockItem(item)
            _feedbackEvents.tryEmit(if (item.id == 0L) "Produto cadastrado no estoque." else "Produto atualizado.")
        }
    }

    fun applyStockMovement(item: StockItem, movementType: StockMovementType, units: Long) {
        viewModelScope.launch {
            val ok = repository.applyStockMovement(item, movementType, units)
            if (ok) {
                _feedbackEvents.tryEmit("Estoque atualizado: ${item.sku}.")
            } else {
                _feedbackEvents.tryEmit("Saldo insuficiente para a saída em ${item.sku}.")
            }
        }
    }

    fun deleteStockItem(id: Long) {
        viewModelScope.launch {
            repository.deleteStockItem(id)
            _feedbackEvents.tryEmit("Produto removido.")
        }
    }

    fun saveSale(sale: SaleRecord) {
        viewModelScope.launch {
            repository.saveSale(sale)
            _feedbackEvents.tryEmit(if (sale.id == 0L) "Venda registrada na DRE." else "Venda atualizada.")
        }
    }

    fun deleteSale(id: Long) {
        viewModelScope.launch {
            repository.deleteSale(id)
            _feedbackEvents.tryEmit("Venda removida.")
        }
    }

    fun saveSeller(seller: SellerProfile) {
        viewModelScope.launch {
            repository.saveSeller(seller)
            _feedbackEvents.tryEmit(if (seller.id == 0L) "Vendedor cadastrado." else "Meta atualizada.")
        }
    }

    fun deleteSeller(id: Long) {
        viewModelScope.launch {
            repository.deleteSeller(id)
            _feedbackEvents.tryEmit("Vendedor removido.")
        }
    }

    fun saveFixedExpense(expense: FixedExpense) {
        viewModelScope.launch {
            repository.saveFixedExpense(expense)
            _feedbackEvents.tryEmit(if (expense.id == 0L) "Despesa fixa registrada." else "Despesa fixa atualizada.")
        }
    }

    fun deleteFixedExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteFixedExpense(id)
            _feedbackEvents.tryEmit("Despesa fixa removida.")
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
            _feedbackEvents.tryEmit("Dados de exemplo carregados!")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _feedbackEvents.tryEmit("Dados limpos. Pronto para começar do zero!")
        }
    }
}
