package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsDataStoreInstrumentedTest {
    @Test
    fun appearancePreferencePersistsSupportedModesAndRejectsUnknownValues() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsDataStore(context)

        try {
            settings.setThemeMode("DARK")
            assertEquals("DARK", settings.settingsFlow.first().themeMode)

            settings.setThemeMode("LIGHT")
            assertEquals("LIGHT", settings.settingsFlow.first().themeMode)

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { settings.setThemeMode("AUTO") }
            }
        } finally {
            settings.setThemeMode("SYSTEM")
        }
    }
}
