package com.example.data

import android.content.Context
import android.util.AtomicFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.CashEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class PrumoBackupRecoveryInstrumentedTest {
    @Test
    fun interruptedRestoreIsRecoveredBeforeFinancialDataIsUsed() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(
            context,
            PrumoDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = database.prumoDao()
        val settings = SettingsDataStore(context)
        val manager = PrumoBackupManager(dao, settings)
        val journalFile = File(context.noBackupFilesDir, PrumoBackupManager.ROLLBACK_FILE_NAME)
        val atomicJournal = AtomicFile(journalFile)

        try {
            settings.updateSettings("Negócio original", 12_500L, 2_000L, 6.5)
            settings.setAppLockEnabled(true)
            settings.setThemeMode("DARK")
            dao.insertCashEntry(
                CashEntry(
                    description = "Receita original",
                    amountCents = 8_900L,
                    type = "INCOME",
                    category = "Vendas",
                    account = "Conta Principal",
                    paymentMethod = "Pix",
                    status = "PAID",
                    dateMillis = 1_700_000_000_000L
                )
            )

            val journal = JSONObject(manager.exportJson())
            journal.getJSONObject("settings")
                .put("rollbackAppLockEnabled", true)
                .put("rollbackThemeMode", "DARK")
            val output = atomicJournal.startWrite()
            output.write(journal.toString(2).toByteArray(Charsets.UTF_8))
            atomicJournal.finishWrite(output)

            dao.clearAllDataAtomic()
            settings.updateSettings("Negócio alterado", 99_000L, 5_000L, 9.0)
            settings.setAppLockEnabled(false)
            settings.setThemeMode("LIGHT")

            manager.recoverPendingRestore()

            val restoredEntry = dao.allCashEntries().single()
            val restoredSettings = settings.settingsFlow.first()
            assertEquals("Receita original", restoredEntry.description)
            assertEquals(8_900L, restoredEntry.amountCents)
            assertEquals("Negócio original", restoredSettings.businessName)
            assertEquals(12_500L, restoredSettings.initialBalanceCents)
            assertEquals(true, restoredSettings.appLockEnabled)
            assertEquals("DARK", restoredSettings.themeMode)
            assertFalse(journalFile.exists())
        } finally {
            settings.setAppLockEnabled(false)
            settings.setThemeMode("SYSTEM")
            settings.resetDefaults()
            atomicJournal.delete()
            database.close()
        }
    }
}
