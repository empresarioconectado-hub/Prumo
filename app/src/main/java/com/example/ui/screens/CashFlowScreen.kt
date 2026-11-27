package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CashEntry
import com.example.model.CashEntryStatus
import com.example.model.CashEntryType
import com.example.model.DailyCashPoint
import com.example.model.PrumoConstants
import com.example.ui.components.CashProjectionChart
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
import com.example.util.formatDayMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashFlowScreen(
    data: PrumoDashboardData,
    onShiftMonth: (Int) -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (CashEntry) -> Unit,
    onTogglePaid: (CashEntry) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by remember { mutableStateOf<CashEntryStatus?>(null) }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var showAll30Days by remember { mutableStateOf(false) }

    val filteredEntries = remember(data.monthCashEntries, selectedStatus, selectedAccount) {
        data.monthCashEntries.filter { entry ->
            val okStatus = selectedStatus == null || entry.entryStatus == selectedStatus
            val okAccount = selectedAccount == null || entry.account == selectedAccount
            okStatus && okAccount
        }
    }

    val displayedProjectionDays = remember(data.cashProjection30Days, showAll30Days) {
        if (showAll30Days) data.cashProjection30Days else data.cashProjection30Days.take(7)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("cash_flow_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filtro por Mês/Ano
        item {
            MonthFilterHeader(
                monthLabel = data.selectedMonth.formattedLabel(),
                onPrev = { onShiftMonth(-1) },
                onNext = { onShiftMonth(1) },
                actionLabel = "Lançar",
                onAction = onAddEntry
            )
        }

        // Resumo do Fluxo de Caixa (Saldo Inicial, Entradas, Saídas, A Receber, A Pagar, Pró-labore)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CashMetricTile("Saldo Inicial", formatBrlFromCents(data.settings.initialBalanceCents), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    CashMetricTile("Total Entradas", formatBrlFromCents(data.monthPaidIncomeCents), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    CashMetricTile("Total Saídas", formatBrlFromCents(data.monthPaidExpenseCents), PrumoDanger, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CashMetricTile("A Receber", formatBrlFromCents(data.monthPendingIncomeCents), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    CashMetricTile("A Pagar", formatBrlFromCents(data.monthPendingExpenseCents), PrumoDanger, Modifier.weight(1f))
                    CashMetricTile("Pró-labore", formatBrlFromCents(data.monthPartnerWithdrawalsCents), PrumoAlertYellowInk, Modifier.weight(1f))
                }
            }
        }

        // Simulador / Projeção de Caixa (30 dias) - Gráfico + Lista dia a dia
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
                            Text("Projeção de Caixa (30 dias)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Mínimo de segurança: ${formatBrlFromCents(data.settings.minCashAlertCents)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showAll30Days = !showAll30Days }) {
                            Text(if (showAll30Days) "Ver 7 dias" else "Ver 30 dias")
                        }
                    }

                    CashProjectionChart(
                        points = data.cashProjection30Days,
                        minAlertCents = data.settings.minCashAlertCents
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Cabeçalho da tabela dia a dia
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Data", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.75f))
                        Text("Entradas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("Saídas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("Saldo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.1f))
                        Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        displayedProjectionDays.forEach { day ->
                            DailyProjectionRow(day = day)
                        }
                    }
                }
            }
        }

        // Filtros de Status e Conta
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Lançamentos do Caixa (${filteredEntries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("Todos status") }
                    )
                    CashEntryStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = if (selectedStatus == st) null else st },
                            label = { Text(st.label) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedAccount == null,
                        onClick = { selectedAccount = null },
                        label = { Text("Todas contas") }
                    )
                    PrumoConstants.cashAccounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccount == acc,
                            onClick = { selectedAccount = if (selectedAccount == acc) null else acc },
                            label = { Text(acc) }
                        )
                    }
                }
            }
        }

        if (filteredEntries.isEmpty()) {
            item {
                PrumoEmptyState(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Sem lançamentos neste filtro",
                    subtitle = "Registre entradas ou saídas em ${data.selectedMonth.formattedLabel()} para acompanhar o saldo.",
                    actionLabel = "Novo lançamento",
                    onAction = onAddEntry
                )
            }
        } else {
            items(filteredEntries, key = { it.id }) { entry ->
                CashEntryCard(
                    entry = entry,
                    onTogglePaid = { onTogglePaid(entry) },
                    onEdit = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry.id) }
                )
            }
        }
    }
}

@Composable
fun MonthFilterHeader(
    monthLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
                }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = onNext, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
                }
            }
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" $actionLabel", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CashMetricTile(
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
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyProjectionRow(day: DailyCashPoint) {
    val isDark = isSystemInDarkTheme()
    val isNegative = day.projectedBalanceCents < 0L
    val badgeBg = when {
        isNegative && isDark -> PrumoDarkDangerBg
        isNegative -> PrumoDangerBg
        day.isLowCash && isDark -> PrumoDarkAlertYellowBg
        day.isLowCash -> PrumoAlertYellowBg
        else -> PrumoGreenSoft
    }
    val badgeTextColor = when {
        isNegative -> PrumoDanger
        day.isLowCash -> if (isDark) PrumoAlertYellow else PrumoAlertYellowInk
        else -> PrumoGreen
    }
    val badgeLabel = if (day.isLowCash || isNegative) "CAIXA BAIXO" else "OK"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDayMonth(day.dateMillis),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.75f)
        )
        Text(
            text = if (day.expectedInflowCents > 0L) "+${formatBrlFromCents(day.expectedInflowCents, false)}" else "—",
            style = MaterialTheme.typography.bodySmall,
            color = if (day.expectedInflowCents > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (day.expectedOutflowCents > 0L) "-${formatBrlFromCents(day.expectedOutflowCents, false)}" else "—",
            style = MaterialTheme.typography.bodySmall,
            color = if (day.expectedOutflowCents > 0L) PrumoDanger else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatBrlFromCents(day.projectedBalanceCents, false),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isNegative) PrumoDanger else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            color = badgeBg,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeTextColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun CashEntryCard(
    entry: CashEntry,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isIncome = entry.entryType == CashEntryType.INCOME
    val isPending = entry.entryStatus == CashEntryStatus.PENDING

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
                    Text(entry.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${formatDateShort(entry.dateMillis)} · ${entry.category} · ${entry.paymentMethod} · ${entry.account}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isIncome) "+" else "-"}${formatBrlFromCents(entry.amountCents)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) MaterialTheme.colorScheme.primary else PrumoDanger
                    )
                    Surface(
                        color = if (isPending) {
                            if (isDark) PrumoDarkAlertYellowBg else PrumoAlertYellowBg
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = entry.entryStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPending) {
                                if (isDark) PrumoAlertYellow else PrumoAlertYellowInk
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPending) {
                    OutlinedButton(
                        onClick = onTogglePaid,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mark_paid_button_${entry.id}")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Marcar Pago", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    TextButton(onClick = onTogglePaid, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Text("Voltar p/ Pendente")
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar lançamento")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir lançamento", tint = PrumoDanger)
                    }
                }
            }
        }
    }
}
