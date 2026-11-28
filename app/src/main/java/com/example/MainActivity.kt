package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PrumoBackupManager
import com.example.data.PrumoDatabase
import com.example.data.SettingsDataStore
import com.example.model.BusinessSettings
import com.example.ui.PrumoApp
import com.example.ui.theme.PrumoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsStore = remember { SettingsDataStore(applicationContext) }
            val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = BusinessSettings()
            )
            val backupManager = remember {
                PrumoBackupManager(
                    PrumoDatabase.getInstance(applicationContext).prumoDao(),
                    settingsStore
                )
            }
            var recoveryComplete by remember { mutableStateOf(false) }
            var recoveryError by remember { mutableStateOf<String?>(null) }
            var recoveryAttempt by remember { mutableStateOf(0) }

            LaunchedEffect(recoveryAttempt) {
                recoveryError = null
                try {
                    withContext(Dispatchers.IO) {
                        backupManager.recoverPendingRestore()
                    }
                    recoveryComplete = true
                } catch (error: Exception) {
                    recoveryError = error.message
                        ?: "Não foi possível recuperar os dados da última restauração."
                }
            }

            PrumoTheme(themeMode = settings.themeMode) {
                when {
                    recoveryComplete -> PrumoApp()
                    recoveryError != null -> RecoveryErrorScreen(
                        message = recoveryError ?: "Falha na recuperação dos dados.",
                        onRetry = { recoveryAttempt += 1 }
                    )
                    else -> RecoveryLoadingScreen()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RecoveryLoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text(
                text = "Verificando a integridade dos dados...",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun RecoveryErrorScreen(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Recuperação de dados pendente",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "O Prumo não exibirá os dados financeiros até concluir a recuperação do estado anterior.",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Tentar novamente")
            }
        }
    }
}
