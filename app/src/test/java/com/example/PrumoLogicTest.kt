package com.example

import com.example.model.DreSummary
import com.example.model.StockItem
import com.example.model.StockStatus
import com.example.util.parseBrlInputToCents
import org.junit.Assert.assertEquals
import org.junit.Test

class PrumoLogicTest {

    @Test
    fun parseBrlInputToCents_parsesPortugueseAndDecimalFormats() {
        assertEquals(150050L, parseBrlInputToCents("1.500,50"))
        assertEquals(150050L, parseBrlInputToCents("R$ 1500,50"))
        assertEquals(4800000L, parseBrlInputToCents("48000"))
        assertEquals(null, parseBrlInputToCents("-100"))
    }

    @Test
    fun stockItem_computesReorderPointAndSuggestedPurchase() {
        val item = StockItem(
            id = 1L,
            sku = "CAF-05",
            name = "Café 500g",
            category = "Bebidas & Alimentos",
            unit = "un",
            quantity = 8L,
            minStock = 5L,
            leadTimeDays = 3L,
            avgDailySales = 2.5,
            unitCostCents = 1800L,
            unitPriceCents = 3500L,
            soldUnitsCount = 24L
        )
        // reorderPoint = ceil(2.5 * 3) + 5 = 8 + 5 = 13
        assertEquals(13L, item.reorderPoint)
        assertEquals(StockStatus.REORDER, item.status)
        // suggested = max(13*2, 10) - 8 = 26 - 8 = 18
        assertEquals(18L, item.suggestedPurchaseQty)
        assertEquals(18L * 1800L, item.estimatedPurchaseCostCents)
    }

    @Test
    fun dreSummary_computesMarginsCorrectly() {
        val dre = DreSummary(
            grossRevenueCents = 100_000L,
            salesTaxesCents = 6_000L,
            netRevenueCents = 94_000L,
            costOfGoodsCents = 34_000L,
            commissionsCents = 5_000L,
            contributionMarginCents = 55_000L,
            fixedExpensesCents = 30_000L,
            operatingProfitEbitdaCents = 25_000L,
            netProfitCents = 25_000L
        )
        assertEquals(55.0, dre.contributionMarginPercent, 0.01)
        assertEquals(25.0, dre.netProfitPercent, 0.01)
    }
}
