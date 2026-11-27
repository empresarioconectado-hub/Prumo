package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.CashEntry
import com.example.model.FixedExpense
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem
import com.example.model.StockMovement

@Database(
    entities = [CashEntry::class, StockItem::class, SaleRecord::class, SellerProfile::class, FixedExpense::class, StockMovement::class],
    version = 2,
    exportSchema = false
)
abstract class PrumoDatabase : RoomDatabase() {
    abstract fun prumoDao(): PrumoDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cash_entries ADD COLUMN saleId INTEGER")
                db.execSQL("ALTER TABLE stock_items ADD COLUMN initialQuantity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE stock_items SET initialQuantity = quantity")
                db.execSQL("ALTER TABLE sale_records ADD COLUMN stockItemId INTEGER")
                db.execSQL("ALTER TABLE sale_records ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Pix'")
                db.execSQL("ALTER TABLE sale_records ADD COLUMN account TEXT NOT NULL DEFAULT 'Conta Principal'")
                db.execSQL("ALTER TABLE sale_records ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'PAID'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS stock_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        stockItemId INTEGER NOT NULL,
                        sku TEXT NOT NULL,
                        productName TEXT NOT NULL,
                        movementType TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitCostCents INTEGER NOT NULL,
                        referenceNote TEXT NOT NULL,
                        dateMillis INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        @Volatile private var INSTANCE: PrumoDatabase? = null

        fun getInstance(context: Context): PrumoDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, PrumoDatabase::class.java, "prumo_gestao.db"
            ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
