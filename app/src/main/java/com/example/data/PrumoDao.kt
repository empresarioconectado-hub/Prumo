package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.model.*
import kotlinx.coroutines.flow.Flow

data class PrumoDataSnapshot(
    val cashEntries: List<CashEntry>,
    val stockItems: List<StockItem>,
    val stockMovements: List<StockMovement>,
    val sales: List<SaleRecord>,
    val sellers: List<SellerProfile>,
    val fixedExpenses: List<FixedExpense>
)

@Dao
interface PrumoDao {
    @Query("SELECT * FROM cash_entries ORDER BY dateMillis DESC, id DESC") fun observeCashEntries(): Flow<List<CashEntry>>
    @Query("SELECT * FROM cash_entries") suspend fun allCashEntries(): List<CashEntry>

    @Transaction
    suspend fun readAllDataSnapshot(): PrumoDataSnapshot = PrumoDataSnapshot(
        cashEntries = allCashEntries(),
        stockItems = allStockItems(),
        stockMovements = allStockMovements(),
        sales = allSales(),
        sellers = allSellers(),
        fixedExpenses = allFixedExpenses()
    )
    @Query("SELECT * FROM cash_entries WHERE id = :id LIMIT 1") suspend fun cashEntryById(id: Long): CashEntry?
    @Query("SELECT * FROM cash_entries WHERE saleId = :saleId LIMIT 1") suspend fun cashEntryForSale(saleId: Long): CashEntry?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCashEntry(entry: CashEntry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCashEntries(entries: List<CashEntry>)
    @Update suspend fun updateCashEntry(entry: CashEntry)
    @Query("UPDATE cash_entries SET status = :status WHERE id = :id") suspend fun updateCashEntryStatus(id: Long, status: String)

    @Transaction
    suspend fun updateCashEntryStatusAtomic(id: Long, status: String) {
        val entry = cashEntryById(id) ?: return
        updateCashEntryStatus(id, status)
        entry.saleId?.let { updateSalePaymentStatus(it, status) }
    }
    @Query("DELETE FROM cash_entries WHERE id = :id") suspend fun deleteCashEntryById(id: Long)
    @Query("DELETE FROM cash_entries WHERE saleId = :saleId") suspend fun deleteCashEntriesForSale(saleId: Long)
    @Query("DELETE FROM cash_entries") suspend fun clearCashEntries()

    @Query("SELECT * FROM stock_items ORDER BY name ASC") fun observeStockItems(): Flow<List<StockItem>>
    @Query("SELECT * FROM stock_items") suspend fun allStockItems(): List<StockItem>
    @Query("SELECT * FROM stock_items WHERE id = :id LIMIT 1") suspend fun stockItemById(id: Long): StockItem?
    @Query("SELECT COUNT(*) FROM stock_items WHERE sku = :sku COLLATE NOCASE AND id != :excludeId") suspend fun countStockItemsWithSku(sku: String, excludeId: Long): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStockItem(item: StockItem): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStockItems(items: List<StockItem>)
    @Update suspend fun updateStockItem(item: StockItem)
    @Query("DELETE FROM stock_items WHERE id = :id") suspend fun deleteStockItemById(id: Long)
    @Query("DELETE FROM stock_items") suspend fun clearStockItems()

    @Query("SELECT * FROM stock_movements ORDER BY dateMillis DESC, id DESC") fun observeStockMovements(): Flow<List<StockMovement>>
    @Query("SELECT * FROM stock_movements") suspend fun allStockMovements(): List<StockMovement>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStockMovement(movement: StockMovement): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStockMovements(movements: List<StockMovement>)

    @Transaction
    suspend fun insertStockItemWithOpeningMovement(item: StockItem, movement: StockMovement?): Long {
        val id = insertStockItem(item)
        if (movement != null) insertStockMovement(movement.copy(stockItemId = id))
        return id
    }

    @Transaction
    suspend fun updateStockItemWithMovement(item: StockItem, movement: StockMovement?) {
        updateStockItem(item)
        if (movement != null) insertStockMovement(movement)
    }
    @Query("DELETE FROM stock_movements") suspend fun clearStockMovements()

    @Query("SELECT * FROM sale_records ORDER BY saleDateMillis DESC, id DESC") fun observeSales(): Flow<List<SaleRecord>>
    @Query("SELECT * FROM sale_records") suspend fun allSales(): List<SaleRecord>
    @Query("SELECT * FROM sale_records WHERE id = :id LIMIT 1") suspend fun saleById(id: Long): SaleRecord?
    @Query("SELECT COUNT(*) FROM sale_records WHERE stockItemId = :stockItemId") suspend fun countSalesForStockItem(stockItemId: Long): Int
    @Query("UPDATE sale_records SET paymentStatus = :status WHERE id = :saleId") suspend fun updateSalePaymentStatus(saleId: Long, status: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSale(sale: SaleRecord): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSales(sales: List<SaleRecord>)
    @Update suspend fun updateSale(sale: SaleRecord)
    @Query("DELETE FROM sale_records WHERE id = :id") suspend fun deleteSaleById(id: Long)
    @Query("DELETE FROM sale_records") suspend fun clearSales()

    @Transaction
    suspend fun saveSaleAtomic(sale: SaleRecord) {
        require(sale.quantity > 0L) { "A quantidade da venda deve ser maior que zero." }
        require(sale.grossAmountCents > 0L && sale.costAmountCents >= 0L) { "Os valores da venda são inválidos." }
        require(sale.taxPercent in 0.0..100.0 && sale.commissionPercent in 0.0..100.0) { "Percentuais inválidos." }
        require(sale.paymentStatus == CashEntryStatus.PAID.name || sale.paymentStatus == CashEntryStatus.PENDING.name) {
            "Status de recebimento inválido."
        }
        val previous = if (sale.id != 0L) saleById(sale.id) else null
        if (sale.id != 0L) require(previous != null) { "Venda não encontrada para edição." }
        if (previous != null) reverseSaleStock(previous)
        val id = if (sale.id == 0L) insertSale(sale) else { updateSale(sale); sale.id }
        val saved = sale.copy(id = id)
        if (saved.stockItemId != null) {
            val item = stockItemById(saved.stockItemId) ?: error("Produto vinculado não encontrado.")
            require(saved.quantity > 0L) { "A quantidade da venda deve ser maior que zero." }
            require(item.quantity >= saved.quantity) { "Estoque insuficiente para ${item.name}. Disponível: ${item.quantity}." }
            updateStockItem(item.copy(quantity = item.quantity - saved.quantity, soldUnitsCount = Math.addExact(item.soldUnitsCount, saved.quantity)))
            insertStockMovement(StockMovement(
                stockItemId = item.id, sku = item.sku, productName = item.name,
                movementType = StockMovementType.SALE.name, quantity = saved.quantity,
                unitCostCents = item.unitCostCents, referenceNote = "Venda #${id}",
                dateMillis = saved.saleDateMillis
            ))
        }
        deleteCashEntriesForSale(id)
        insertCashEntry(CashEntry(
            description = "Venda #${id} - ${saved.description}",
            amountCents = saved.grossAmountCents, type = CashEntryType.INCOME.name,
            category = if (saved.paymentStatus == CashEntryStatus.PAID.name) "Vendas à vista" else "Vendas a prazo", account = saved.account,
            paymentMethod = saved.paymentMethod, status = saved.paymentStatus,
            dateMillis = saved.saleDateMillis, saleId = id
        ))
    }

    @Transaction
    suspend fun deleteSaleAtomic(id: Long) {
        val sale = saleById(id) ?: return
        reverseSaleStock(sale)
        deleteCashEntriesForSale(id)
        deleteSaleById(id)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSellers(sellers: List<SellerProfile>)
    @Query("SELECT * FROM seller_profiles ORDER BY name ASC") fun observeSellers(): Flow<List<SellerProfile>>
    @Query("SELECT * FROM seller_profiles") suspend fun allSellers(): List<SellerProfile>
    @Query("SELECT * FROM seller_profiles WHERE id = :id LIMIT 1") suspend fun sellerById(id: Long): SellerProfile?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSeller(seller: SellerProfile): Long
    @Update suspend fun updateSeller(seller: SellerProfile)
    @Query("DELETE FROM seller_profiles WHERE id = :id") suspend fun deleteSellerById(id: Long)
    @Query("DELETE FROM seller_profiles") suspend fun clearSellers()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFixedExpenses(expenses: List<FixedExpense>)
    @Query("SELECT * FROM fixed_expenses ORDER BY dateMillis DESC, id DESC") fun observeFixedExpenses(): Flow<List<FixedExpense>>
    @Query("SELECT * FROM fixed_expenses") suspend fun allFixedExpenses(): List<FixedExpense>
    @Query("SELECT * FROM fixed_expenses WHERE id = :id LIMIT 1") suspend fun fixedExpenseById(id: Long): FixedExpense?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFixedExpense(expense: FixedExpense): Long
    @Update suspend fun updateFixedExpense(expense: FixedExpense)
    @Query("DELETE FROM fixed_expenses WHERE id = :id") suspend fun deleteFixedExpenseById(id: Long)
    @Query("DELETE FROM fixed_expenses") suspend fun clearFixedExpenses()

    @Transaction
    suspend fun clearAllDataAtomic() {
        clearCashEntries()
        clearStockItems()
        clearStockMovements()
        clearSales()
        clearSellers()
        clearFixedExpenses()
    }

    @Transaction
    suspend fun applyStockMovementAtomic(itemId: Long, movementType: StockMovementType, units: Long): Boolean {
        if (movementType == StockMovementType.ADJUSTMENT) {
            require(units in 0L..PrumoValidation.MAX_STOCK_UNITS) { "O novo saldo está fora do limite permitido." }
        } else {
            require(units in 1L..PrumoValidation.MAX_STOCK_UNITS) { "A quantidade deve ser maior que zero." }
        }
        val current = stockItemById(itemId) ?: return false
        val updated = when (movementType) {
            StockMovementType.PURCHASE -> current.copy(quantity = Math.addExact(current.quantity, units))
            StockMovementType.SALE -> {
                require(current.quantity >= units) { "Estoque insuficiente para ${current.name}." }
                current.copy(quantity = current.quantity - units, soldUnitsCount = Math.addExact(current.soldUnitsCount, units))
            }
            StockMovementType.LOSS -> {
                require(current.quantity >= units) { "Estoque insuficiente para registrar a perda de ${current.name}." }
                current.copy(quantity = current.quantity - units, lossUnitsCount = Math.addExact(current.lossUnitsCount, units))
            }
            StockMovementType.ADJUSTMENT -> current.copy(quantity = units)
        }
        PrumoValidation.stockItem(updated)
        val movementUnits = if (movementType == StockMovementType.ADJUSTMENT) {
            kotlin.math.abs(units - current.quantity)
        } else units
        if (movementUnits == 0L) return true
        updateStockItem(updated)
        insertStockMovement(StockMovement(
            stockItemId = current.id, sku = current.sku, productName = current.name,
            movementType = movementType.name, quantity = movementUnits, unitCostCents = current.unitCostCents,
            referenceNote = when (movementType) {
                StockMovementType.PURCHASE -> "Compra / reposição"
                StockMovementType.SALE -> "Saída manual de estoque"
                StockMovementType.LOSS -> "Perda / avaria"
                StockMovementType.ADJUSTMENT -> "Ajuste manual de saldo: ${current.quantity} → ${units}"
            },
            dateMillis = System.currentTimeMillis()
        ))
        return true
    }

    @Transaction
    suspend fun replaceAllData(
        cash: List<CashEntry>, stock: List<StockItem>, sales: List<SaleRecord>,
        sellers: List<SellerProfile>, expenses: List<FixedExpense>, movements: List<StockMovement>
    ) {
        clearCashEntries(); clearStockItems(); clearSales(); clearSellers(); clearFixedExpenses(); clearStockMovements()
        if (cash.isNotEmpty()) insertCashEntries(cash)
        if (stock.isNotEmpty()) insertStockItems(stock)
        if (sales.isNotEmpty()) insertSales(sales)
        if (sellers.isNotEmpty()) insertSellers(sellers)
        if (expenses.isNotEmpty()) insertFixedExpenses(expenses)
        if (movements.isNotEmpty()) insertStockMovements(movements)
    }

}

private suspend fun PrumoDao.reverseSaleStock(sale: SaleRecord) {
    val stockId = sale.stockItemId ?: return
    val item = stockItemById(stockId) ?: return
    updateStockItem(item.copy(
        quantity = Math.addExact(item.quantity, sale.quantity),
        soldUnitsCount = (item.soldUnitsCount - sale.quantity).coerceAtLeast(0L)
    ))
    insertStockMovement(StockMovement(
        stockItemId = item.id, sku = item.sku, productName = item.name,
        movementType = StockMovementType.ADJUSTMENT.name, quantity = sale.quantity,
        unitCostCents = item.unitCostCents, referenceNote = "Estorno da venda #${sale.id}",
        dateMillis = System.currentTimeMillis()
    ))
}
