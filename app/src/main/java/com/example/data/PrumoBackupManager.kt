package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * Portable, versioned JSON backup for all financial and operational data.
 * Restore validates the complete payload before replacing Room tables.
 */
class PrumoBackupManager(
    private val dao: PrumoDao,
    private val settingsStore: SettingsDataStore
) {
    suspend fun exportJson(): String {
        val settings = settingsStore.settingsFlow.first()
        val root = JSONObject()
            .put("format", "prumo-backup")
            .put("schemaVersion", 1)
            .put("exportedAtMillis", System.currentTimeMillis())
            .put("settings", JSONObject()
                .put("businessName", settings.businessName)
                .put("initialBalanceCents", settings.initialBalanceCents)
                .put("minCashAlertCents", settings.minCashAlertCents)
                .put("taxRatePercent", settings.taxRatePercent))
            .put("cashEntries", JSONArray().apply { dao.allCashEntries().forEach { put(it.toJson()) } })
            .put("stockItems", JSONArray().apply { dao.allStockItems().forEach { put(it.toJson()) } })
            .put("stockMovements", JSONArray().apply { dao.allStockMovements().forEach { put(it.toJson()) } })
            .put("sales", JSONArray().apply { dao.allSales().forEach { put(it.toJson()) } })
            .put("sellers", JSONArray().apply { dao.allSellers().forEach { put(it.toJson()) } })
            .put("fixedExpenses", JSONArray().apply { dao.allFixedExpenses().forEach { put(it.toJson()) } })
        return root.toString(2)
    }

    suspend fun restoreJson(json: String) {
        val root = JSONObject(json)
        require(root.getString("format") == "prumo-backup") { "Arquivo não é um backup Prumo." }
        require(root.getInt("schemaVersion") == 1) { "Versão de backup não suportada." }
        val settings = root.getJSONObject("settings").let {
            BusinessSettings(
                businessName = it.getString("businessName"),
                initialBalanceCents = it.getLong("initialBalanceCents"),
                minCashAlertCents = it.getLong("minCashAlertCents"),
                taxRatePercent = it.getDouble("taxRatePercent")
            )
        }
        val cash = root.getJSONArray("cashEntries").toList { it.toCashEntry() }
        val stock = root.getJSONArray("stockItems").toList { it.toStockItem() }
        val movements = root.getJSONArray("stockMovements").toList { it.toStockMovement() }
        val sales = root.getJSONArray("sales").toList { it.toSaleRecord() }
        val sellers = root.getJSONArray("sellers").toList { it.toSellerProfile() }
        val expenses = root.getJSONArray("fixedExpenses").toList { it.toFixedExpense() }

        require(cash.all { it.amountCents >= 0L } && sales.all { it.grossAmountCents >= 0L }) {
            "Backup contém valores financeiros inválidos."
        }
        require(stock.all { it.quantity >= 0L && it.initialQuantity >= 0L }) {
            "Backup contém quantidades de estoque inválidas."
        }
        dao.replaceAllData(cash, stock, sales, sellers, expenses, movements)
        settingsStore.updateSettings(
            settings.businessName, settings.initialBalanceCents,
            settings.minCashAlertCents, settings.taxRatePercent
        )
    }

    private fun CashEntry.toJson() = JSONObject()
        .put("id", id).put("description", description).put("amountCents", amountCents)
        .put("type", type).put("category", category).put("account", account)
        .put("paymentMethod", paymentMethod).put("status", status).put("dateMillis", dateMillis)
        .put("saleId", saleId ?: JSONObject.NULL)

    private fun StockItem.toJson() = JSONObject()
        .put("id", id).put("sku", sku).put("name", name).put("category", category)
        .put("unit", unit).put("quantity", quantity).put("minStock", minStock)
        .put("leadTimeDays", leadTimeDays).put("avgDailySales", avgDailySales)
        .put("unitCostCents", unitCostCents).put("unitPriceCents", unitPriceCents)
        .put("soldUnitsCount", soldUnitsCount).put("lossUnitsCount", lossUnitsCount)
        .put("initialQuantity", initialQuantity)

    private fun StockMovement.toJson() = JSONObject()
        .put("id", id).put("stockItemId", stockItemId).put("sku", sku)
        .put("productName", productName).put("movementType", movementType)
        .put("quantity", quantity).put("unitCostCents", unitCostCents)
        .put("referenceNote", referenceNote).put("dateMillis", dateMillis)

    private fun SaleRecord.toJson() = JSONObject()
        .put("id", id).put("description", description).put("clientName", clientName)
        .put("sellerName", sellerName).put("quantity", quantity)
        .put("grossAmountCents", grossAmountCents).put("costAmountCents", costAmountCents)
        .put("taxPercent", taxPercent).put("commissionPercent", commissionPercent)
        .put("saleDateMillis", saleDateMillis).put("stockItemId", stockItemId ?: JSONObject.NULL)
        .put("paymentMethod", paymentMethod).put("account", account).put("paymentStatus", paymentStatus)

    private fun SellerProfile.toJson() = JSONObject()
        .put("id", id).put("name", name).put("monthlyGoalCents", monthlyGoalCents)
        .put("commissionPercent", commissionPercent)

    private fun FixedExpense.toJson() = JSONObject()
        .put("id", id).put("description", description).put("category", category)
        .put("amountCents", amountCents).put("dateMillis", dateMillis)

    private inline fun <T> JSONArray.toList(parse: (JSONObject) -> T): List<T> =
        (0 until length()).map { parse(getJSONObject(it)) }

    private fun JSONObject.nullableLong(key: String): Long? =
        if (isNull(key)) null else getLong(key)

    private fun JSONObject.toCashEntry() = CashEntry(
        id = getLong("id"), description = getString("description"), amountCents = getLong("amountCents"),
        type = getString("type"), category = getString("category"), account = getString("account"),
        paymentMethod = getString("paymentMethod"), status = getString("status"),
        dateMillis = getLong("dateMillis"), saleId = nullableLong("saleId")
    )

    private fun JSONObject.toStockItem() = StockItem(
        id = getLong("id"), sku = getString("sku"), name = getString("name"),
        category = getString("category"), unit = getString("unit"), quantity = getLong("quantity"),
        minStock = getLong("minStock"), leadTimeDays = getLong("leadTimeDays"),
        avgDailySales = getDouble("avgDailySales"), unitCostCents = getLong("unitCostCents"),
        unitPriceCents = getLong("unitPriceCents"), soldUnitsCount = getLong("soldUnitsCount"),
        lossUnitsCount = getLong("lossUnitsCount"), initialQuantity = getLong("initialQuantity")
    )

    private fun JSONObject.toStockMovement() = StockMovement(
        id = getLong("id"), stockItemId = getLong("stockItemId"), sku = getString("sku"),
        productName = getString("productName"), movementType = getString("movementType"),
        quantity = getLong("quantity"), unitCostCents = getLong("unitCostCents"),
        referenceNote = getString("referenceNote"), dateMillis = getLong("dateMillis")
    )

    private fun JSONObject.toSaleRecord() = SaleRecord(
        id = getLong("id"), description = getString("description"), clientName = getString("clientName"),
        sellerName = getString("sellerName"), quantity = getLong("quantity"),
        grossAmountCents = getLong("grossAmountCents"), costAmountCents = getLong("costAmountCents"),
        taxPercent = getDouble("taxPercent"), commissionPercent = getDouble("commissionPercent"),
        saleDateMillis = getLong("saleDateMillis"), stockItemId = nullableLong("stockItemId"),
        paymentMethod = getString("paymentMethod"), account = getString("account"),
        paymentStatus = getString("paymentStatus")
    )

    private fun JSONObject.toSellerProfile() = SellerProfile(
        id = getLong("id"), name = getString("name"),
        monthlyGoalCents = getLong("monthlyGoalCents"), commissionPercent = getDouble("commissionPercent")
    )

    private fun JSONObject.toFixedExpense() = FixedExpense(
        id = getLong("id"), description = getString("description"),
        category = getString("category"), amountCents = getLong("amountCents"),
        dateMillis = getLong("dateMillis")
    )
}
