package com.forge.domain.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NutritionEngineTest {

    @Test
    fun maintenance_estimation_calculates_reasonable_values() {
        val calories = NutritionEngine.estimateMaintenanceCalories(
            weightKg = 80.0,
            heightCm = 178.0,
            age = 25,
            isMale = true,
            activityMultiplier = 1.55
        )
        // 80kg male 178cm 25y moderately active ~ 2700-2900 kcal
        assertThat(calories).isIn(2600..3000)
    }

    @Test
    fun macro_split_calories_are_internally_consistent() {
        val bodyweight = 75.0
        val maintenance = 2500

        val cutMacros = NutritionEngine.computeMacroSplit(maintenance, NutritionGoal.CUT, bodyweight)
        assertThat(cutMacros.calories).isEqualTo(2100)
        assertThat(cutMacros.proteinGrams).isEqualTo(150) // 75 * 2.0
        val totalCalories = (cutMacros.proteinGrams * 4) + (cutMacros.fatGrams * 9) + (cutMacros.carbsGrams * 4)
        // Ensure within rounding tolerance (±15 kcal)
        assertThat(kotlin.math.abs(cutMacros.calories - totalCalories)).isAtMost(15)

        val bulkMacros = NutritionEngine.computeMacroSplit(maintenance, NutritionGoal.LEAN_BULK, bodyweight)
        assertThat(bulkMacros.calories).isEqualTo(2700)
        assertThat(bulkMacros.proteinGrams).isEqualTo(150)
    }

    @Test
    fun weight_trend_detects_plateau_on_cut() {
        // Flat weights over 10 days
        val weights = listOf(80.2, 80.1, 80.3, 80.1, 80.2, 80.2, 80.1, 80.2, 80.3, 80.2)
        val feedback = NutritionEngine.evaluateWeightTrend(weights, NutritionGoal.CUT)

        assertThat(feedback.isAdjustmentRecommended).isTrue()
        assertThat(feedback.recommendedCalorieDelta).isEqualTo(-150)
        assertThat(feedback.rationale).contains("plateaued")
    }

    @Test
    fun weight_trend_detects_optimal_lean_bulk() {
        // Modest upward slope (~0.25 kg/week)
        val weights = listOf(75.0, 75.1, 75.1, 75.2, 75.3, 75.3, 75.4, 75.4, 75.5, 75.6)
        val feedback = NutritionEngine.evaluateWeightTrend(weights, NutritionGoal.LEAN_BULK)

        assertThat(feedback.isAdjustmentRecommended).isFalse()
        assertThat(feedback.recommendedCalorieDelta).isEqualTo(0)
        assertThat(feedback.rationale).contains("optimal lean surplus")
    }

    @Test
    fun weight_trend_requires_at_least_seven_days() {
        val weights = listOf(75.0, 75.1, 75.2)
        val feedback = NutritionEngine.evaluateWeightTrend(weights, NutritionGoal.MAINTENANCE)

        assertThat(feedback.isAdjustmentRecommended).isFalse()
        assertThat(feedback.rationale).contains("at least 7 days")
    }
}
