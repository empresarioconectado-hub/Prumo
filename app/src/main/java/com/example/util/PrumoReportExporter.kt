package com.example.util

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.ui.viewmodel.PrumoDashboardData
import java.io.OutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrumoReportExporter {
    fun writeDrePdf(output: OutputStream, data: PrumoDashboardData) {
        val document = PdfDocument()
        try {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val title = Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val heading = Paint().apply { textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val body = Paint().apply { textSize = 11f; color = android.graphics.Color.DKGRAY }
        val green = Paint().apply { color = android.graphics.Color.rgb(15, 123, 79); strokeWidth = 2f }
        var y = 54f
        canvas.drawText("PRUMO GESTÃO", 40f, y, title); y += 26f
        canvas.drawText("DRE Gerencial — ${data.selectedMonth.formattedLabel()}", 40f, y, heading); y += 18f
        canvas.drawLine(40f, y, 555f, y, green); y += 24f
        val rows = listOf(
            "Receita bruta" to data.dreMonth.grossRevenueCents,
            "Impostos sobre vendas" to -data.dreMonth.salesTaxesCents,
            "Receita líquida" to data.dreMonth.netRevenueCents,
            "CMV / CSV" to -data.dreMonth.costOfGoodsCents,
            "Comissões" to -data.dreMonth.commissionsCents,
            "Margem de contribuição" to data.dreMonth.contributionMarginCents,
            "Despesas fixas" to -data.dreMonth.fixedExpensesCents,
            "Resultado operacional (EBITDA)" to data.dreMonth.operatingProfitEbitdaCents,
            "Lucro líquido gerencial" to data.dreMonth.netProfitCents
        )
        rows.forEach { (label, cents) ->
            canvas.drawText(label, 44f, y, body)
            canvas.drawText(money(cents), 420f, y, body)
            y += 25f
        }
        y += 18f
        canvas.drawText("Aviso: relatório gerencial baseado nos dados cadastrados; não substitui escrituração contábil ou documento fiscal.", 40f, y, body)
        y += 32f
        canvas.drawText("Gerado em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())}", 40f, y, body)
        document.finishPage(page)
        document.writeTo(output)
        } finally {
            document.close()
        }
    }

    fun cashEntriesCsv(data: PrumoDashboardData): String {
        val header = "Data;Descrição;Tipo;Categoria;Conta;Status;Valor (R$)"
        val rows = data.monthCashEntries.sortedBy { it.dateMillis }.map { entry ->
            listOf(
                csv(SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(entry.dateMillis))),
                csv(entry.description, protectFormula = true),
                csv(entry.entryType.label),
                csv(entry.category, protectFormula = true),
                csv(entry.account, protectFormula = true),
                csv(entry.entryStatus.label),
                BigDecimal.valueOf(entry.amountCents, 2).toPlainString()
            ).joinToString(";")
        }
        // UTF-8 BOM improves compatibility with Excel when opening Portuguese text directly.
        return "\uFEFF" + (listOf(header) + rows).joinToString("\r\n")
    }

    private fun csv(value: String, protectFormula: Boolean = false): String {
        val safeValue = if (protectFormula && value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) {
            "'$value"
        } else value
        return "\"" + safeValue.replace("\"", "\"\"") + "\""
    }

    private fun money(cents: Long): String =
        "R$ " + BigDecimal.valueOf(cents, 2).toPlainString()
}
