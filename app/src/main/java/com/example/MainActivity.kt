package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.PrumoApp
import com.example.ui.theme.PrumoTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsFlow = remember { SettingsDataStore(applicationContext).settingsFlow }
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = BusinessSettings())
            PrumoTheme(themeMode = settings.themeMode) {
                PrumoApp()
            }
        }
    }
}
