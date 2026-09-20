package com.forge.domain.engine

import kotlin.math.roundToInt

enum class NutritionGoal(val title: String) {
    CUT("Cut (Fat Loss)"),
    LEAN_BULK("Lean Bulk (Muscle Gain)"),
    RECOMPOSITION("Body Recomposition"),
    MAINTENANCE("Maintenance")
}

data class MacroSplit(
    val calories: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val carbsGrams: Int,
    val calorieTargetRange: IntRange
)

data class WeightTrendFeedback(
    val weeklyWeightChangeKg: Double,
    val isAdjustmentRecommended: Boolean,
    val recommendedCalorieDelta: Int,
    val rationale: String
)

object NutritionEngine {

    /**
     * Calculates estimated maintenance calories via Mifflin-St Jeor formula.
     */
    fun estimateMaintenanceCalories(
        weightKg: Double,
        heightCm: Double = 175.0,
        age: Int = 28,
        isMale: Boolean? = true,
        activityMultiplier: Double = 1.55 // Moderately active (3-5 resistance training days)
    ): Int {
        val baseBmr = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
        val genderBmr = when (isMale) {
            true -> baseBmr + 5.0
            false -> baseBmr - 161.0
            null -> baseBmr - 78.0
        }
        val tdee = genderBmr * activityMultiplier
        return tdee.roundToInt()
    }

    /**
     * Calculates daily calorie target and balanced macronutrient distribution.
     */
    fun computeMacroSplit(
        maintenanceCalories: Int,
        goal: NutritionGoal,
        bodyweightKg: Double,
        proteinPerKg: Double = 2.0,
        fatPerKg: Double = 0.85
    ): MacroSplit {
        val (targetCalories, range) = when (goal) {
            NutritionGoal.CUT -> {
                val deficit = 400
                (maintenanceCalories - deficit) to ((maintenanceCalories - 500)..(maintenanceCalories - 300))
            }
            NutritionGoal.LEAN_BULK -> {
                val surplus = 200
                (maintenanceCalories + surplus) to ((maintenanceCalories + 150)..(maintenanceCalories + 300))
            }
            NutritionGoal.RECOMPOSITION -> {
                maintenanceCalories to ((maintenanceCalories - 100)..(maintenanceCalories + 100))
            }
            NutritionGoal.MAINTENANCE -> {
                maintenanceCalories to ((maintenanceCalories - 75)..(maintenanceCalories + 75))
            }
        }

        val clampedCalories = maxOf(1400, targetCalories)
        val proteinGrams = (bodyweightKg * proteinPerKg).roundToInt()
        val fatGrams = (bodyweightKg * fatPerKg).roundToInt()

        val proteinCalories = proteinGrams * 4
        val fatCalories = fatGrams * 9
        val remainingCalories = clampedCalories - (proteinCalories + fatCalories)

        val carbsGrams = maxOf(30, (remainingCalories / 4.0).roundToInt())

        return MacroSplit(
            calories = clampedCalories,
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
            carbsGrams = carbsGrams,
            calorieTargetRange = range
        )
    }

    /**
     * Evaluates a rolling series of daily weights to generate an adaptive feedback loop.
     * Prevents abrupt daily fluctuations from skewing recommendations.
     */
    fun evaluateWeightTrend(
        recentWeights: List<Double>, // In chronological order (at least 7 days recommended)
        goal: NutritionGoal
    ): WeightTrendFeedback {
        if (recentWeights.size < 7) {
            return WeightTrendFeedback(
                weeklyWeightChangeKg = 0.0,
                isAdjustmentRecommended = false,
                recommendedCalorieDelta = 0,
                rationale = "Need at least 7 days of bodyweight logs to establish a reliable rolling trend."
            )
        }

        // Compare first half to second half
        val halfSize = recentWeights.size / 2
        val firstHalfAvg = recentWeights.take(halfSize).average()
        val secondHalfAvg = recentWeights.takeLast(halfSize).average()

        val delta = secondHalfAvg - firstHalfAvg
        val weeklyRate = ((delta / halfSize.toDouble()) * 7.0 * 10.0).roundToInt() / 10.0

        return when (goal) {
            NutritionGoal.LEAN_BULK -> {
                when {
                    weeklyRate < 0.05 -> {
                        WeightTrendFeedback(
                            weeklyWeightChangeKg = weeklyRate,
                            isAdjustmentRecommended = true,
                            recommendedCalorieDelta = +150,
                            rationale = "Weight has remained stable ($weeklyRate kg/week). Modestly increase intake by +150 kcal to support hypertrophy."
                        )
                    }
                    weeklyRate > 0.45 -> {
                        WeightTrendFeedback(
                            weeklyWeightChangeKg = weeklyRate,
                            isAdjustmentRecommended = true,
                            recommendedCalorieDelta = -100,
                            rationale = "Weight is rising faster than intended ($weeklyRate kg/week). Modestly reduce surplus by -100 kcal to prevent excess fat gain."
                        )
                    }
                    else -> {
                        WeightTrendFeedback(
                            weeklyWeightChangeKg = weeklyRate,
                            isAdjustmentRecommended = false,
                            recommendedCalorieDelta = 0,
                            rationale = "Weight trend is progressing in optimal lean surplus range ($weeklyRate kg/week)."
                        )
                    }
                }
            }
            NutritionGoal.CUT -> {
                when {
                    weeklyRate > -0.1 -> {
                        WeightTrendFeedback(
                            weeklyWeightChangeKg = weeklyRate,
                            isAdjustmentRecommended = true,
                            recommendedCalorieDelta = -150,
                            rationale = "Weight trend has plateaued ($weeklyRate kg/week). Consider reducing target by -150 kcal."
                        )
                    }
                    weeklyRate < -0.9 -> {
                        WeightTrendFeedback(
                            weeklyWeightChangeKg = weeklyRate,
                            isAdjustmentRecommended = true,
                            recommendedCalorieDelta = +150,
                            rationale = "Weight is dropping rapidly ($weeklyRate kg/week). Increase intake by +150 kcal to protect lean muscle mass."
                        )
                    }
                    else -> {
                        WeightTrendFeedback(
                            weeklyWeightChangeKg = weeklyRate,
                            isAdjustmentRecommended = false,
                            recommendedCalorieDelta = 0,
                            rationale = "Fat loss rate is on target ($weeklyRate kg/week)."
                        )
                    }
                }
            }
            NutritionGoal.MAINTENANCE, NutritionGoal.RECOMPOSITION -> {
                if (kotlin.math.abs(weeklyRate) > 0.4) {
                    val dir = if (weeklyRate > 0) -100 else +100
                    WeightTrendFeedback(
                        weeklyWeightChangeKg = weeklyRate,
                        isAdjustmentRecommended = true,
                        recommendedCalorieDelta = dir,
                        rationale = "Weight is drifting ($weeklyRate kg/week). Adjust daily target by $dir kcal."
                    )
                } else {
                    WeightTrendFeedback(
                        weeklyWeightChangeKg = weeklyRate,
                        isAdjustmentRecommended = false,
                        recommendedCalorieDelta = 0,
                        rationale = "Weight is stable ($weeklyRate kg/week) within maintenance range."
                    )
                }
            }
        }
    }
}
