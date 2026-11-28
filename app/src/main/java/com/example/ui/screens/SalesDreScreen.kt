package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FixedExpense
import com.example.model.PrumoConstants
import com.example.model.SaleRecord
import com.example.model.SellerProfile
import com.example.model.SellerRankingItem
import com.example.ui.components.DreStructureBar
import com.example.ui.components.PrumoEmptyState
import com.example.ui.theme.PrumoAlertYellow
import com.example.ui.theme.PrumoAlertYellowBg
import com.example.ui.theme.PrumoAlertYellowInk
import com.example.ui.theme.PrumoDanger
import com.example.ui.theme.PrumoDarkAlertYellowBg
import com.example.ui.theme.PrumoGreen
import com.example.ui.theme.PrumoGreenSoft
import com.example.ui.viewmodel.PrumoDashboardData
import com.example.util.formatBrlFromCents
import com.example.util.formatDateShort
import com.example.util.formatPercent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDreScreen(
    data: PrumoDashboardData,
    hideValues: Boolean = false,
    onShiftMonth: (Int) -> Unit,
    onAddSale: () -> Unit,
    onEditSale: (SaleRecord) -> Unit,
    onDeleteSale: (Long) -> Unit,
    onAddSeller: () -> Unit,
    onEditSeller: (SellerProfile) -> Unit,
    onDeleteSeller: (Long) -> Unit,
    onAddFixedExpense: () -> Unit,
    onEditFixedExpense: (FixedExpense) -> Unit,
    onDeleteFixedExpense: (Long) -> Unit,
    onExportDrePdf: () -> Unit,
    onExportCashCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("DRE Gerencial", "Vendas & Metas", "Despesas Fixas")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("sales_dre_screen")
    ) {
        SecondaryTabRow(selectedTabIndex = subTab) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = subTab == idx,
                    onClick = { subTab = idx },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthFilterHeader(
                    monthLabel = data.selectedMonth.formattedLabel(),
                    onPrev = { onShiftMonth(-1) },
                    onNext = { onShiftMonth(1) },
                    actionLabel = when (subTab) {
                        2 -> "Despesa"
                        else -> "Venda"
                    },
                    onAction = if (subTab == 2) onAddFixedExpense else onAddSale
                )
            }

            if (subTab == 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onExportDrePdf, modifier = Modifier.weight(1f)) {
                            Text("Exportar DRE PDF")
                        }
                        OutlinedButton(onClick = onExportCashCsv, modifier = Modifier.weight(1f)) {
                            Text("Exportar Caixa CSV")
                        }
                    }
                }
            }

            when (subTab) {
                0 -> {
                    item { DreSummaryCard(data = data, hideValues = hideValues) }
                }

                1 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ranking de Vendedores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = onAddSeller,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" Vendedor")
                            }
                        }
                    }

                    if (data.sellerRanking.isEmpty()) {
                        item {
                            PrumoEmptyState(
                                icon = Icons.Filled.PointOfSale,
                                title = "Sem vendedores ou vendas no mês",
                                subtitle = "Cadastre vendedores com meta mensal ou registre vendas em ${data.selectedMonth.formattedLabel()}.",
                                actionLabel = "Cadastrar vendedor",
                                onAction = onAddSeller
                            )
                        }
                    } else {
                        items(data.sellerRanking, key = { it.sellerName }) { rank ->
                            val sellerProfile = data.sellers.firstOrNull { it.id == rank.sellerId }
                            SellerRankingCard(
                                item = rank,
                                onEdit = sellerProfile?.let { prof -> { onEditSeller(prof) } },
                                onDelete = sellerProfile?.let { prof -> { onDeleteSeller(prof.id) } },
                                hideValues = hideValues
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Vendas Registradas (${data.monthSales.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    if (data.monthSales.isEmpty()) {
                        item {
                            PrumoEmptyState(
                                icon = Icons.Filled.PointOfSale,
                                title = "Nenhuma venda em ${data.selectedMonth.formattedLabel()}",
                                subtitle = "Cada venda atualiza o ranking e alimenta a DRE automaticamente.",
                                actionLabel = "Registrar venda",
                                onAction = onAddSale
                            )
                        }
                    } else {
                        items(data.monthSales, key = { it.id }) { sale ->
                            SaleRecordCard(
                                sale = sale,
                                onEdit = { onEditSale(sale) },
                                onDelete = { onDeleteSale(sale.id) },
                                hideValues = hideValues
                            )
                        }
                    }
                }

                2 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Despesas Fixas Operacionais",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total no mês: ${privacyMoney(data.dreMonth.fixedExpensesCents, hideValues)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onAddFixedExpense,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" Nova")
                            }
                        }
                    }

                    if (data.monthFixedExpenses.isEmpty()) {
                        item {
                            PrumoEmptyState(
                                icon = Icons.Filled.ReceiptLong,
                                title = "Sem despesas fixas em ${data.selectedMonth.formattedLabel()}",
                                subtitle = "Lance Aluguel, Energia/Internet, Folha, Contador, Marketing ou Sistemas para apurar o Lucro Líquido.",
                                actionLabel = "Nova despesa fixa",
                                onAction = onAddFixedExpense
                            )
                        }
                    } else {
                        items(data.monthFixedExpenses, key = { it.id }) { exp ->
                            FixedExpenseCard(
                                expense = exp,
                                onEdit = { onEditFixedExpense(exp) },
                                onDelete = { onDeleteFixedExpense(exp.id) },
                                hideValues = hideValues
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DreSummaryCard(data: PrumoDashboardData, hideValues: Boolean) {
    val dre = data.dreMonth
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .testTag("dre_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DRE Gerencial · ${data.selectedMonth.formattedLabel()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Resultado estimado com base nas vendas e despesas fixas cadastradas. Não representa lucro líquido contábil; não duplique despesas nesta aba e no caixa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Alíquota padrão configurada: ${formatPercent(data.settings.taxRatePercent)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DreStructureBar(dre = dre)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            DreLine("(=) Receita Bruta de Vendas", privacyMoney(dre.grossRevenueCents, hideValues), isBold = true)
            DreLine("(-) Impostos sobre Vendas", "-${privacyMoney(dre.salesTaxesCents, hideValues)}", valueColor = PrumoDanger)
            DreLine("(=) Receita Líquida", privacyMoney(dre.netRevenueCents, hideValues), isBold = true)
            DreLine("(-) CMV / CSV", "-${privacyMoney(dre.costOfGoodsCents, hideValues)}", valueColor = PrumoDanger)
            DreLine("(-) Comissões de Venda", "-${privacyMoney(dre.commissionsCents, hideValues)}", valueColor = PrumoDanger)
            DreLine(
                "(=) Margem de Contribuição (${formatPercent(dre.contributionMarginPercent)})",
                privacyMoney(dre.contributionMarginCents, hideValues),
                isBold = true,
                valueColor = if (dre.contributionMarginCents >= 0L) MaterialTheme.colorScheme.primary else PrumoDanger
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            DreLine(
                "(-) Despesas Fixas Operacionais",
                "-${privacyMoney(dre.fixedExpensesCents, hideValues)}",
                isBold = true,
                valueColor = PrumoDanger
            )
            PrumoConstants.fixedExpenseCategories.forEach { cat ->
                val catTotal = dre.fixedExpensesByCategory[cat] ?: 0L
                if (catTotal > 0L) {
                    DreLine("   • $cat", "-${privacyMoney(catTotal, hideValues)}", isSubtle = true)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            DreLine(
                "(=) Lucro Operacional (EBITDA)",
                privacyMoney(dre.operatingProfitEbitdaCents, hideValues),
                isBold = true,
                valueColor = if (dre.operatingProfitEbitdaCents >= 0L) MaterialTheme.colorScheme.primary else PrumoDanger
            )
            Surface(
                color = if (dre.netProfitCents >= 0L) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "(=) Resultado Gerencial Estimado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Margem gerencial: ${formatPercent(dre.netProfitPercent)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = privacyMoney(dre.netProfitCents, hideValues),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (dre.netProfitCents >= 0L) MaterialTheme.colorScheme.primary else PrumoDanger
                    )
                }
            }
        }
    }
}

@Composable
private fun DreLine(
    label: String,
    value: String,
    isBold: Boolean = false,
    isSubtle: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isSubtle) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isSubtle) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (isSubtle) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun SellerRankingCard(
    item: SellerRankingItem,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    hideValues: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val pct = (item.goalProgress * 100f).toDouble()

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.sellerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${item.salesCount} venda(s) · Comissão ${formatPercent(item.commissionPercent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (item.isGoalMet) PrumoGreenSoft else if (isDark) PrumoDarkAlertYellowBg else PrumoAlertYellowBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (item.isGoalMet) "META BATIDA" else "ABAIXO DA META",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isGoalMet) PrumoGreen else if (isDark) PrumoAlertYellow else PrumoAlertYellowInk,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { item.goalProgress.coerceIn(0f, 1f) },
                color = if (item.isGoalMet) PrumoGreen else PrumoAlertYellow,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vendido: ${privacyMoney(item.totalSalesCents, hideValues)} (${formatPercent(pct)})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Meta: ${privacyMoney(item.goalCents, hideValues)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comissão estimada: ${privacyMoney(item.estimatedCommissionCents, hideValues)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (onEdit != null && onDelete != null) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar vendedor")
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir vendedor", tint = PrumoDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleRecordCard(
    sale: SaleRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    hideValues: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${sale.quantity}x ${sale.description}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "${formatDateShort(sale.saleDateMillis)} · Cliente: ${sale.clientName} · Vend.: ${sale.sellerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Margem contrib.: ${privacyMoney(sale.contributionMarginCents, hideValues)} · CMV: ${privacyMoney(sale.costAmountCents, hideValues)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = privacyMoney(sale.grossAmountCents, hideValues),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar venda")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir venda", tint = PrumoDanger)
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedExpenseCard(
    expense: FixedExpense,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    hideValues: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(expense.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = formatDateShort(expense.dateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = privacyMoney(expense.amountCents, hideValues),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrumoDanger
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar despesa fixa")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir despesa fixa", tint = PrumoDanger)
                    }
                }
            }
        }
    }
}


private fun privacyMoney(cents: Long, hidden: Boolean, decimals: Boolean = true): String =
    if (hidden) "••••••" else formatBrlFromCents(cents, includeDecimals = decimals)
