package com.example.data

import com.example.model.BusinessSettings
import com.example.model.CashEntry
import com.example.model.CashEntryStatus
import com.example.model.CashEntryType
import com.example.model.FixedExpense
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem
import com.example.model.StockMovementType
import com.example.model.StockMovement
import java.util.Calendar
import kotlinx.coroutines.flow.Flow

class PrumoRepository(
    private val dao: PrumoDao,
    private val settingsStore: SettingsDataStore
) {
    val settingsFlow: Flow<BusinessSettings> = settingsStore.settingsFlow
    val cashEntriesFlow: Flow<List<CashEntry>> = dao.observeCashEntries()
    val stockItemsFlow: Flow<List<StockItem>> = dao.observeStockItems()
    val stockMovementsFlow: Flow<List<StockMovement>> = dao.observeStockMovements()
    val salesFlow: Flow<List<SaleRecord>> = dao.observeSales()
    val sellersFlow: Flow<List<SellerProfile>> = dao.observeSellers()
    val fixedExpensesFlow: Flow<List<FixedExpense>> = dao.observeFixedExpenses()

    private val backupManager = PrumoBackupManager(dao, settingsStore)

    suspend fun exportBackupJson(password: String): String = backupManager.exportEncryptedJson(password)

    suspend fun restoreBackupJson(json: String, password: String) = backupManager.restoreBackup(json, password)

    suspend fun updateSettings(
        businessName: String,
        initialBalanceCents: Long,
        minCashAlertCents: Long,
        taxRatePercent: Double
    ) {
        PrumoValidation.settings(BusinessSettings(businessName, initialBalanceCents, minCashAlertCents, taxRatePercent))
        settingsStore.updateSettings(businessName, initialBalanceCents, minCashAlertCents, taxRatePercent)
    }

    suspend fun saveCashEntry(entry: CashEntry): Boolean {
        if (entry.saleId != null) return false
        PrumoValidation.cashEntry(entry)
        if (entry.id == 0L) dao.insertCashEntry(entry) else {
            require(dao.cashEntryById(entry.id) != null) { "Lançamento não encontrado para edição." }
            dao.updateCashEntry(entry)
        }
        return true
    }

    suspend fun markCashEntryStatus(id: Long, status: CashEntryStatus) {
        dao.updateCashEntryStatusAtomic(id, status.name)
    }

    suspend fun deleteCashEntry(id: Long): Boolean {
        val entry = dao.cashEntryById(id)
        if (entry?.saleId != null) return false
        dao.deleteCashEntryById(id)
        return true
    }

    suspend fun saveStockItem(item: StockItem) {
        PrumoValidation.stockItem(item)
        if (item.id == 0L) {
            val openingMovement = if (item.quantity > 0L) StockMovement(
                stockItemId = 0L, sku = item.sku, productName = item.name,
                movementType = StockMovementType.PURCHASE.name, quantity = item.quantity,
                unitCostCents = item.unitCostCents, referenceNote = "Saldo inicial do cadastro",
                dateMillis = System.currentTimeMillis()
            ) else null
            dao.insertStockItemWithOpeningMovement(item, openingMovement)
        } else {
            val current = dao.stockItemById(item.id)
                ?: error("Produto não encontrado para edição.")
            val movement = if (current.quantity != item.quantity) StockMovement(
                stockItemId = item.id, sku = item.sku, productName = item.name,
                movementType = StockMovementType.ADJUSTMENT.name,
                quantity = kotlin.math.abs(item.quantity - current.quantity),
                unitCostCents = item.unitCostCents, referenceNote = "Ajuste manual de saldo",
                dateMillis = System.currentTimeMillis()
            ) else null
            dao.updateStockItemWithMovement(item, movement)
        }
    }
    suspend fun applyStockMovement(item: StockItem, movementType: StockMovementType, units: Long): Boolean {
        if (units <= 0L) return false
        return dao.applyStockMovementAtomic(item.id, movementType, units)
    }

    suspend fun deleteStockItem(id: Long): Boolean {
        if (dao.countSalesForStockItem(id) > 0) return false
        dao.deleteStockItemById(id)
        return true
    }

    suspend fun saveSale(sale: SaleRecord) {
        PrumoValidation.sale(sale)
        dao.saveSaleAtomic(sale)
    }

    suspend fun deleteSale(id: Long) {
        dao.deleteSaleAtomic(id)
    }

    suspend fun saveSeller(seller: SellerProfile) {
        PrumoValidation.seller(seller)
        if (seller.id == 0L) {
            dao.insertSeller(seller)
        } else {
            dao.updateSeller(seller)
        }
    }

    suspend fun deleteSeller(id: Long) {
        dao.deleteSellerById(id)
    }

    suspend fun saveFixedExpense(expense: FixedExpense) {
        PrumoValidation.fixedExpense(expense)
        if (expense.id == 0L) {
            dao.insertFixedExpense(expense)
        } else {
            dao.updateFixedExpense(expense)
        }
    }

    suspend fun deleteFixedExpense(id: Long) {
        dao.deleteFixedExpenseById(id)
    }

    suspend fun clearAllData() {
        // Room changes are committed together; settings are reset only after the DB transaction succeeds.
        dao.clearAllDataAtomic()
        settingsStore.resetDefaults()
    }

    suspend fun loadSampleData() {
        clearAllData()

        settingsStore.updateSettings(
            businessName = "Empório Prumo Ltda",
            initialBalanceCents = 850_000L, // R$ 8.500,00
            minCashAlertCents = 500_000L,   // R$ 5.000,00
            taxRatePercent = 6.0
        )

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayNoon = cal.timeInMillis

        dao.insertCashEntries(
            listOf(
                CashEntry(
                    description = "Vendas balcão (semana 1)",
                    amountCents = 640_000L,
                    type = CashEntryType.INCOME.name,
                    category = "Vendas à vista",
                    account = "Caixa Loja",
                    paymentMethod = "Pix",
                    status = CashEntryStatus.PAID.name,
                    dateMillis = todayNoon - 4 * dayMs
                ),
                CashEntry(
                    description = "Repasse maquininha cartão",
                    amountCents = 920_000L,
                    type = CashEntryType.INCOME.name,
                    category = "Recebimento cartão",
                    account = "Conta Principal",
                    paymentMethod = "Cartão",
                    status = CashEntryStatus.PAID.name,
                    dateMillis = todayNoon - 2 * dayMs
                ),
                CashEntry(
                    description = "Compra fornecedor Café Sul",
                    amountCents = 480_000L,
                    type = CashEntryType.EXPENSE.name,
                    category = "Fornecedores",
                    account = "Conta Principal",
                    paymentMethod = "Boleto",
                    status = CashEntryStatus.PAID.name,
                    dateMillis = todayNoon - 3 * dayMs
                ),
                CashEntry(
                    description = "Pró-labore sócio administrador",
                    amountCents = 450_000L,
                    type = CashEntryType.EXPENSE.name,
                    category = "Pró-labore / Retirada",
                    account = "Conta Principal",
                    paymentMethod = "Transferência",
                    status = CashEntryStatus.PAID.name,
                    dateMillis = todayNoon - 1 * dayMs
                ),
                CashEntry(
                    description = "Aporte reserva de emergência",
                    amountCents = 250_000L,
                    type = CashEntryType.INCOME.name,
                    category = "Aporte / Outros",
                    account = "Conta Reserva",
                    paymentMethod = "Pix",
                    status = CashEntryStatus.PAID.name,
                    dateMillis = todayNoon - 5 * dayMs
                ),
                CashEntry(
                    description = "Faturamento B2B Padaria Central",
                    amountCents = 490_000L,
                    type = CashEntryType.INCOME.name,
                    category = "Serviços",
                    account = "Conta Principal",
                    paymentMethod = "Boleto",
                    status = CashEntryStatus.PENDING.name,
                    dateMillis = todayNoon + 5 * dayMs
                ),
                CashEntry(
                    description = "Boleto fornecedor Embalagens",
                    amountCents = 1_180_000L,
                    type = CashEntryType.EXPENSE.name,
                    category = "Fornecedores",
                    account = "Conta Principal",
                    paymentMethod = "Boleto",
                    status = CashEntryStatus.PENDING.name,
                    dateMillis = todayNoon + 10 * dayMs
                ),
                CashEntry(
                    description = "Parcela DAS Simples Nacional",
                    amountCents = 230_000L,
                    type = CashEntryType.EXPENSE.name,
                    category = "Impostos",
                    account = "Conta Principal",
                    paymentMethod = "Pix",
                    status = CashEntryStatus.PENDING.name,
                    dateMillis = todayNoon + 14 * dayMs
                ),
                CashEntry(
                    description = "Recebível cartão parcelado",
                    amountCents = 780_000L,
                    type = CashEntryType.INCOME.name,
                    category = "Recebimento cartão",
                    account = "Conta Principal",
                    paymentMethod = "Cartão",
                    status = CashEntryStatus.PENDING.name,
                    dateMillis = todayNoon + 18 * dayMs
                )
            )
        )

        dao.insertStockItems(
            listOf(
                StockItem(
                    sku = "FIL-02",
                    name = "Filtro de Papel 103",
                    category = "Embalagens",
                    unit = "cx",
                    quantity = 0L,
                    minStock = 8L,
                    leadTimeDays = 4L,
                    avgDailySales = 2.0,
                    unitCostCents = 1400L,
                    unitPriceCents = 2800L,
                    soldUnitsCount = 46L,
                    lossUnitsCount = 2L
                ),
                StockItem(
                    sku = "CAF-05",
                    name = "Café Especial Torrado 500g",
                    category = "Bebidas & Alimentos",
                    unit = "pct",
                    quantity = 12L,
                    minStock = 10L,
                    leadTimeDays = 3L,
                    avgDailySales = 3.0,
                    unitCostCents = 1950L,
                    unitPriceCents = 4200L,
                    soldUnitsCount = 72L,
                    lossUnitsCount = 1L
                ),
                StockItem(
                    sku = "CAN-12",
                    name = "Caneca Cerâmica Prumo",
                    category = "Mercadorias",
                    unit = "un",
                    quantity = 48L,
                    minStock = 12L,
                    leadTimeDays = 5L,
                    avgDailySales = 1.5,
                    unitCostCents = 2200L,
                    unitPriceCents = 5400L,
                    soldUnitsCount = 35L,
                    lossUnitsCount = 0L
                ),
                StockItem(
                    sku = "XAR-09",
                    name = "Xarope Baunilha Barista",
                    category = "Insumos",
                    unit = "lt",
                    quantity = 7L,
                    minStock = 6L,
                    leadTimeDays = 4L,
                    avgDailySales = 1.2,
                    unitCostCents = 3400L,
                    unitPriceCents = 6900L,
                    soldUnitsCount = 28L,
                    lossUnitsCount = 1L
                )
            )
        )

        dao.insertSellers(
            listOf(
                SellerProfile(name = "Ana Paula", monthlyGoalCents = 1_500_000L, commissionPercent = 5.0),
                SellerProfile(name = "Carlos Eduardo", monthlyGoalCents = 1_200_000L, commissionPercent = 4.0),
                SellerProfile(name = "Mariana Costa", monthlyGoalCents = 1_000_000L, commissionPercent = 5.0)
            )
        )

        dao.insertSales(
            listOf(
                SaleRecord(
                    description = "Kit 40x Café Especial 500g",
                    clientName = "Padaria Central",
                    sellerName = "Ana Paula",
                    quantity = 40L,
                    grossAmountCents = 1_680_000L,
                    costAmountCents = 780_000L,
                    taxPercent = 6.0,
                    commissionPercent = 5.0,
                    saleDateMillis = todayNoon - 3 * dayMs
                ),
                SaleRecord(
                    description = "15x Caneca Cerâmica Prumo",
                    clientName = "Escritório Vértice",
                    sellerName = "Carlos Eduardo",
                    quantity = 15L,
                    grossAmountCents = 810_000L,
                    costAmountCents = 330_000L,
                    taxPercent = 6.0,
                    commissionPercent = 4.0,
                    saleDateMillis = todayNoon - 2 * dayMs
                ),
                SaleRecord(
                    description = "Assinatura Mensal Café Corporativo",
                    clientName = "TechSul Coworking",
                    sellerName = "Mariana Costa",
                    quantity = 10L,
                    grossAmountCents = 1_120_000L,
                    costAmountCents = 460_000L,
                    taxPercent = 6.0,
                    commissionPercent = 5.0,
                    saleDateMillis = todayNoon - 1 * dayMs
                )
            )
        )

        dao.insertFixedExpenses(
            listOf(
                FixedExpense(description = "Aluguel ponto comercial", category = "Aluguel", amountCents = 320_000L, dateMillis = todayNoon),
                FixedExpense(description = "Energia elétrica e fibra óptica", category = "Energia/Internet", amountCents = 68_000L, dateMillis = todayNoon),
                FixedExpense(description = "Folha de pagamento + encargos", category = "Folha de Pagamento", amountCents = 480_000L, dateMillis = todayNoon),
                FixedExpense(description = "Honorários contabilidade", category = "Contador", amountCents = 45_000L, dateMillis = todayNoon),
                FixedExpense(description = "Campanhas Meta / Google Ads", category = "Tráfego Pago/Marketing", amountCents = 95_000L, dateMillis = todayNoon),
                FixedExpense(description = "ERP / Emissor fiscal", category = "Sistemas", amountCents = 24_000L, dateMillis = todayNoon)
            )
        )
    }
}
