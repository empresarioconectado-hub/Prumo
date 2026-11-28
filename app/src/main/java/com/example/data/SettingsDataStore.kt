package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.BusinessSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prumoDataStore: DataStore<Preferences> by preferencesDataStore(name = "prumo_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val INITIAL_BALANCE_CENTS = longPreferencesKey("initial_balance_cents")
        val MIN_CASH_ALERT_CENTS = longPreferencesKey("min_cash_alert_cents")
        val TAX_RATE_PERCENT = doublePreferencesKey("tax_rate_percent")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<BusinessSettings> = context.prumoDataStore.data.map { prefs ->
        BusinessSettings(
            businessName = prefs[Keys.BUSINESS_NAME] ?: "Minha Empresa",
            initialBalanceCents = prefs[Keys.INITIAL_BALANCE_CENTS] ?: 0L,
            minCashAlertCents = prefs[Keys.MIN_CASH_ALERT_CENTS] ?: 500_000L,
            taxRatePercent = prefs[Keys.TAX_RATE_PERCENT] ?: 6.0,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM"
        )
    }

    suspend fun updateSettings(
        businessName: String,
        initialBalanceCents: Long,
        minCashAlertCents: Long,
        taxRatePercent: Double
    ) {
        context.prumoDataStore.edit { prefs ->
            prefs[Keys.BUSINESS_NAME] = businessName.trim().ifBlank { "Minha Empresa" }
            prefs[Keys.INITIAL_BALANCE_CENTS] = initialBalanceCents
            prefs[Keys.MIN_CASH_ALERT_CENTS] = minCashAlertCents.coerceAtLeast(0L)
            prefs[Keys.TAX_RATE_PERCENT] = taxRatePercent.coerceIn(0.0, 100.0)
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.prumoDataStore.edit { prefs -> prefs[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        require(mode in setOf("SYSTEM", "LIGHT", "DARK")) { "Modo de aparência inválido." }
        context.prumoDataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode }
    }

    suspend fun resetDefaults() {
        context.prumoDataStore.edit { prefs ->
            prefs[Keys.BUSINESS_NAME] = "Minha Empresa"
            prefs[Keys.INITIAL_BALANCE_CENTS] = 0L
            prefs[Keys.MIN_CASH_ALERT_CENTS] = 500_000L
            prefs[Keys.TAX_RATE_PERCENT] = 6.0
            prefs[Keys.THEME_MODE] = "SYSTEM"
        }
    }
}
