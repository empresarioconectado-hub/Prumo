package com.example.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.ui.viewmodel.PrumoDashboardData
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrumoReportExporter {
    fun writeDrePdf(output: OutputStream, data: PrumoDashboardData) {
        val document = PdfDocument()
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
        document.close()
    }

    fun cashEntriesCsv(data: PrumoDashboardData): String {
        val header = "Data;Descrição;Tipo;Categoria;Conta;Status;Valor (R$)"
        val rows = data.monthCashEntries.sortedBy { it.dateMillis }.map { entry ->
            listOf(
                SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(entry.dateMillis)),
                entry.description,
                entry.entryType.label,
                entry.category,
                entry.account,
                entry.entryStatus.label,
                String.format(Locale.US, "%.2f", entry.amountCents / 100.0)
            ).joinToString(";") { csv(it) }
        }
        return (listOf(header) + rows).joinToString("\r\n")
    }

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
    private fun money(cents: Long): String =
        "R$ " + String.format(Locale("pt", "BR"), "%,.2f", cents / 100.0)
}
