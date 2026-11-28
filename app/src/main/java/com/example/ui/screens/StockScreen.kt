package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StockItem
import com.example.model.StockStatus
import com.example.ui.components.PrumoEmptyState
import com.example.ui.theme.PrumoAlertYellow
import com.example.ui.theme.PrumoAlertYellowBg
import com.example.ui.theme.PrumoAlertYellowInk
import com.example.ui.theme.PrumoDanger
import com.example.ui.theme.PrumoDangerBg
import com.example.ui.theme.PrumoDarkAlertYellowBg
import com.example.ui.theme.PrumoDarkDangerBg
import com.example.ui.theme.PrumoGreen
import com.example.ui.theme.PrumoGreenSoft
import com.example.ui.viewmodel.PrumoDashboardData
import com.example.util.formatBrlFromCents
import com.example.util.formatDateShort
import java.util.Locale

private val stockPtBr = Locale.Builder().setLanguage("pt").setRegion("BR").build()

@Composable
fun StockScreen(
    data: PrumoDashboardData,
    onAddItem: () -> Unit,
    onEditItem: (StockItem) -> Unit,
    onMoveItem: (StockItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    hideValues: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("stock_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Indicadores no topo
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Controle de Estoque", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Ponto de pedido e sugestão de compra automáticos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddItem,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_stock_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Produto", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StockKpiTile(
                        title = "Valor em Estoque (Custo)",
                        value = privacyMoney(data.totalStockCostCents, hideValues),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    StockKpiTile(
                        title = "Potencial de Venda",
                        value = privacyMoney(data.totalStockPotentialSaleCents, hideValues),
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StockKpiTile(
                        title = "Total de SKUs",
                        value = "${data.stockItems.size} itens",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    StockKpiTile(
                        title = "Alertas de Reposição",
                        value = "${data.criticalOrReorderItems.size} (${privacyMoney(data.estimatedRestockCostCents, hideValues, false)})",
                        valueColor = if (data.criticalOrReorderItems.isNotEmpty()) PrumoDanger else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Painel de Alertas de Compra (Críticos Primeiro)
        if (data.criticalOrReorderItems.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDark) PrumoDarkAlertYellowBg else PrumoAlertYellowBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, PrumoAlertYellow, RoundedCornerShape(18.dp))
                        .testTag("stock_purchase_alerts_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "Alertas de compra",
                                tint = if (isDark) PrumoAlertYellow else PrumoAlertYellowInk
                            )
                            Text(
                                text = "Painel de Alertas de Compra",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) PrumoAlertYellow else PrumoAlertYellowInk
                            )
                        }
                        Text(
                            text = "Investimento estimado para repor todos os itens críticos: ${privacyMoney(data.estimatedRestockCostCents, hideValues)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        data.criticalOrReorderItems.forEach { item ->
                            PurchaseAlertRow(item = item, hideValues = hideValues, onBuyClick = { onMoveItem(item) })
                        }
                    }
                }
            }
        }

        if (data.stockItems.isEmpty()) {
            item {
                PrumoEmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = "Nenhum SKU cadastrado",
                    subtitle = "Cadastre produtos para calcular Ponto de Pedido, Giro de Estoque e Sugestão de Compra automaticamente.",
                    actionLabel = "Cadastrar produto",
                    onAction = onAddItem
                )
            }
        } else {
            item {
                Text(
                    text = "Catálogo & Movimentações (${data.stockItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(data.stockItems, key = { it.id }) { item ->
                StockItemCard(
                    item = item,
                    onMove = { onMoveItem(item) },
                    onEdit = { onEditItem(item) },
                    onDelete = { onDeleteItem(item.id) },
                    hideValues = hideValues
                )
            }
        }

        item {
            Text("Histórico de movimentações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (data.stockMovements.isEmpty()) {
            item { Text("Nenhuma movimentação registrada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(data.stockMovements, key = { "movement-${it.id}" }) { movement ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        val movementLabel = when (movement.movementType) {
                            "PURCHASE" -> "Compra"
                            "SALE" -> "Venda / saída"
                            "LOSS" -> "Perda"
                            "ADJUSTMENT" -> "Ajuste / estorno"
                            else -> movement.movementType
                        }
                        Text("${movement.productName} · ${movement.sku}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("${movementLabel} · Quantidade: ${movement.quantity} · ${movement.referenceNote}", style = MaterialTheme.typography.bodySmall)
                        Text(formatDateShort(movement.dateMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockKpiTile(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PurchaseAlertRow(
    item: StockItem,
    hideValues: Boolean,
    onBuyClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isZero = item.status == StockStatus.OUT_OF_STOCK
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isZero) PrumoDanger else PrumoAlertYellow, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isZero) {
                            if (isDark) PrumoDarkDangerBg else PrumoDangerBg
                        } else {
                            if (isDark) PrumoDarkAlertYellowBg else PrumoAlertYellowBg
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.status.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isZero) PrumoDanger else if (isDark) PrumoAlertYellow else PrumoAlertYellowInk,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${item.sku} · ${item.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Saldo: ${item.quantity} ${item.unit} · Sugestão compra: ${item.suggestedPurchaseQty} ${item.unit} (${privacyMoney(item.estimatedPurchaseCostCents, hideValues)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onBuyClick, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCartCheckout,
                    contentDescription = "Registrar entrada de compra",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StockItemCard(
    item: StockItem,
    onMove: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    hideValues: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val badgeBg = when (item.status) {
        StockStatus.OUT_OF_STOCK -> if (isDark) PrumoDarkDangerBg else PrumoDangerBg
        StockStatus.REORDER -> if (isDark) PrumoDarkAlertYellowBg else PrumoAlertYellowBg
        StockStatus.NORMAL -> PrumoGreenSoft
    }
    val badgeTextColor = when (item.status) {
        StockStatus.OUT_OF_STOCK -> PrumoDanger
        StockStatus.REORDER -> if (isDark) PrumoAlertYellow else PrumoAlertYellowInk
        StockStatus.NORMAL -> PrumoGreen
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${item.sku} · ${item.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${item.category} · Custo ${privacyMoney(item.unitCostCents, hideValues)} · Venda ${privacyMoney(item.unitPriceCents, hideValues)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = item.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saldo: ${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.quantity <= 0L) PrumoDanger else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ponto pedido: ${item.reorderPoint} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Giro: ${String.format(stockPtBr, "%.1fx", item.turnoverRatio)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.suggestedPurchaseQty > 0L || item.lossUnitsCount > 0L) {
                Text(
                    text = buildString {
                        if (item.suggestedPurchaseQty > 0L) {
                            append("Sugestão compra: ${item.suggestedPurchaseQty} ${item.unit} (${formatBrlFromCents(item.estimatedPurchaseCostCents)})")
                        }
                        if (item.lossUnitsCount > 0L) {
                            if (isNotEmpty()) append(" · ")
                            append("Perdas: ${item.lossUnitsCount} ${item.unit}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.suggestedPurchaseQty > 0L) PrumoDanger else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onMove,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Movimentar")
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar produto")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir produto", tint = PrumoDanger)
                    }
                }
            }
        }
    }
}


private fun privacyMoney(cents: Long, hidden: Boolean, decimals: Boolean = true): String =
    if (hidden) "••••••" else formatBrlFromCents(cents, includeDecimals = decimals)
