package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.model.PrumoConstants
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.StockItem
import com.example.util.centsToInputText
import com.example.util.formatBrlFromCents
import com.example.util.parseBrlInputToCents

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSaleDialog(
    sellers: List<SellerProfile>,
    stockItems: List<StockItem>,
    defaultTaxPercent: Double,
    onDismiss: () -> Unit,
    onSave: (SaleRecord) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Pix") }
    var isPaid by remember { mutableStateOf(true) }
    var selectedStockItemId by remember { mutableStateOf<Long?>(null) }
    var productMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedItem = stockItems.firstOrNull { it.id == selectedStockItemId }
    val seller = sellers.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Venda rápida", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (stockItems.isNotEmpty()) {
                    Text("Produto do estoque (opcional)", style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedButton(
                            onClick = { productMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                selectedItem?.let { "${it.name} · ${formatBrlFromCents(it.unitPriceCents)}" }
                                    ?: "Selecionar produto"
                            )
                        }
                        DropdownMenu(
                            expanded = productMenuExpanded,
                            onDismissRequest = { productMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Venda sem vínculo com estoque") },
                                onClick = {
                                    selectedStockItemId = null
                                    productMenuExpanded = false
                                }
                            )
                            stockItems.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${item.name} · ${formatBrlFromCents(item.unitPriceCents)} · saldo ${item.quantity}")
                                    },
                                    onClick = {
                                        selectedStockItemId = item.id
                                        description = item.name
                                        val currentQuantity = quantityText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                                        val total = runCatching { Math.multiplyExact(item.unitPriceCents, currentQuantity) }.getOrNull()
                                        amountText = total?.let(::centsToInputText).orEmpty()
                                        productMenuExpanded = false
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(120) },
                    label = { Text("Produto ou serviço") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_sale_description")
                )
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it.take(80) },
                    label = { Text("Cliente (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { raw ->
                        quantityText = raw.filter(Char::isDigit).take(9)
                        val nextQuantity = quantityText.toLongOrNull()
                        if (selectedItem != null && nextQuantity != null && nextQuantity > 0L) {
                            val total = runCatching {
                                Math.multiplyExact(selectedItem.unitPriceCents, nextQuantity)
                            }.getOrNull()
                            amountText = total?.let(::centsToInputText).orEmpty()
                        }
                    },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_sale_quantity")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.take(20) },
                    label = { Text("Total da venda (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_sale_amount")
                )

                Text("Forma de pagamento", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrumoConstants.paymentMethods.forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }
                Text("Recebimento", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = isPaid,
                        onClick = { isPaid = true },
                        label = { Text("Já recebido") }
                    )
                    FilterChip(
                        selected = !isPaid,
                        onClick = { isPaid = false },
                        label = { Text("A receber") }
                    )
                }
                if (selectedItem == null) {
                    Text(
                        "Sem produto vinculado: o custo será registrado como R$ 0,00. Use a edição completa para informar o CMV de mercadorias ou serviços.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Disponível: ${selectedItem.quantity} ${selectedItem.unit}. O estoque será atualizado ao salvar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (errorMessage != null) {
                    Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toLongOrNull()
                    val total = parseBrlInputToCents(amountText)
                    when {
                        description.isBlank() -> errorMessage = "Informe o produto ou serviço."
                        qty == null || qty <= 0L -> errorMessage = "Informe uma quantidade válida."
                        selectedItem != null && qty > selectedItem.quantity ->
                            errorMessage = "Estoque insuficiente. Disponível: ${selectedItem.quantity}."
                        total == null || total <= 0L -> errorMessage = "Informe um total maior que zero."
                        else -> {
                            val cost = if (selectedItem == null) 0L else runCatching {
                                Math.multiplyExact(selectedItem.unitCostCents, qty)
                            }.getOrElse {
                                errorMessage = "O valor de custo excede o limite permitido."
                                return@Button
                            }
                            onSave(
                                SaleRecord(
                                    description = description.trim(),
                                    clientName = clientName.trim().ifBlank { "Cliente Balcão" },
                                    sellerName = seller?.name ?: "Balcão",
                                    quantity = qty,
                                    grossAmountCents = total,
                                    costAmountCents = cost,
                                    taxPercent = defaultTaxPercent.coerceIn(0.0, 100.0),
                                    commissionPercent = seller?.commissionPercent?.coerceIn(0.0, 100.0) ?: 5.0,
                                    saleDateMillis = System.currentTimeMillis(),
                                    stockItemId = selectedItem?.id,
                                    paymentMethod = paymentMethod,
                                    account = PrumoConstants.cashAccounts.first(),
                                    paymentStatus = if (isPaid) "PAID" else "PENDING"
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("quick_sale_save_button")
            ) {
                Text("Concluir venda", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
