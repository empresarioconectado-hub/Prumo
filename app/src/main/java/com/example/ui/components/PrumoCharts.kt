package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.DailyCashPoint
import com.example.model.DreSummary
import com.example.ui.theme.PrumoAlertYellow
import com.example.ui.theme.PrumoAlertYellowInk
import com.example.ui.theme.PrumoDanger
import com.example.ui.theme.PrumoGreen
import com.example.util.formatBrlCompact
import com.example.util.formatDayMonth
import kotlin.math.max
import kotlin.math.min

@Composable
fun CashProjectionChart(
    points: List<DailyCashPoint>,
    minAlertCents: Long,
    hideValues: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val alertColor = PrumoAlertYellow
    val dangerColor = PrumoDanger
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        if (points.size < 2) {
            Text(
                text = "Adicione lançamentos para visualizar a projeção de 30 dias.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val values = points.map { it.projectedBalanceCents.toFloat() }
        val maxVal = max(values.maxOrNull() ?: 0f, minAlertCents.toFloat())
        val minVal = min(values.minOrNull() ?: 0f, minAlertCents.toFloat())
        val range = max(1000f, maxVal - minVal)
        val paddedMin = minVal - range * 0.12f
        val paddedMax = maxVal + range * 0.12f
        val paddedRange = max(1f, paddedMax - paddedMin)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (hideValues) "Máx: ••••••" else "Máx: ${formatBrlCompact(maxVal.toLong())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hideValues) "Mín. Segurança: ••••••" else "Mín. Segurança: ${formatBrlCompact(minAlertCents)}",
                style = MaterialTheme.typography.labelSmall,
                color = PrumoAlertYellowInk
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height
            val stepX = w / (points.size - 1).coerceAtLeast(1)

            fun yFor(value: Float): Float {
                val normalized = (value - paddedMin) / paddedRange
                return h - (normalized * h)
            }

            for (i in 0..2) {
                val y = h * (i / 2f)
                drawLine(
                    color = gridColor.copy(alpha = 0.65f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val alertY = yFor(minAlertCents.toFloat()).coerceIn(0f, h)
            drawLine(
                color = alertColor,
                start = Offset(0f, alertY),
                end = Offset(w, alertY),
                strokeWidth = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            val linePath = Path()
            val fillPath = Path()
            points.forEachIndexed { idx, point ->
                val x = idx * stepX
                val y = yFor(point.projectedBalanceCents.toFloat())
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (idx == points.lastIndex) {
                    fillPath.lineTo(x, h)
                    fillPath.close()
                }
            }

            val hasNegative = points.any { it.projectedBalanceCents < 0L }
            val hasLow = points.any { it.isLowCash }
            val activeStroke = when {
                hasNegative -> dangerColor
                hasLow -> alertColor
                else -> lineColor
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        activeStroke.copy(alpha = 0.25f),
                        activeStroke.copy(alpha = 0.02f)
                    )
                )
            )

            drawPath(
                path = linePath,
                color = activeStroke,
                style = Stroke(
                    width = 2.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            val lastX = (points.size - 1) * stepX
            val lastY = yFor(points.last().projectedBalanceCents.toFloat())
            drawCircle(color = activeStroke, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
            drawCircle(color = Color.White, radius = 2.2.dp.toPx(), center = Offset(lastX, lastY))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Hoje (${formatDayMonth(points.first().dateMillis)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+15 dias",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+30 dias (${formatDayMonth(points.last().dateMillis)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DreStructureBar(
    dre: DreSummary,
    modifier: Modifier = Modifier
) {
    val gross = max(1L, dre.grossRevenueCents).toFloat()
    val taxesRatio = (dre.salesTaxesCents / gross).coerceIn(0f, 1f)
    val cmvRatio = ((dre.costOfGoodsCents + dre.commissionsCents) / gross).coerceIn(0f, 1f)
    val fixedRatio = (dre.fixedExpensesCents / gross).coerceIn(0f, 1f)
    val profitRatio = (dre.netProfitCents.coerceAtLeast(0L) / gross).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                val w = size.width
                val h = size.height
                var cursor = 0f

                fun segment(ratio: Float, color: Color) {
                    val segW = (w * ratio).coerceAtMost(w - cursor)
                    if (segW > 0f) {
                        drawRect(color = color, topLeft = Offset(cursor, 0f), size = Size(segW, h))
                        cursor += segW
                    }
                }

                segment(profitRatio, PrumoGreen)
                segment(fixedRatio, PrumoAlertYellow)
                segment(cmvRatio, Color(0xFF55A77A))
                segment(taxesRatio, PrumoDanger)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendDot("Lucro", PrumoGreen)
            LegendDot("Fixas", PrumoAlertYellow)
            LegendDot("CMV/Com.", Color(0xFF55A77A))
            LegendDot("Impostos", PrumoDanger)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PrumoEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(text = actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
