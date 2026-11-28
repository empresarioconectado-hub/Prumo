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
    private companion object {
        const val MAX_BACKUP_CHARACTERS = 10_000_000
        const val MAX_ENCRYPTED_BACKUP_CHARACTERS = 15_000_000
    }

    suspend fun exportEncryptedJson(password: String): String =
        PrumoBackupCrypto.encrypt(exportJson(), password)

    suspend fun restoreBackup(json: String, password: String) {
        require(json.length <= MAX_ENCRYPTED_BACKUP_CHARACTERS) { "O arquivo de backup excede o tamanho permitido." }
        val format = JSONObject(json).optString("format")
        if (format == "prumo-encrypted-backup") {
            restoreJson(PrumoBackupCrypto.decrypt(json, password))
        } else {
            // Compatibility with existing, unencrypted schemaVersion 1 backups.
            restoreJson(json)
        }
    }

    suspend fun exportJson(): String {
        val settings = settingsStore.settingsFlow.first()
        val snapshot = dao.readAllDataSnapshot()
        val root = JSONObject()
            .put("format", "prumo-backup")
            .put("schemaVersion", 1)
            .put("exportedAtMillis", System.currentTimeMillis())
            .put("settings", JSONObject()
                .put("businessName", settings.businessName)
                .put("initialBalanceCents", settings.initialBalanceCents)
                .put("minCashAlertCents", settings.minCashAlertCents)
                .put("taxRatePercent", settings.taxRatePercent))
            .put("cashEntries", JSONArray().apply { snapshot.cashEntries.forEach { put(it.toJson()) } })
            .put("stockItems", JSONArray().apply { snapshot.stockItems.forEach { put(it.toJson()) } })
            .put("stockMovements", JSONArray().apply { snapshot.stockMovements.forEach { put(it.toJson()) } })
            .put("sales", JSONArray().apply { snapshot.sales.forEach { put(it.toJson()) } })
            .put("sellers", JSONArray().apply { snapshot.sellers.forEach { put(it.toJson()) } })
            .put("fixedExpenses", JSONArray().apply { snapshot.fixedExpenses.forEach { put(it.toJson()) } })
        return root.toString(2)
    }

    suspend fun restoreJson(json: String) {
        require(json.length <= MAX_BACKUP_CHARACTERS) { "O arquivo de backup excede o tamanho permitido." }
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

        PrumoValidation.settings(settings)
        cash.forEach(PrumoValidation::cashEntry)
        stock.forEach(PrumoValidation::stockItem)
        sales.forEach(PrumoValidation::sale)
        sellers.forEach(PrumoValidation::seller)
        expenses.forEach(PrumoValidation::fixedExpense)

        require(cash.all { it.amountCents >= 0L } && sales.all { it.grossAmountCents >= 0L }) {
            "Backup contém valores financeiros inválidos."
        }
        require(settings.businessName.isNotBlank() &&
            settings.minCashAlertCents >= 0L && settings.taxRatePercent.isFinite() &&
            settings.taxRatePercent in 0.0..100.0) { "Configurações financeiras inválidas no backup." }
        require(cash.all { it.id > 0L && it.description.isNotBlank() && it.amountCents >= 0L &&
            it.dateMillis > 0L && it.type in setOf("INCOME", "EXPENSE") &&
            it.status in setOf("PAID", "PENDING") }) { "Backup contém lançamentos inválidos." }
        require(stock.all { it.id > 0L && it.sku.isNotBlank() && it.name.isNotBlank() &&
            it.quantity >= 0L && it.initialQuantity >= 0L && it.minStock >= 0L &&
            it.unitCostCents >= 0L && it.unitPriceCents >= 0L && it.avgDailySales.isFinite() &&
            it.avgDailySales >= 0.0 && it.leadTimeDays >= 0L }) { "Backup contém produtos inválidos." }
        require(sales.all { it.id > 0L && it.description.isNotBlank() && it.quantity > 0L &&
            it.grossAmountCents > 0L && it.costAmountCents >= 0L &&
            it.taxPercent.isFinite() && it.taxPercent in 0.0..100.0 &&
            it.commissionPercent.isFinite() && it.commissionPercent in 0.0..100.0 &&
            it.paymentStatus in setOf("PAID", "PENDING") }) { "Backup contém vendas inválidas." }
        require(expenses.all { it.id > 0L && it.description.isNotBlank() &&
            it.amountCents >= 0L && it.dateMillis > 0L }) { "Backup contém despesas inválidas." }
        require(sellers.all { it.id > 0L && it.name.isNotBlank() && it.monthlyGoalCents >= 0L &&
            it.commissionPercent.isFinite() && it.commissionPercent in 0.0..100.0 }) { "Backup contém vendedores inválidos." }
        require(movements.all { it.id > 0L && it.stockItemId > 0L && it.quantity > 0L &&
            it.movementType in setOf("PURCHASE", "SALE", "LOSS", "ADJUSTMENT") &&
            it.unitCostCents >= 0L && it.dateMillis > 0L }) { "Backup contém movimentações inválidas." }
        require(cash.map { it.id }.distinct().size == cash.size &&
            stock.map { it.id }.distinct().size == stock.size &&
            sales.map { it.id }.distinct().size == sales.size &&
            sellers.map { it.id }.distinct().size == sellers.size &&
            expenses.map { it.id }.distinct().size == expenses.size &&
            movements.map { it.id }.distinct().size == movements.size) {
            "Backup contém identificadores duplicados."
        }
        require(stock.map { it.sku.lowercase() }.distinct().size == stock.size) {
            "Backup contém SKUs duplicados."
        }
        val stockIds = stock.map { it.id }.toSet()
        require(sales.all { it.stockItemId == null || it.stockItemId in stockIds }) {
            "Backup contém referências a produtos inexistentes."
        }
        val saleIds = sales.map { it.id }.toSet()
        require(cash.mapNotNull { it.saleId }.distinct().size == cash.count { it.saleId != null }) {
            "Backup contém mais de um lançamento vinculado à mesma venda."
        }
        require(cash.all { it.saleId == null || it.saleId in saleIds }) {
            "Backup contém lançamentos vinculados a vendas inexistentes."
        }
        val previousSettings = settingsStore.settingsFlow.first()
        val previousData = dao.readAllDataSnapshot()
        try {
            dao.replaceAllData(cash, stock, sales, sellers, expenses, movements)
            settingsStore.updateSettings(
                settings.businessName, settings.initialBalanceCents,
                settings.minCashAlertCents, settings.taxRatePercent
            )
        } catch (restoreError: Exception) {
            try {
                dao.replaceAllData(
                    previousData.cashEntries, previousData.stockItems, previousData.sales,
                    previousData.sellers, previousData.fixedExpenses, previousData.stockMovements
                )
                settingsStore.updateSettings(
                    previousSettings.businessName, previousSettings.initialBalanceCents,
                    previousSettings.minCashAlertCents, previousSettings.taxRatePercent
                )
            } catch (rollbackError: Exception) {
                restoreError.addSuppressed(rollbackError)
            }
            throw IllegalStateException(
                "A restauração não foi concluída. O aplicativo tentou recuperar os dados anteriores.",
                restoreError
            )
        }
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
