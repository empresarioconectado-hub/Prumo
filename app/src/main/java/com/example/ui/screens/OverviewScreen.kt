package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StockStatus
import com.example.ui.components.CashProjectionChart
import com.example.ui.theme.PrumoAlertYellow
import com.example.ui.theme.PrumoAlertYellowBg
import com.example.ui.theme.PrumoAlertYellowInk
import com.example.ui.theme.PrumoDanger
import com.example.ui.theme.PrumoDangerBg
import com.example.ui.theme.PrumoDarkAlertYellowBg
import com.example.ui.theme.PrumoDarkDangerBg
import com.example.ui.theme.PrumoGreen
import com.example.ui.theme.PrumoGreenDark
import com.example.ui.viewmodel.PrumoDashboardData
import com.example.util.formatBrlFromCents
import com.example.util.formatPercent

@Composable
fun OverviewScreen(
    data: PrumoDashboardData,
    hideValues: Boolean = false,
    onQuickIncome: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickSale: () -> Unit,
    onQuickStockMove: () -> Unit,
    onOpenCashTab: () -> Unit,
    onOpenStockTab: () -> Unit,
    onOpenDreTab: () -> Unit,
    onLoadSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val hasOutOfStock = data.criticalOrReorderItems.any { it.status == StockStatus.OUT_OF_STOCK }
    val hasStockAlerts = data.criticalOrReorderItems.isNotEmpty()
    val hasAnyAlert = data.isLowCashAlert || hasStockAlerts

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("overview_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!data.hasAnyData) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                            text = "Vamos organizar seu negócio?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Seu painel ainda está vazio. Comece registrando uma entrada ou sua primeira venda. Você pode incluir os demais dados conforme a rotina do negócio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onQuickIncome,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("overview_first_income_button")
                        ) {
                            Text("Registrar primeira entrada", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onQuickSale,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("overview_first_sale_button")
                        ) {
                            Text("Registrar primeira venda")
                        }
                        OutlinedButton(
                            onClick = onLoadSampleData,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("overview_load_sample_button")
                        ) {
                            Text("Explorar com dados de exemplo")
                        }
                    }
                }
            }
        }

        }
        // Banner de Alerta Inteligente
        item {
            if (hasAnyAlert) {
                val isCritical = data.minProjected30dCents < 0L || data.realizedBalanceCents < 0L || hasOutOfStock
                val alertBg = when {
                    isCritical && isDark -> PrumoDarkDangerBg
                    isCritical -> PrumoDangerBg
                    isDark -> PrumoDarkAlertYellowBg
                    else -> PrumoAlertYellowBg
                }
                val alertBorder = if (isCritical) PrumoDanger else PrumoAlertYellow
                val alertTitleColor = if (isCritical) {
                    if (isDark) Color(0xFFFFB4AB) else PrumoDanger
                } else {
                    if (isDark) PrumoAlertYellow else PrumoAlertYellowInk
                }

                val reasons = buildList {
                    if (data.isLowCashAlert) {
                        add(
                            "Projeção de 30 dias atinge ${privacyMoney(data.minProjected30dCents, hideValues)} " +
                                "(abaixo do mínimo de segurança ${privacyMoney(data.settings.minCashAlertCents, hideValues)})."
                        )
                    }
                    if (hasStockAlerts) {
                        val zeroCount = data.criticalOrReorderItems.count { it.status == StockStatus.OUT_OF_STOCK }
                        val reorderCount = data.criticalOrReorderItems.size - zeroCount
                        add(
                            buildString {
                                if (zeroCount > 0) append("$zeroCount SKU(s) zerado(s)")
                                if (zeroCount > 0 && reorderCount > 0) append(" e ")
                                if (reorderCount > 0) append("$reorderCount item(ns) no ponto de pedido")
                                append(".")
                            }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = alertBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, alertBorder, RoundedCornerShape(16.dp))
                        .testTag("smart_alert_banner")
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
                                contentDescription = "Alerta de gestão",
                                tint = alertTitleColor
                            )
                            Text(
                                text = if (isCritical) "Alerta Crítico de Gestão" else "Atenção Antecipada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = alertTitleColor
                            )
                        }
                        reasons.forEach { reason ->
                            Text(
                                text = "• $reason",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (data.isLowCashAlert) {
                                OutlinedButton(
                                    onClick = onOpenCashTab,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Ver Projeção 30d")
                                }
                            }
                            if (hasStockAlerts) {
                                OutlinedButton(
                                    onClick = onOpenStockTab,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Repor Estoque")
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .testTag("healthy_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Operação saudável",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Caixa e Estoque sob controle",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Projeção de 30 dias acima do mínimo (${privacyMoney(data.settings.minCashAlertCents, hideValues)}) e nenhum SKU crítico.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Hero Saldo Atual Realizado + Saldo Final Previsto
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrumoGreenDark, PrumoGreen)
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Saldo Atual Realizado",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = privacyMoney(data.realizedBalanceCents, hideValues),
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White,
                                modifier = Modifier.testTag("overview_realized_balance")
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = data.selectedMonth.formattedLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeroMiniMetric(
                            label = "Saldo Final Previsto",
                            value = privacyMoney(data.monthPredictedEndBalanceCents, hideValues),
                            modifier = Modifier.weight(1f)
                        )
                        HeroMiniMetric(
                            label = "A Receber / A Pagar",
                            value = "${privacyMoney(data.monthPendingIncomeCents, hideValues, false)} / ${privacyMoney(data.monthPendingExpenseCents, hideValues, false)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4 Cards Executivos: Faturamento Bruto & Lucro Líquido (junto com Saldo Realizado & Saldo Previsto acima)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExecutiveKpiCard(
                    title = "Faturamento Bruto",
                    value = privacyMoney(data.dreMonth.grossRevenueCents, hideValues),
                    subtitle = "${data.monthSales.size} venda(s) no mês",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_gross_revenue"),
                    onClick = onOpenDreTab
                )
                ExecutiveKpiCard(
                    title = "Lucro Líquido do Mês",
                    value = privacyMoney(data.dreMonth.netProfitCents, hideValues),
                    subtitle = "Margem ${formatPercent(data.dreMonth.netProfitPercent)}",
                    valueColor = if (data.dreMonth.netProfitCents >= 0L) MaterialTheme.colorScheme.primary else PrumoDanger,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_net_profit"),
                    onClick = onOpenDreTab
                )
            }
        }

        // Atalhos rápidos (4 ações obrigatórias)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Atalhos rápidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.AddCircleOutline,
                        label = "Nova Entrada",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onQuickIncome,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shortcut_new_income")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.RemoveCircleOutline,
                        label = "Nova Saída",
                        tint = PrumoDanger,
                        onClick = onQuickExpense,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shortcut_new_expense")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.PointOfSale,
                        label = "Registrar Venda",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onQuickSale,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shortcut_new_sale")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.SwapVert,
                        label = "Movimentar Estoque",
                        tint = PrumoAlertYellowInk,
                        onClick = onQuickStockMove,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shortcut_move_stock")
                    )
                }
            }
        }

        // Gráfico Projeção 30 Dias
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Simulador de Caixa · 30 Dias",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mínimo projetado: ${privacyMoney(data.minProjected30dCents, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenCashTab,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Dia a dia")
                        }
                    }
                    CashProjectionChart(
                        points = data.cashProjection30Days,
                        minAlertCents = data.settings.minCashAlertCents
                    )
                }
            }
        }

        // Resumo das 3 Planilhas Prumo
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Os 3 Pilares da Gestão",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                PillarNavCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Fluxo de Caixa & Projeção",
                    metric = "${data.monthCashEntries.size} lançamentos em ${data.selectedMonth.formattedLabel()}",
                    onClick = onOpenCashTab
                )
                PillarNavCard(
                    icon = Icons.Filled.Inventory2,
                    title = "Controle de Estoque",
                    metric = "${data.stockItems.size} SKUs · ${data.criticalOrReorderItems.size} alerta(s) de compra",
                    onClick = onOpenStockTab
                )
                PillarNavCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Vendas, Metas & DRE Gerencial",
                    metric = "EBITDA ${privacyMoney(data.dreMonth.operatingProfitEbitdaCents, hideValues)}",
                    onClick = onOpenDreTab
                )
            }
        }
    }
}

@Composable
private fun HeroMiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExecutiveKpiCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PillarNavCard(
    icon: ImageVector,
    title: String,
    metric: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(metric, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Abrir $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


private fun privacyMoney(cents: Long, hidden: Boolean, decimals: Boolean = true): String =
    if (hidden) "••••••" else formatBrlFromCents(cents, includeDecimals = decimals)
