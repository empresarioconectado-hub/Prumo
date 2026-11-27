package com.example.ui

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
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
import kotlinx.coroutines.launch

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
    val context = LocalContext.current.applicationContext as Application
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val backupRepository = remember(context) {
        PrumoRepository(PrumoDatabase.getInstance(context).prumoDao(), SettingsDataStore(context))
    }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            try {
                val json = backupRepository.exportBackupJson()
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Não foi possível abrir o arquivo de destino.")
                output.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                snackbarHostState.showSnackbar("Backup exportado com sucesso.")
            } catch (error: Exception) {
                snackbarHostState.showSnackbar("Falha ao exportar backup: ${error.message}")
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Não foi possível ler o arquivo selecionado.")
                backupRepository.restoreBackupJson(json)
                snackbarHostState.showSnackbar("Backup restaurado. Os dados locais foram substituídos.")
            } catch (error: Exception) {
                snackbarHostState.showSnackbar("Falha ao restaurar backup: ${error.message}")
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
                                onDeleteItem = viewModel::deleteStockItem
                            )
                        }
                        composable(ROUTE_SALES_DRE) {
                            SalesDreScreen(
                                data = dashboardData,
                                onShiftMonth = viewModel::shiftMonth,
                                onAddSale = { activeDialog = ActiveDialog.SaleForm(null) },
                                onEditSale = { activeDialog = ActiveDialog.SaleForm(it) },
                                onDeleteSale = viewModel::deleteSale,
                                onAddSeller = { activeDialog = ActiveDialog.SellerForm(null) },
                                onEditSeller = { activeDialog = ActiveDialog.SellerForm(it) },
                                onDeleteSeller = viewModel::deleteSeller,
                                onAddFixedExpense = { activeDialog = ActiveDialog.FixedExpenseForm(null) },
                                onEditFixedExpense = { activeDialog = ActiveDialog.FixedExpenseForm(it) },
                                onDeleteFixedExpense = viewModel::deleteFixedExpense
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
                onExportBackup = { exportBackupLauncher.launch("prumo-backup-${System.currentTimeMillis()}.json") },
                onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "text/json")) }
            )
        }
    }
}
