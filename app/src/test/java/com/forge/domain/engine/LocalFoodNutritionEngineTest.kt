package com.forge.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFoodNutritionEngineTest {

    @Test
    fun estimateMealFromText_singleItemGramQuantity_calculatesAccurately() {
        // 200g chicken breast (chicken breast: 165 kcal, 31g protein, 0g carb, 4g fat per 100g)
        val result = LocalFoodNutritionEngine.estimateMealFromText("200g chicken breast")
        assertNotNull(result)
        assertEquals(1, result.items.size)
        assertEquals("Chicken Breast", result.items[0].name)
        assertEquals(200, result.items[0].grams)
        assertEquals(330, result.totalCalories)
        assertEquals(62, result.totalProteinG)
        assertEquals(0, result.totalCarbsG)
        assertEquals(8, result.totalFatG)
    }

    @Test
    fun estimateMealFromText_compositeMealString_aggregatesAllItems() {
        // "200g chicken breast and 300g white rice"
        // chicken: 330 kcal, 62p, 0c, 8f
        // white rice (130 kcal, 3g p, 28g c, 0g f per 100g): 390 kcal, 9g p, 84g c, 0g f
        val result = LocalFoodNutritionEngine.estimateMealFromText("200g chicken breast and 300g white rice")
        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertTrue(result.totalCalories in 700..740)
        assertTrue(result.totalProteinG in 68..75)
        assertTrue(result.totalCarbsG in 80..90)
        assertTrue(result.totalFatG in 7..10)
    }

    @Test
    fun estimateMealFromText_pieceQuantity_convertsToGramsCorrectly() {
        // "4 large eggs" -> 4 * 50g = 200g egg
        val result = LocalFoodNutritionEngine.estimateMealFromText("4 large eggs")
        assertNotNull(result)
        assertEquals(1, result.items.size)
        assertEquals(200, result.items[0].grams)
        // 200g egg: 286 kcal, 26g p, 2g c, 20g f
        assertTrue(result.totalCalories in 280..295)
        assertTrue(result.totalProteinG in 24..28)
    }

    @Test
    fun estimateMealFromText_unrecognizedWord_fallsBackToBalancedEstimate() {
        val result = LocalFoodNutritionEngine.estimateMealFromText("quantum synthetic plasma")
        assertNotNull(result)
        assertEquals(1, result.items.size)
        assertTrue(result.items[0].name.contains("Whole Food Meal"))
        assertTrue(result.totalCalories > 0)
    }
}
