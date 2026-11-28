package com.example.ui

import android.app.Application
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.PrumoDatabase
import com.example.data.PrumoRepository
import com.example.data.SettingsDataStore
import com.example.model.BusinessSettings
import com.example.model.CashEntry
import com.example.model.CashEntryType
import com.example.model.FixedExpense
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem
import com.example.ui.components.BusinessSettingsDialog
import com.example.ui.components.CashEntryDialog
import com.example.ui.components.FixedExpenseDialog
import com.example.ui.components.QuickEntryMenuDialog
import com.example.ui.components.SaleRecordDialog
import com.example.ui.components.SellerDialog
import com.example.ui.components.StockItemDialog
import com.example.ui.components.StockMovementDialog
import com.example.ui.screens.CashFlowScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SalesDreScreen
import com.example.ui.screens.StockScreen
import com.example.ui.viewmodel.PrumoViewModel
import com.example.util.PrumoReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

private const val ROUTE_OVERVIEW = "overview"
private const val ROUTE_CASH = "cash_flow"
private const val ROUTE_STOCK = "stock"
private const val ROUTE_SALES_DRE = "sales_dre"

private data class NavTabItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val testTag: String
)

private sealed interface ActiveDialog {
    data object QuickMenu : ActiveDialog
    data class CashEntryForm(val existing: CashEntry?, val initialType: CashEntryType) : ActiveDialog
    data class StockForm(val existing: StockItem?) : ActiveDialog
    data class StockMovementForm(val initialItem: StockItem?) : ActiveDialog
    data class SaleForm(val existing: SaleRecord?) : ActiveDialog
    data class SellerForm(val existing: SellerProfile?) : ActiveDialog
    data class FixedExpenseForm(val existing: FixedExpense?) : ActiveDialog
    data object BusinessSettingsForm : ActiveDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrumoApp() {
    val activity = LocalContext.current.findFragmentActivity()
        ?: error("A tela do Prumo precisa estar vinculada a uma FragmentActivity.")
    val context = activity.applicationContext as Application
    val viewModel: PrumoViewModel = viewModel(
        factory = remember(context) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = PrumoDatabase.getInstance(context)
                    val settings = SettingsDataStore(context)
                    val repository = PrumoRepository(db.prumoDao(), settings)
                    return PrumoViewModel(repository) as T
                }
            }
        }
    )

    val dashboardData by viewModel.dashboardState.collectAsStateWithLifecycle()
    val securitySettingsFlow = remember(context) { SettingsDataStore(context).settingsFlow }
    val securitySettings by securitySettingsFlow.collectAsStateWithLifecycle(
        initialValue = BusinessSettings(appLockEnabled = true)
    )
    var securitySettingsLoaded by remember { mutableStateOf(false) }
    val appLockEnabled = securitySettings.appLockEnabled
    var hideValues by rememberSaveable { mutableStateOf(false) }
    var isAppUnlocked by remember { mutableStateOf(false) }
    var appLockError by remember { mutableStateOf<String?>(null) }
    val keyguardManager = remember(activity) {
        activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }
    var deviceHasSecureLock by remember { mutableStateOf(false) }

    LaunchedEffect(securitySettingsFlow, keyguardManager) {
        securitySettingsFlow.first()
        deviceHasSecureLock = keyguardManager.isDeviceSecure
        securitySettingsLoaded = true
    }

    val biometricPrompt = remember(activity) {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isAppUnlocked = true
                    appLockError = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    isAppUnlocked = false
                    appLockError = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    appLockError = "Não foi possível confirmar sua identidade. Tente novamente."
                }
            }
        )
    }

    fun requestAppUnlock() {
        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Prumo")
            .setSubtitle("Confirme sua identidade para acessar os dados financeiros.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            promptBuilder.setDeviceCredentialAllowed(true)
        }
        biometricPrompt.authenticate(promptBuilder.build())
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appLockEnabled, securitySettingsLoaded, biometricPrompt) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (securitySettingsLoaded && appLockEnabled) {
                        isAppUnlocked = false
                    }
                }
                Lifecycle.Event.ON_START -> {
                    deviceHasSecureLock = keyguardManager.isDeviceSecure
                    if (securitySettingsLoaded && appLockEnabled && deviceHasSecureLock && !isAppUnlocked) {
                        appLockError = null
                        requestAppUnlock()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SideEffect {
        if (!securitySettingsLoaded || appLockEnabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val backupRepository = remember(context) {
        PrumoRepository(PrumoDatabase.getInstance(context).prumoDao(), SettingsDataStore(context))
    }
    var backupPassword by remember { mutableStateOf("") }
    var backupPasswordConfirmation by remember { mutableStateOf("") }
    var pendingBackupImportJson by remember { mutableStateOf<String?>(null) }
    var showBackupExportPasswordDialog by remember { mutableStateOf(false) }
    var showBackupImportPasswordDialog by remember { mutableStateOf(false) }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            try {
                val json = backupRepository.exportBackupJson(backupPassword)
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error("Não foi possível abrir o arquivo de destino.")
                    output.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                backupPassword = ""
                backupPasswordConfirmation = ""
                snackbarHostState.showSnackbar("Backup criptografado exportado com sucesso.")
            } catch (error: Exception) {
                backupPassword = ""
                backupPasswordConfirmation = ""
                snackbarHostState.showSnackbar("Falha ao exportar backup: ${error.message}")
            }
        } else {
            backupPassword = ""
            backupPasswordConfirmation = ""
        }
    }
    val exportDrePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            try {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Não foi possível abrir o arquivo de destino.")
                output.use { PrumoReportExporter.writeDrePdf(it, dashboardData) }
                snackbarHostState.showSnackbar("DRE exportada em PDF.")
            } catch (error: Exception) {
                snackbarHostState.showSnackbar("Falha ao exportar PDF: ${error.message}")
            }
        }
    }
    val exportCashCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            try {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Não foi possível abrir o arquivo de destino.")
                output.use { it.write(PrumoReportExporter.cashEntriesCsv(dashboardData).toByteArray(Charsets.UTF_8)) }
                snackbarHostState.showSnackbar("Fluxo de caixa exportado em CSV.")
            } catch (error: Exception) {
                snackbarHostState.showSnackbar("Falha ao exportar CSV: ${error.message}")
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Não foi possível abrir o arquivo selecionado.")
                    input.use { stream ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8_192)
                        var totalBytes = 0
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            require(totalBytes <= 20_000_000) { "O arquivo de backup excede o tamanho permitido." }
                            output.write(buffer, 0, count)
                        }
                        String(output.toByteArray(), StandardCharsets.UTF_8)
                    }
                }
                pendingBackupImportJson = json
                backupPassword = ""
                showBackupImportPasswordDialog = true
            } catch (error: Exception) {
                snackbarHostState.showSnackbar("Falha ao ler backup: ${error.message}")
            }
        }
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = backStackEntry?.destination?.route

    var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }
    var currentTabIndex by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.feedbackEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navItems = remember {
        listOf(
            NavTabItem("Início", Icons.Filled.Home, ROUTE_OVERVIEW, "nav_overview"),
            NavTabItem("Caixa", Icons.Filled.AccountBalanceWallet, ROUTE_CASH, "nav_cash"),
            NavTabItem("Estoque", Icons.Filled.Inventory2, ROUTE_STOCK, "nav_stock"),
            NavTabItem("Vendas & DRE", Icons.AutoMirrored.Filled.TrendingUp, ROUTE_SALES_DRE, "nav_dre")
        )
    }

    fun navigateTo(route: String, idx: Int) {
        currentTabIndex = idx
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Column {
                            Text(
                                text = "Prumo Gestão",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${dashboardData.settings.businessName} · 100% Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { hideValues = !hideValues },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (hideValues) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (hideValues) "Mostrar valores" else "Ocultar valores"
                            )
                        }
                        IconButton(
                            onClick = { activeDialog = ActiveDialog.BusinessSettingsForm },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_button")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Configurações e dados")
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEachIndexed { idx, item ->
                            val selected = currentRouteName == item.route ||
                                (currentRouteName == null && idx == currentTabIndex)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route, idx) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { activeDialog = ActiveDialog.QuickMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_quick_entry")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Lançamento rápido")
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                    if (useRail) {
                    NavigationRail {
                        navItems.forEachIndexed { idx, item ->
                            val selected = currentRouteName == item.route ||
                                (currentRouteName == null && idx == currentTabIndex)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route, idx) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_OVERVIEW,
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                    ) {
                        composable(ROUTE_OVERVIEW) {
                            OverviewScreen(
                                data = dashboardData,
                                hideValues = hideValues,
                                onQuickIncome = { activeDialog = ActiveDialog.CashEntryForm(null, CashEntryType.INCOME) },
                                onQuickExpense = { activeDialog = ActiveDialog.CashEntryForm(null, CashEntryType.EXPENSE) },
                                onQuickSale = { activeDialog = ActiveDialog.SaleForm(null) },
                                onQuickStockMove = { activeDialog = ActiveDialog.StockMovementForm(null) },
                                onOpenCashTab = { navigateTo(ROUTE_CASH, 1) },
                                onOpenStockTab = { navigateTo(ROUTE_STOCK, 2) },
                                onOpenDreTab = { navigateTo(ROUTE_SALES_DRE, 3) },
                                onLoadSampleData = { viewModel.loadSampleData() }
                            )
                        }
                        composable(ROUTE_CASH) {
                            CashFlowScreen(
                                data = dashboardData,
                                hideValues = hideValues,
                                onShiftMonth = viewModel::shiftMonth,
                                onAddEntry = { activeDialog = ActiveDialog.CashEntryForm(null, CashEntryType.INCOME) },
                                onEditEntry = { activeDialog = ActiveDialog.CashEntryForm(it, it.entryType) },
                                onTogglePaid = viewModel::markAsPaid,
                                onDeleteEntry = viewModel::deleteCashEntry
                            )
                        }
                        composable(ROUTE_STOCK) {
                            StockScreen(
                                data = dashboardData,
                                onAddItem = { activeDialog = ActiveDialog.StockForm(null) },
                                onEditItem = { activeDialog = ActiveDialog.StockForm(it) },
                                onMoveItem = { activeDialog = ActiveDialog.StockMovementForm(it) },
                                onDeleteItem = viewModel::deleteStockItem,
                                hideValues = hideValues
                            )
                        }
                        composable(ROUTE_SALES_DRE) {
                            SalesDreScreen(
                                data = dashboardData,
                                hideValues = hideValues,
                                onShiftMonth = viewModel::shiftMonth,
                                onAddSale = { activeDialog = ActiveDialog.SaleForm(null) },
                                onEditSale = { activeDialog = ActiveDialog.SaleForm(it) },
                                onDeleteSale = viewModel::deleteSale,
                                onAddSeller = { activeDialog = ActiveDialog.SellerForm(null) },
                                onEditSeller = { activeDialog = ActiveDialog.SellerForm(it) },
                                onDeleteSeller = viewModel::deleteSeller,
                                onAddFixedExpense = { activeDialog = ActiveDialog.FixedExpenseForm(null) },
                                onEditFixedExpense = { activeDialog = ActiveDialog.FixedExpenseForm(it) },
                                onDeleteFixedExpense = viewModel::deleteFixedExpense,
                                onExportDrePdf = { exportDrePdfLauncher.launch("prumo-dre-${System.currentTimeMillis()}.pdf") },
                                onExportCashCsv = { exportCashCsvLauncher.launch("prumo-caixa-${System.currentTimeMillis()}.csv") }
                            )
                        }
                    }
                }
            }
        }
    }

    when (val dialog = activeDialog) {
        null -> Unit
        ActiveDialog.QuickMenu -> {
            QuickEntryMenuDialog(
                onDismiss = { activeDialog = null },
                onNewIncome = { activeDialog = ActiveDialog.CashEntryForm(null, CashEntryType.INCOME) },
                onNewExpense = { activeDialog = ActiveDialog.CashEntryForm(null, CashEntryType.EXPENSE) },
                onNewSale = { activeDialog = ActiveDialog.SaleForm(null) },
                onMoveStock = { activeDialog = ActiveDialog.StockMovementForm(null) },
                onNewFixedExpense = { activeDialog = ActiveDialog.FixedExpenseForm(null) }
            )
        }
        is ActiveDialog.CashEntryForm -> {
            CashEntryDialog(
                existing = dialog.existing,
                initialType = dialog.initialType,
                onDismiss = { activeDialog = null },
                onSave = { entry ->
                    viewModel.saveCashEntry(entry)
                    activeDialog = null
                }
            )
        }
        is ActiveDialog.StockForm -> {
            StockItemDialog(
                existing = dialog.existing,
                onDismiss = { activeDialog = null },
                onSave = { item ->
                    viewModel.saveStockItem(item)
                    activeDialog = null
                }
            )
        }
        is ActiveDialog.StockMovementForm -> {
            StockMovementDialog(
                stockItems = dashboardData.stockItems,
                initialItem = dialog.initialItem,
                onDismiss = { activeDialog = null },
                onConfirm = { item, type, qty ->
                    viewModel.applyStockMovement(item, type, qty)
                    activeDialog = null
                }
            )
        }
        is ActiveDialog.SaleForm -> {
            SaleRecordDialog(
                existing = dialog.existing,
                sellers = dashboardData.sellers,
                stockItems = dashboardData.stockItems,
                defaultTaxPercent = dashboardData.settings.taxRatePercent,
                onDismiss = { activeDialog = null },
                onSave = { sale ->
                    viewModel.saveSale(sale)
                    activeDialog = null
                }
            )
        }
        is ActiveDialog.SellerForm -> {
            SellerDialog(
                existing = dialog.existing,
                onDismiss = { activeDialog = null },
                onSave = { seller ->
                    viewModel.saveSeller(seller)
                    activeDialog = null
                }
            )
        }
        is ActiveDialog.FixedExpenseForm -> {
            FixedExpenseDialog(
                existing = dialog.existing,
                onDismiss = { activeDialog = null },
                onSave = { exp ->
                    viewModel.saveFixedExpense(exp)
                    activeDialog = null
                }
            )
        }
        ActiveDialog.BusinessSettingsForm -> {
            BusinessSettingsDialog(
                current = dashboardData.settings,
                onDismiss = { activeDialog = null },
                onSave = { name, initCents, minCents, taxRate ->
                    viewModel.saveSettings(name, initCents, minCents, taxRate)
                    activeDialog = null
                },
                onLoadSampleData = { viewModel.loadSampleData() },
                onClearAllData = { viewModel.clearAllData() },
                onExportBackup = {
                    activeDialog = null
                    backupPassword = ""
                    backupPasswordConfirmation = ""
                    showBackupExportPasswordDialog = true
                },
                onImportBackup = {
                    activeDialog = null
                    importBackupLauncher.launch(arrayOf("application/json", "text/json"))
                },
                appLockEnabled = appLockEnabled,
                onThemeModeChange = { mode -> viewModel.setThemeMode(mode) },
                onOpenPrivacyPolicy = {
                    activeDialog = null
                    activity.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/empresarioconectado-hub/Prumo/blob/main/docs/politica-de-privacidade.md"))
                    )
                },
                onOpenTerms = {
                    activeDialog = null
                    activity.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/empresarioconectado-hub/Prumo/blob/main/docs/termos-de-uso.md"))
                    )
                },
                onToggleAppLock = { enabled ->
                    if (enabled && !keyguardManager.isDeviceSecure) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Configure um PIN, padrão ou senha de bloqueio no Android antes de ativar a proteção do Prumo.")
                        }
                        false
                    } else {
                        viewModel.setAppLockEnabled(enabled)
                        if (enabled) {
                            isAppUnlocked = true
                            appLockError = null
                        }
                        true
                    }
                }
            )
        }
    }
    if (showBackupExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showBackupExportPasswordDialog = false
                backupPassword = ""
                backupPasswordConfirmation = ""
            },
            title = { Text("Proteger backup") },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Text("Crie uma senha com pelo menos 12 caracteres. Ela será necessária para restaurar este arquivo e não é armazenada pelo Prumo.")
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it.take(128) },
                        label = { Text("Senha do backup") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = backupPasswordConfirmation,
                        onValueChange = { backupPasswordConfirmation = it.take(128) },
                        label = { Text("Confirme a senha") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (backupPasswordConfirmation.isNotEmpty() && backupPassword != backupPasswordConfirmation) {
                        Text("As senhas não coincidem.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = backupPassword.length >= 12 && backupPassword == backupPasswordConfirmation,
                    onClick = {
                        showBackupExportPasswordDialog = false
                        exportBackupLauncher.launch("prumo-backup-${System.currentTimeMillis()}.json")
                    }
                ) { Text("Exportar criptografado") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupExportPasswordDialog = false
                    backupPassword = ""
                    backupPasswordConfirmation = ""
                }) { Text("Cancelar") }
            }
        )
    }

    if (showBackupImportPasswordDialog) {
        val encryptedImport = pendingBackupImportJson?.contains("prumo-encrypted-backup") == true
        AlertDialog(
            onDismissRequest = {
                showBackupImportPasswordDialog = false
                pendingBackupImportJson = null
                backupPassword = ""
            },
            title = { Text("Restaurar backup") },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (encryptedImport) "Informe a senha usada na exportação. Sem a senha correta, o arquivo não poderá ser restaurado."
                        else "Este é um backup antigo sem criptografia. Você pode restaurá-lo sem senha; faça um novo backup protegido após a importação."
                    )
                    if (encryptedImport) {
                        OutlinedTextField(
                            value = backupPassword,
                            onValueChange = { backupPassword = it.take(128) },
                            label = { Text("Senha do backup") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pendingBackupImportJson != null && (!encryptedImport || backupPassword.isNotBlank()),
                    onClick = {
                        val json = pendingBackupImportJson ?: return@TextButton
                        val password = backupPassword
                        showBackupImportPasswordDialog = false
                        pendingBackupImportJson = null
                        backupPassword = ""
                        coroutineScope.launch {
                            try {
                                backupRepository.restoreBackupJson(json, password)
                                snackbarHostState.showSnackbar("Backup restaurado. Os dados locais foram substituídos.")
                            } catch (error: Exception) {
                                snackbarHostState.showSnackbar("Falha ao restaurar backup: ${error.message}")
                            }
                        }
                    }
                ) { Text("Restaurar dados") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupImportPasswordDialog = false
                    pendingBackupImportJson = null
                    backupPassword = ""
                }) { Text("Cancelar") }
            }
        )
    }

    if (!securitySettingsLoaded || (appLockEnabled && !isAppUnlocked)) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    if (securitySettingsLoaded) "Prumo bloqueado" else "Preparando proteção",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        !securitySettingsLoaded -> "Aguarde enquanto as configurações de segurança são carregadas."
                        deviceHasSecureLock -> "Use a biometria ou a credencial de bloqueio do dispositivo para acessar os dados financeiros."
                        else -> "Configure um PIN, padrão ou senha nas configurações do Android para proteger o Prumo."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (appLockError != null) {
                    Text(
                        appLockError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (securitySettingsLoaded) {
                    Button(
                        onClick = {
                            if (deviceHasSecureLock) requestAppUnlock()
                            else activity.startActivity(Intent(AndroidSettings.ACTION_SECURITY_SETTINGS))
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(if (deviceHasSecureLock) "Desbloquear" else "Configurar bloqueio")
                    }
                }
            }
        }
    }

}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
