package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Entity(tableName = "cash_entries")
data class CashEntryV1(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val description: String,
    val amountCents: Long,
    val type: String,
    val category: String,
    val account: String,
    val paymentMethod: String,
    val status: String,
    val dateMillis: Long
)

@Entity(tableName = "stock_items")
data class StockItemV1(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sku: String,
    val name: String,
    val category: String,
    val unit: String,
    val quantity: Long,
    val minStock: Long,
    val leadTimeDays: Long,
    val avgDailySales: Double,
    val unitCostCents: Long,
    val unitPriceCents: Long,
    val soldUnitsCount: Long,
    val lossUnitsCount: Long
)

@Entity(tableName = "sale_records")
data class SaleRecordV1(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val description: String,
    val clientName: String,
    val sellerName: String,
    val quantity: Long,
    val grossAmountCents: Long,
    val costAmountCents: Long,
    val taxPercent: Double,
    val commissionPercent: Double,
    val saleDateMillis: Long
)

@Entity(tableName = "seller_profiles")
data class SellerProfileV1(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val monthlyGoalCents: Long,
    val commissionPercent: Double
)

@Entity(tableName = "fixed_expenses")
data class FixedExpenseV1(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val description: String,
    val category: String,
    val amountCents: Long,
    val dateMillis: Long
)

@Dao
interface PrumoDatabaseV1Dao {
    @Insert suspend fun insertCashEntry(entry: CashEntryV1): Long
    @Insert suspend fun insertStockItem(item: StockItemV1): Long
    @Insert suspend fun insertSale(sale: SaleRecordV1): Long
    @Insert suspend fun insertSeller(seller: SellerProfileV1): Long
    @Insert suspend fun insertFixedExpense(expense: FixedExpenseV1): Long
}

@Database(
    entities = [
        CashEntryV1::class,
        StockItemV1::class,
        SaleRecordV1::class,
        SellerProfileV1::class,
        FixedExpenseV1::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrumoDatabaseV1 : RoomDatabase() {
    abstract fun v1Dao(): PrumoDatabaseV1Dao
}

class PrumoDatabaseMigrationInstrumentedTest {
    @Test
    fun migrationFromVersionOnePreservesRecordsAndAddsVersionTwoFields() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "prumo_migration_v1_test.db"
        context.deleteDatabase(databaseName)

        var versionOneDatabase: PrumoDatabaseV1? = null
        var migratedDatabase: PrumoDatabase? = null
        try {
            versionOneDatabase = Room.databaseBuilder(
                context,
                PrumoDatabaseV1::class.java,
                databaseName
            ).build()

            val oldDao = versionOneDatabase.v1Dao()
            val cashId = oldDao.insertCashEntry(
                CashEntryV1(
                    description = "Receita anterior à migração",
                    amountCents = 15_000L,
                    type = "INCOME",
                    category = "Vendas",
                    account = "Conta Principal",
                    paymentMethod = "Pix",
                    status = "PAID",
                    dateMillis = 1_700_000_000_000L
                )
            )
            val stockId = oldDao.insertStockItem(
                StockItemV1(
                    sku = "LEGADO-001",
                    name = "Produto legado",
                    category = "Geral",
                    unit = "un",
                    quantity = 12L,
                    minStock = 2L,
                    leadTimeDays = 3L,
                    avgDailySales = 1.0,
                    unitCostCents = 100L,
                    unitPriceCents = 200L,
                    soldUnitsCount = 3L,
                    lossUnitsCount = 1L
                )
            )
            val saleId = oldDao.insertSale(
                SaleRecordV1(
                    description = "Venda anterior à migração",
                    clientName = "Cliente legado",
                    sellerName = "Vendedor legado",
                    quantity = 2L,
                    grossAmountCents = 1_000L,
                    costAmountCents = 400L,
                    taxPercent = 6.0,
                    commissionPercent = 2.0,
                    saleDateMillis = 1_700_000_000_000L
                )
            )
            val sellerId = oldDao.insertSeller(
                SellerProfileV1(
                    name = "Vendedor legado",
                    monthlyGoalCents = 50_000L,
                    commissionPercent = 2.0
                )
            )
            val expenseId = oldDao.insertFixedExpense(
                FixedExpenseV1(
                    description = "Aluguel legado",
                    category = "Ocupação",
                    amountCents = 80_000L,
                    dateMillis = 1_700_000_000_000L
                )
            )
            versionOneDatabase.close()
            versionOneDatabase = null

            migratedDatabase = Room.databaseBuilder(
                context,
                PrumoDatabase::class.java,
                databaseName
            ).addMigrations(PrumoDatabase.MIGRATION_1_2).build()

            val migratedDao = migratedDatabase.prumoDao()
            val cash = migratedDao.allCashEntries().single()
            val stock = migratedDao.allStockItems().single()
            val sale = migratedDao.allSales().single()
            val seller = migratedDao.allSellers().single()
            val expense = migratedDao.allFixedExpenses().single()

            assertEquals(cashId, cash.id)
            assertNull(cash.saleId)
            assertEquals(stockId, stock.id)
            assertEquals(12L, stock.initialQuantity)
            assertEquals(saleId, sale.id)
            assertNull(sale.stockItemId)
            assertEquals("Pix", sale.paymentMethod)
            assertEquals("Conta Principal", sale.account)
            assertEquals("PAID", sale.paymentStatus)
            assertEquals(sellerId, seller.id)
            assertEquals(expenseId, expense.id)
            assertEquals(0, migratedDao.allStockMovements().size)
        } finally {
            migratedDatabase?.close()
            versionOneDatabase?.close()
            context.deleteDatabase(databaseName)
        }
    }
}
