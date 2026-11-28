package com.example.util

import com.example.model.SaleRecord
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a plain-text receipt that can be shared through the Android Sharesheet.
 * This formatter has no Android dependencies, so its output can be unit tested.
 */
fun saleReceiptText(sale: SaleRecord, businessName: String): String {
    val locale = Locale("pt", "BR")
    val currency = NumberFormat.getCurrencyInstance(locale)
    val date = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", locale)
        .format(Date(sale.saleDateMillis))
    val paymentStatus = if (sale.paymentStatus == "PAID") "Recebido" else "Pendente"
    val safeBusinessName = businessName.trim().ifBlank { "Meu negócio" }

    return buildString {
        appendLine(safeBusinessName)
        appendLine("COMPROVANTE DE VENDA")
        if (sale.id > 0L) appendLine("Venda: #${sale.id}")
        appendLine("Data: $date")
        appendLine("Cliente: ${sale.clientName.ifBlank { "Cliente Balcão" }}")
        appendLine("Item: ${sale.description}")
        appendLine("Quantidade: ${sale.quantity}")
        appendLine("Total: ${currency.format(sale.grossAmountCents / 100.0)}")
        appendLine("Pagamento: ${sale.paymentMethod} — $paymentStatus")
        appendLine()
        append("Obrigado pela preferência!")
    }
}
