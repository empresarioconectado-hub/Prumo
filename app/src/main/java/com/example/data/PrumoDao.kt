package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrumoDao {
    @Query("SELECT * FROM cash_entries ORDER BY dateMillis DESC, id DESC") fun observeCashEntries(): Flow<List<CashEntry>>
    @Query("SELECT * FROM cash_entries") suspend fun allCashEntries(): List<CashEntry>
    @Query("SELECT * FROM cash_entries WHERE id = :id LIMIT 1") suspend fun cashEntryById(id: Long): CashEntry?
    @Query("SELECT * FROM cash_entries WHERE saleId = :saleId LIMIT 1") suspend fun cashEntryForSale(saleId: Long): CashEntry?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCashEntry(entry: CashEntry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCashEntries(entries: List<CashEntry>)
    @Update suspend fun updateCashEntry(entry: CashEntry)
    @Query("UPDATE cash_entries SET status = :status WHERE id = :id") suspend fun updateCashEntryStatus(id: Long, status: String)
    @Query("DELETE FROM cash_entries WHERE id = :id") suspend fun deleteCashEntryById(id: Long)
    @Query("DELETE FROM cash_entries WHERE saleId = :saleId") suspend fun deleteCashEntriesForSale(saleId: Long)
    @Query("DELETE FROM cash_entries") suspend fun clearCashEntries()

    @Query("SELECT * FROM stock_items ORDER BY name ASC") fun observeStockItems(): Flow<List<StockItem>>
    @Query("SELECT * FROM stock_items") suspend fun allStockItems(): List<StockItem>
    @Query("SELECT * FROM stock_items WHERE id = :id LIMIT 1") suspend fun stockItemById(id: Long): StockItem?
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
    @Query("UPDATE sale_records SET paymentStatus = :status WHERE id = :saleId") suspend fun updateSalePaymentStatus(saleId: Long, status: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSale(sale: SaleRecord): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSales(sales: List<SaleRecord>)
    @Update suspend fun updateSale(sale: SaleRecord)
    @Query("DELETE FROM sale_records WHERE id = :id") suspend fun deleteSaleById(id: Long)
    @Query("DELETE FROM sale_records") suspend fun clearSales()

    @Transaction
    suspend fun saveSaleAtomic(sale: SaleRecord) {
        val previous = if (sale.id != 0L) saleById(sale.id) else null
        if (previous != null) reverseSaleStock(previous)
        val id = if (sale.id == 0L) insertSale(sale) else { updateSale(sale); sale.id }
        val saved = sale.copy(id = id)
        if (saved.stockItemId != null) {
            val item = stockItemById(saved.stockItemId) ?: error("Produto vinculado não encontrado.")
            require(saved.quantity > 0L) { "A quantidade da venda deve ser maior que zero." }
            require(item.quantity >= saved.quantity) { "Estoque insuficiente para ${item.name}. Disponível: ${item.quantity}." }
            updateStockItem(item.copy(quantity = item.quantity - saved.quantity, soldUnitsCount = item.soldUnitsCount + saved.quantity))
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

    private suspend fun reverseSaleStock(sale: SaleRecord) {
        val stockId = sale.stockItemId ?: return
        val item = stockItemById(stockId) ?: return
        updateStockItem(item.copy(
            quantity = item.quantity + sale.quantity,
            soldUnitsCount = (item.soldUnitsCount - sale.quantity).coerceAtLeast(0L)
        ))
        insertStockMovement(StockMovement(
            stockItemId = item.id, sku = item.sku, productName = item.name,
            movementType = StockMovementType.ADJUSTMENT.name, quantity = sale.quantity,
            unitCostCents = item.unitCostCents, referenceNote = "Estorno da venda #${sale.id}",
            dateMillis = System.currentTimeMillis()
        ))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSellers(sellers: List<SellerProfile>)
    @Query("SELECT * FROM seller_profiles ORDER BY name ASC") fun observeSellers(): Flow<List<SellerProfile>>
    @Query("SELECT * FROM seller_profiles") suspend fun allSellers(): List<SellerProfile>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSeller(seller: SellerProfile): Long
    @Update suspend fun updateSeller(seller: SellerProfile)
    @Query("DELETE FROM seller_profiles WHERE id = :id") suspend fun deleteSellerById(id: Long)
    @Query("DELETE FROM seller_profiles") suspend fun clearSellers()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFixedExpenses(expenses: List<FixedExpense>)
    @Query("SELECT * FROM fixed_expenses ORDER BY dateMillis DESC, id DESC") fun observeFixedExpenses(): Flow<List<FixedExpense>>
    @Query("SELECT * FROM fixed_expenses") suspend fun allFixedExpenses(): List<FixedExpense>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFixedExpense(expense: FixedExpense): Long
    @Update suspend fun updateFixedExpense(expense: FixedExpense)
    @Query("DELETE FROM fixed_expenses WHERE id = :id") suspend fun deleteFixedExpenseById(id: Long)
    @Query("DELETE FROM fixed_expenses") suspend fun clearFixedExpenses()

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
