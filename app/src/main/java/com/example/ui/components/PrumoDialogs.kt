package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BusinessSettings
import com.example.model.CashEntry
import com.example.model.CashEntryStatus
import com.example.model.CashEntryType
import com.example.model.FixedExpense
import com.example.model.PrumoConstants
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem
import com.example.model.StockMovementType
import com.example.ui.theme.PrumoDanger
import com.example.ui.theme.PrumoGreen
import com.example.util.centsToInputText
import com.example.util.formatBrlFromCents
import com.example.util.formatDateShort
import com.example.util.parseBrlInputToCents
import kotlin.math.ceil

@Composable
fun QuickEntryMenuDialog(
    onDismiss: () -> Unit,
    onNewIncome: () -> Unit,
    onNewExpense: () -> Unit,
    onNewSale: () -> Unit,
    onMoveStock: () -> Unit,
    onNewFixedExpense: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lançamento rápido", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickMenuOption(Icons.Filled.ArrowUpward, "Nova Entrada", "Recebimentos, vendas à vista, aportes", PrumoGreen) {
                    onDismiss(); onNewIncome()
                }
                QuickMenuOption(Icons.Filled.ArrowDownward, "Nova Saída", "Fornecedores, pró-labore, impostos", PrumoDanger) {
                    onDismiss(); onNewExpense()
                }
                QuickMenuOption(Icons.Filled.PointOfSale, "Registrar Venda", "Alimenta a DRE e metas do vendedor", PrumoGreen) {
                    onDismiss(); onNewSale()
                }
                QuickMenuOption(Icons.Filled.Inventory2, "Movimentar Estoque", "Compra (+), venda (−) ou perda", PrumoGreen) {
                    onDismiss(); onMoveStock()
                }
                QuickMenuOption(Icons.Filled.ReceiptLong, "Despesa Fixa (DRE)", "Aluguel, folha, energia, sistemas", MaterialTheme.colorScheme.onSurface) {
                    onDismiss(); onNewFixedExpense()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun QuickMenuOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = title, tint = tint)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CashEntryDialog(
    existing: CashEntry?,
    initialType: CashEntryType = CashEntryType.INCOME,
    onDismiss: () -> Unit,
    onSave: (CashEntry) -> Unit
) {
    var type by remember { mutableStateOf(existing?.entryType ?: initialType) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var amountText by remember { mutableStateOf(existing?.let { centsToInputText(it.amountCents) } ?: "") }
    var category by remember {
        mutableStateOf(
            existing?.category
                ?: if (initialType == CashEntryType.INCOME) PrumoConstants.incomeCategories.first()
                else PrumoConstants.expenseCategories.first()
        )
    }
    var account by remember { mutableStateOf(existing?.account ?: PrumoConstants.cashAccounts.first()) }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: PrumoConstants.paymentMethods.first()) }
    var status by remember { mutableStateOf(existing?.entryStatus ?: CashEntryStatus.PAID) }
    var dateMillis by remember { mutableStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val categories = if (type == CashEntryType.INCOME) PrumoConstants.incomeCategories else PrumoConstants.expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Lançamento no Caixa" else "Editar Lançamento",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CashEntryType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                val list = if (t == CashEntryType.INCOME) PrumoConstants.incomeCategories else PrumoConstants.expenseCategories
                                if (category !in list) category = list.first()
                            },
                            label = { Text(t.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(120) },
                    label = { Text("Descrição") },
                    placeholder = { Text("Ex.: Venda balcão / Fornecedor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cash_description_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Valor (R$)") },
                        placeholder = { Text("1500,00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("cash_amount_input")
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.CenterVertically).height(56.dp)
                    ) {
                        Text(formatDateShort(dateMillis))
                    }
                }

                Text("Categoria", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Text("Conta", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrumoConstants.cashAccounts.forEach { acc ->
                        FilterChip(
                            selected = account == acc,
                            onClick = { account = acc },
                            label = { Text(acc) }
                        )
                    }
                }

                Text("Forma de Pagamento", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrumoConstants.paymentMethods.forEach { m ->
                        FilterChip(
                            selected = paymentMethod == m,
                            onClick = { paymentMethod = m },
                            label = { Text(m) }
                        )
                    }
                }

                Text("Status", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CashEntryStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = parseBrlInputToCents(amountText)
                    when {
                        description.isBlank() -> errorMsg = "Informe a descrição."
                        cents == null || cents <= 0L -> errorMsg = "Informe um valor maior que zero."
                        else -> onSave(
                            CashEntry(
                                id = existing?.id ?: 0L,
                                description = description.trim(),
                                amountCents = cents,
                                type = type.name,
                                category = category,
                                account = account,
                                paymentMethod = paymentMethod,
                                status = status.name,
                                dateMillis = dateMillis
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_cash_button")
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it + 12L * 60L * 60L * 1000L }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockItemDialog(
    existing: StockItem?,
    onDismiss: () -> Unit,
    onSave: (StockItem) -> Unit
) {
    var sku by remember { mutableStateOf(existing?.sku ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: PrumoConstants.stockCategories.first()) }
    var unit by remember { mutableStateOf(existing?.unit ?: PrumoConstants.stockUnits.first()) }
    var qtyText by remember { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var minText by remember { mutableStateOf(existing?.minStock?.toString() ?: "5") }
    var leadText by remember { mutableStateOf(existing?.leadTimeDays?.toString() ?: "3") }
    var avgDailyText by remember { mutableStateOf(existing?.avgDailySales?.toString() ?: "1.0") }
    var costText by remember { mutableStateOf(existing?.let { centsToInputText(it.unitCostCents) } ?: "") }
    var priceText by remember { mutableStateOf(existing?.let { centsToInputText(it.unitPriceCents) } ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val previewMin = minText.toLongOrNull() ?: 0L
    val previewLead = leadText.toLongOrNull() ?: 0L
    val previewAvg = avgDailyText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val previewReorder = ceil(previewAvg * previewLead).toLong() + previewMin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Novo Produto (SKU)" else "Editar Produto",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it.take(30).uppercase() },
                        label = { Text("SKU") },
                        placeholder = { Text("CAF-05") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f).testTag("stock_sku_input")
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(100) },
                        label = { Text("Nome do produto") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f).testTag("stock_name_input")
                    )
                }

                Text("Categoria", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrumoConstants.stockCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Text("Unidade", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrumoConstants.stockUnits.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter(Char::isDigit) },
                        label = { Text(if (existing == null) "Estoque inicial" else "Saldo atual") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("stock_qty_input")
                    )
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it.filter(Char::isDigit) },
                        label = { Text("Estoque mín.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Custo unit. (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Preço venda (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = avgDailyText,
                        onValueChange = { avgDailyText = it },
                        label = { Text("Venda média/dia") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = leadText,
                        onValueChange = { leadText = it.filter(Char::isDigit) },
                        label = { Text("Reposição (dias)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ponto de pedido calculado: $previewReorder $unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toLongOrNull()
                    val min = minText.toLongOrNull() ?: 0L
                    val lead = leadText.toLongOrNull() ?: 0L
                    val avg = avgDailyText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val cost = parseBrlInputToCents(costText) ?: 0L
                    val price = parseBrlInputToCents(priceText) ?: 0L

                    when {
                        sku.isBlank() -> errorMsg = "Informe o SKU."
                        name.isBlank() -> errorMsg = "Informe o nome do produto."
                        qty == null -> errorMsg = "Informe o estoque inicial/atual."
                        else -> onSave(
                            StockItem(
                                id = existing?.id ?: 0L,
                                sku = sku.trim().uppercase(),
                                name = name.trim(),
                                category = category,
                                unit = unit,
                                quantity = qty,
                                minStock = min,
                                leadTimeDays = lead,
                                avgDailySales = avg,
                                unitCostCents = cost,
                                unitPriceCents = price,
                                soldUnitsCount = existing?.soldUnitsCount ?: 0L,
                                lossUnitsCount = existing?.lossUnitsCount ?: 0L,
                                initialQuantity = existing?.initialQuantity ?: qty
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_stock_button")
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun StockMovementDialog(
    stockItems: List<StockItem>,
    initialItem: StockItem?,
    onDismiss: () -> Unit,
    onConfirm: (StockItem, StockMovementType, Long) -> Unit
) {
    var selectedItem by remember { mutableStateOf(initialItem ?: stockItems.firstOrNull()) }
    var movementType by remember { mutableStateOf(StockMovementType.PURCHASE) }
    var unitsText by remember { mutableStateOf("1") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimentar Estoque", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedItem == null) {
                    Text("Cadastre ao menos um produto antes de movimentar o estoque.")
                } else {
                    if (initialItem == null && stockItems.size > 1) {
                        Text("Selecione o produto", style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            stockItems.forEach { item ->
                                FilterChip(
                                    selected = selectedItem?.id == item.id,
                                    onClick = { selectedItem = item },
                                    label = { Text("${item.sku} · ${item.name} (${item.quantity} ${item.unit})") }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "${selectedItem!!.sku} · ${selectedItem!!.name}\nSaldo atual: ${selectedItem!!.quantity} ${selectedItem!!.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text("Tipo de movimentação", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StockMovementType.entries.filter { it != StockMovementType.ADJUSTMENT }.forEach { m ->
                            FilterChip(
                                selected = movementType == m,
                                onClick = { movementType = m },
                                label = { Text(m.label) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    OutlinedTextField(
                        value = unitsText,
                        onValueChange = { unitsText = it.filter(Char::isDigit) },
                        label = { Text("Quantidade (${selectedItem!!.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (selectedItem != null) {
                Button(onClick = {
                    val units = unitsText.toLongOrNull() ?: 0L
                    val item = selectedItem!!
                    when {
                        units <= 0L -> errorMsg = "Informe uma quantidade maior que zero."
                        movementType != StockMovementType.PURCHASE && units > item.quantity ->
                            errorMsg = "Saldo insuficiente (disponível: ${item.quantity} ${item.unit})."
                        else -> onConfirm(item, movementType, units)
                    }
                }) { Text("Confirmar") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaleRecordDialog(
    existing: SaleRecord?,
    sellers: List<SellerProfile>,
    stockItems: List<StockItem>,
    defaultTaxPercent: Double,
    onDismiss: () -> Unit,
    onSave: (SaleRecord) -> Unit
) {
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var clientName by remember { mutableStateOf(existing?.clientName ?: "Cliente Balcão") }
    var sellerName by remember { mutableStateOf(existing?.sellerName ?: sellers.firstOrNull()?.name ?: "Balcão") }
    var qtyText by remember { mutableStateOf((existing?.quantity ?: 1L).toString()) }
    var grossText by remember { mutableStateOf(existing?.let { centsToInputText(it.grossAmountCents) } ?: "") }
    var costText by remember { mutableStateOf(existing?.let { centsToInputText(it.costAmountCents) } ?: "0,00") }
    var taxText by remember { mutableStateOf((existing?.taxPercent ?: defaultTaxPercent).toString()) }
    var commText by remember {
        mutableStateOf((existing?.commissionPercent ?: sellers.firstOrNull()?.commissionPercent ?: 5.0).toString())
    }
    var selectedStockItemId by remember { mutableStateOf(existing?.stockItemId) }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: "Pix") }
    var account by remember { mutableStateOf(existing?.account ?: PrumoConstants.cashAccounts.first()) }
    var paymentStatus by remember { mutableStateOf(existing?.paymentStatus ?: "PAID") }
    var selectedUnitPriceCents by remember { mutableStateOf<Long?>(null) }
    var selectedUnitCostCents by remember { mutableStateOf<Long?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Registrar Venda" else "Editar Venda",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (stockItems.isNotEmpty()) {
                    Text("Preencher de produto do estoque (opcional)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedStockItemId == null,
                            onClick = { selectedStockItemId = null; selectedUnitPriceCents = null; selectedUnitCostCents = null },
                            label = { Text("Sem vínculo") }
                        )
                        stockItems.take(8).forEach { item ->
                            FilterChip(
                                selected = selectedStockItemId == item.id,
                                onClick = {
                                    selectedStockItemId = item.id
                                    description = item.name
                                    selectedUnitPriceCents = item.unitPriceCents
                                    selectedUnitCostCents = item.unitCostCents
                                    val q = (qtyText.toLongOrNull() ?: 1L).coerceAtLeast(1L)
                                    grossText = centsToInputText(item.unitPriceCents * q)
                                    costText = centsToInputText(item.unitCostCents * q)
                                },
                                label = { Text("${item.name} (${formatBrlFromCents(item.unitPriceCents)})") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(120) },
                    label = { Text("Produto ou serviço") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sale_description_input")
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it.take(80) },
                    label = { Text("Cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (sellers.isNotEmpty()) {
                    Text("Vendedor", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sellers.forEach { s ->
                            FilterChip(
                                selected = sellerName.equals(s.name, ignoreCase = true),
                                onClick = {
                                    sellerName = s.name
                                    commText = s.commissionPercent.toString()
                                },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = sellerName,
                    onValueChange = { sellerName = it.take(80) },
                    label = { Text("Nome do vendedor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { raw ->
                        val digits = raw.filter(Char::isDigit)
                        qtyText = digits
                        val q = digits.toLongOrNull()
                        if (q != null && q > 0 && selectedUnitPriceCents != null && selectedUnitCostCents != null) {
                            grossText = centsToInputText(selectedUnitPriceCents!! * q)
                            costText = centsToInputText(selectedUnitCostCents!! * q)
                        }
                    },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grossText,
                        onValueChange = { grossText = it },
                        label = { Text("Total bruto (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("sale_amount_input")
                    )
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("CMV / CSV (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taxText,
                        onValueChange = { taxText = it },
                        label = { Text("Imposto (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = commText,
                        onValueChange = { commText = it },
                        label = { Text("Comissão (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Forma de pagamento", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrumoConstants.paymentMethods.forEach { method ->
                        FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method }, label = { Text(method) })
                    }
                }
                Text("Conta de recebimento", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrumoConstants.cashAccounts.forEach { acc ->
                        FilterChip(selected = account == acc, onClick = { account = acc }, label = { Text(acc) })
                    }
                }
                Text("Situação do recebimento", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PAID" to "Pago", "PENDING" to "Pendente").forEach { (status, label) ->
                        FilterChip(selected = paymentStatus == status, onClick = { paymentStatus = status }, label = { Text(label) })
                    }
                }
                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = (qtyText.toLongOrNull() ?: 1L).coerceAtLeast(1L)
                    val gross = parseBrlInputToCents(grossText)
                    val cost = parseBrlInputToCents(costText) ?: 0L
                    val tax = taxText.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                    val comm = commText.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0

                    when {
                        description.isBlank() -> errorMsg = "Informe o produto ou serviço."
                        sellerName.isBlank() -> errorMsg = "Informe o vendedor."
                        gross == null || gross <= 0L -> errorMsg = "Informe o valor bruto maior que zero."
                        else -> onSave(
                            SaleRecord(
                                id = existing?.id ?: 0L,
                                description = description.trim(),
                                clientName = clientName.trim().ifBlank { "Cliente Balcão" },
                                sellerName = sellerName.trim(),
                                quantity = q,
                                grossAmountCents = gross,
                                costAmountCents = cost,
                                taxPercent = tax,
                                commissionPercent = comm,
                                saleDateMillis = existing?.saleDateMillis ?: System.currentTimeMillis(),
                                stockItemId = selectedStockItemId,
                                paymentMethod = paymentMethod,
                                account = account,
                                paymentStatus = paymentStatus
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_sale_button")
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun SellerDialog(
    existing: SellerProfile? = null,
    onDismiss: () -> Unit,
    onSave: (SellerProfile) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var goalText by remember { mutableStateOf(existing?.let { centsToInputText(it.monthlyGoalCents) } ?: "10000,00") }
    var commText by remember { mutableStateOf((existing?.commissionPercent ?: 5.0).toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Novo Vendedor e Meta" else "Editar Vendedor", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Nome do vendedor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text("Meta do período (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commText,
                    onValueChange = { commText = it },
                    label = { Text("Comissão (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val goal = parseBrlInputToCents(goalText)
                val comm = commText.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                when {
                    name.isBlank() -> errorMsg = "Informe o nome do vendedor."
                    goal == null || goal <= 0L -> errorMsg = "Informe uma meta maior que zero."
                    else -> onSave(
                        SellerProfile(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            monthlyGoalCents = goal,
                            commissionPercent = comm
                        )
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixedExpenseDialog(
    existing: FixedExpense? = null,
    onDismiss: () -> Unit,
    onSave: (FixedExpense) -> Unit
) {
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: PrumoConstants.fixedExpenseCategories.first()) }
    var amountText by remember { mutableStateOf(existing?.let { centsToInputText(it.amountCents) } ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nova Despesa Fixa (DRE)" else "Editar Despesa Fixa", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Categoria operacional", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrumoConstants.fixedExpenseCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(100) },
                    label = { Text("Descrição") },
                    placeholder = { Text("Ex.: Aluguel da loja") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor mensal (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cents = parseBrlInputToCents(amountText)
                when {
                    description.isBlank() -> errorMsg = "Informe a descrição."
                    cents == null || cents <= 0L -> errorMsg = "Informe um valor maior que zero."
                    else -> onSave(
                        FixedExpense(
                            id = existing?.id ?: 0L,
                            description = description.trim(),
                            category = category,
                            amountCents = cents,
                            dateMillis = existing?.dateMillis ?: System.currentTimeMillis()
                        )
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun BusinessSettingsDialog(
    current: BusinessSettings,
    onDismiss: () -> Unit,
    onSave: (businessName: String, initialBalanceCents: Long, minCashAlertCents: Long, taxRatePercent: Double) -> Unit,
    onLoadSampleData: () -> Unit,
    onClearAllData: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    var name by remember { mutableStateOf(current.businessName) }
    var initialText by remember { mutableStateOf(centsToInputText(current.initialBalanceCents)) }
    var minCashText by remember { mutableStateOf(centsToInputText(current.minCashAlertCents)) }
    var taxText by remember { mutableStateOf(current.taxRatePercent.toString()) }
    var confirmClear by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações & Dados", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Nome da empresa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it },
                    label = { Text("Saldo inicial (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minCashText,
                    onValueChange = { minCashText = it },
                    label = { Text("Saldo mínimo de segurança (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taxText,
                    onValueChange = { taxText = it },
                    label = { Text("Alíquota padrão de impostos (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Gerenciamento de dados offline", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) {
                    Text("Exportar backup JSON")
                }
                OutlinedButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth()) {
                    Text("Restaurar backup JSON")
                }

                OutlinedButton(
                    onClick = {
                        onLoadSampleData()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("load_sample_data_button")
                ) {
                    Text("Carregar Dados de Exemplo")
                }

                if (!confirmClear) {
                    OutlinedButton(
                        onClick = { confirmClear = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("clear_data_button")
                    ) {
                        Text("Limpar Dados para Começar do Zero")
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Tem certeza? Todos os lançamentos, estoque, vendas e despesas fixas serão apagados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { confirmClear = false }, modifier = Modifier.weight(1f)) {
                                    Text("Cancelar")
                                }
                                Button(
                                    onClick = {
                                        onClearAllData()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Apagar tudo")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val initCents = parseBrlInputToCents(initialText) ?: 0L
                val minCash = parseBrlInputToCents(minCashText)
                val tax = taxText.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 6.0
                when {
                    name.isBlank() -> errorMsg = "Informe o nome da empresa."
                    minCash == null || minCash < 0L -> errorMsg = "Saldo mínimo inválido."
                    else -> onSave(name.trim(), initCents, minCash, tax)
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
