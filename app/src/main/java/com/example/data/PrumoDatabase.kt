package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.CashEntry
import com.example.model.FixedExpense
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem

@Database(
    entities = [
        CashEntry::class,
        StockItem::class,
        SaleRecord::class,
        SellerProfile::class,
        FixedExpense::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrumoDatabase : RoomDatabase() {
    abstract fun prumoDao(): PrumoDao

    companion object {
        @Volatile
        private var INSTANCE: PrumoDatabase? = null

        fun getInstance(context: Context): PrumoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrumoDatabase::class.java,
                    "prumo_gestao.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
