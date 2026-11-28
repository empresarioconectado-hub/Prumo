package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.StockItem
import com.example.model.StockMovementType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class PrumoDaoInstrumentedTest {
    private lateinit var database: PrumoDatabase
    private lateinit var dao: PrumoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PrumoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.prumoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createItem(quantity: Long): Long = dao.insertStockItem(
        StockItem(
            sku = "TEST-001",
            name = "Produto de teste",
            category = "Mercadorias",
            quantity = quantity,
            minStock = 2L,
            leadTimeDays = 2L,
            avgDailySales = 1.0,
            unitCostCents = 150L,
            unitPriceCents = 300L
        )
    )

    @Test
    fun purchaseUpdatesBalanceAndMovementInOneTransaction() = runBlocking {
        val id = createItem(10L)

        val result = dao.applyStockMovementAtomic(id, StockMovementType.PURCHASE, 4L)

        assertEquals(true, result)
        assertEquals(14L, dao.stockItemById(id)?.quantity)
        val movements = dao.allStockMovements()
        assertEquals(1, movements.size)
        assertEquals(StockMovementType.PURCHASE.name, movements.single().movementType)
        assertEquals(4L, movements.single().quantity)
    }

    @Test
    fun insufficientStockDoesNotChangeBalanceOrCreateMovement() = runBlocking {
        val id = createItem(3L)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { dao.applyStockMovementAtomic(id, StockMovementType.SALE, 4L) }
        }

        assertEquals(3L, dao.stockItemById(id)?.quantity)
        assertEquals(0, dao.allStockMovements().size)
    }

    @Test
    fun adjustmentCanSetStockToZeroAndRecordsTheDifference() = runBlocking {
        val id = createItem(10L)

        val result = dao.applyStockMovementAtomic(id, StockMovementType.ADJUSTMENT, 0L)

        assertEquals(true, result)
        assertEquals(0L, dao.stockItemById(id)?.quantity)
        assertEquals(10L, dao.allStockMovements().single().quantity)
    }

    @Test
    fun clearAllDataAtomicRemovesInventoryAndMovementHistoryTogether() = runBlocking {
        val id = createItem(10L)
        dao.applyStockMovementAtomic(id, StockMovementType.PURCHASE, 2L)

        dao.clearAllDataAtomic()

        val snapshot = dao.readAllDataSnapshot()
        assertEquals(0, snapshot.stockItems.size)
        assertEquals(0, snapshot.stockMovements.size)
        assertEquals(0, snapshot.cashEntries.size)
        assertEquals(0, snapshot.sales.size)
        assertEquals(0, snapshot.sellers.size)
        assertEquals(0, snapshot.fixedExpenses.size)
    }

    @Test
    fun purchaseOverflowDoesNotChangeBalanceOrCreateMovement() = runBlocking {
        val id = createItem(Long.MAX_VALUE)

        assertThrows(ArithmeticException::class.java) {
            runBlocking { dao.applyStockMovementAtomic(id, StockMovementType.PURCHASE, 1L) }
        }

        assertEquals(Long.MAX_VALUE, dao.stockItemById(id)?.quantity)
        assertEquals(0, dao.allStockMovements().size)
    }
}
