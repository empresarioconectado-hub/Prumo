package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CashEntry
import com.example.model.FixedExpense
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PrumoDao {
    // Cash Entries
    @Query("SELECT * FROM cash_entries ORDER BY dateMillis DESC, id DESC")
    fun observeCashEntries(): Flow<List<CashEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashEntry(entry: CashEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashEntries(entries: List<CashEntry>)

    @Update
    suspend fun updateCashEntry(entry: CashEntry)

    @Query("UPDATE cash_entries SET status = :status WHERE id = :id")
    suspend fun updateCashEntryStatus(id: Long, status: String)

    @Query("DELETE FROM cash_entries WHERE id = :id")
    suspend fun deleteCashEntryById(id: Long)

    @Query("DELETE FROM cash_entries")
    suspend fun clearCashEntries()

    // Stock Items
    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun observeStockItems(): Flow<List<StockItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItem(item: StockItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItems(items: List<StockItem>)

    @Update
    suspend fun updateStockItem(item: StockItem)

    @Query("DELETE FROM stock_items WHERE id = :id")
    suspend fun deleteStockItemById(id: Long)

    @Query("DELETE FROM stock_items")
    suspend fun clearStockItems()

    // Sale Records
    @Query("SELECT * FROM sale_records ORDER BY saleDateMillis DESC, id DESC")
    fun observeSales(): Flow<List<SaleRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleRecord>)

    @Update
    suspend fun updateSale(sale: SaleRecord)

    @Query("DELETE FROM sale_records WHERE id = :id")
    suspend fun deleteSaleById(id: Long)

    @Query("DELETE FROM sale_records")
    suspend fun clearSales()

    // Seller Profiles
    @Query("SELECT * FROM seller_profiles ORDER BY name ASC")
    fun observeSellers(): Flow<List<SellerProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeller(seller: SellerProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSellers(sellers: List<SellerProfile>)

    @Update
    suspend fun updateSeller(seller: SellerProfile)

    @Query("DELETE FROM seller_profiles WHERE id = :id")
    suspend fun deleteSellerById(id: Long)

    @Query("DELETE FROM seller_profiles")
    suspend fun clearSellers()

    // Fixed Expenses
    @Query("SELECT * FROM fixed_expenses ORDER BY dateMillis DESC, id DESC")
    fun observeFixedExpenses(): Flow<List<FixedExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedExpense(expense: FixedExpense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedExpenses(expenses: List<FixedExpense>)

    @Update
    suspend fun updateFixedExpense(expense: FixedExpense)

    @Query("DELETE FROM fixed_expenses WHERE id = :id")
    suspend fun deleteFixedExpenseById(id: Long)

    @Query("DELETE FROM fixed_expenses")
    suspend fun clearFixedExpenses()
}
