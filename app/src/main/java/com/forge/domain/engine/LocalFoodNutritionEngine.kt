package com.forge.domain.engine

import java.util.Locale

data class FoodItemEstimate(
    val name: String,
    val grams: Int,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

data class EstimatedMeal(
    val title: String,
    val items: List<FoodItemEstimate>,
    val totalCalories: Int,
    val totalProteinG: Int,
    val totalCarbsG: Int,
    val totalFatG: Int,
    val isEstimated: Boolean = true
)

/**
 * LocalFoodNutritionEngine
 * Pure deterministic, offline nutritional knowledge engine.
 * Zero paid APIs, zero network overhead, zero cloud dependencies.
 * Inspired by FUD-AI's offline-first architecture.
 */
object LocalFoodNutritionEngine {

    // Nutritional density per 100g: (Calories, Protein, Carbs, Fat)
    private val FOOD_DATABASE = mapOf(
        "chicken breast" to MacroProfile(165, 31, 0, 4),
        "chicken" to MacroProfile(190, 28, 0, 7),
        "rice" to MacroProfile(130, 3, 28, 0),
        "white rice" to MacroProfile(130, 3, 28, 0),
        "brown rice" to MacroProfile(112, 3, 24, 1),
        "egg" to MacroProfile(143, 13, 1, 10), // ~70 kcal per 50g egg
        "eggs" to MacroProfile(143, 13, 1, 10),
        "egg whites" to MacroProfile(52, 11, 1, 0),
        "beef" to MacroProfile(250, 26, 0, 15),
        "ground beef" to MacroProfile(220, 24, 0, 14),
        "steak" to MacroProfile(240, 26, 0, 14),
        "salmon" to MacroProfile(208, 20, 0, 13),
        "tuna" to MacroProfile(132, 28, 0, 1),
        "oats" to MacroProfile(389, 17, 66, 7),
        "oatmeal" to MacroProfile(389, 17, 66, 7),
        "whey" to MacroProfile(375, 75, 8, 4),
        "protein powder" to MacroProfile(375, 75, 8, 4),
        "milk" to MacroProfile(60, 3, 5, 3),
        "greek yogurt" to MacroProfile(59, 10, 4, 0),
        "curd" to MacroProfile(98, 11, 3, 4),
        "paneer" to MacroProfile(296, 18, 4, 22),
        "tofu" to MacroProfile(76, 8, 2, 5),
        "peanut butter" to MacroProfile(588, 25, 20, 50),
        "banana" to MacroProfile(89, 1, 23, 0),
        "apple" to MacroProfile(52, 0, 14, 0),
        "potato" to MacroProfile(77, 2, 17, 0),
        "sweet potato" to MacroProfile(86, 2, 20, 0),
        "olive oil" to MacroProfile(884, 0, 0, 100),
        "butter" to MacroProfile(717, 1, 0, 81),
        "bread" to MacroProfile(265, 9, 49, 3),
        "pasta" to MacroProfile(131, 5, 25, 1)
    )

    private data class MacroProfile(val calories: Int, val protein: Int, val carbs: Int, val fat: Int)

    fun estimateMealFromText(input: String): EstimatedMeal {
        val lower = input.lowercase(Locale.ROOT)
        val items = mutableListOf<FoodItemEstimate>()

        // Split input into lines or clauses by "and", ",", "+"
        val segments = lower.split(Regex("(?i)\\band\\b|,|\\+")).map { it.trim() }.filter { it.isNotEmpty() }

        for (segment in segments) {
            // Find grams (e.g., "200g", "150 grams", "250 g")
            val gramMatch = Regex("(\\d+)\\s*(?:g|grams?|gm)\\b").find(segment)
            val countMatch = Regex("\\b(\\d+)\\s*(?:whole|large|small)?\\s*(eggs?|bananas?|apples?)\\b").find(segment)

            var parsedGrams = 100
            if (gramMatch != null) {
                parsedGrams = gramMatch.groupValues[1].toIntOrNull() ?: 100
            } else if (countMatch != null) {
                val count = countMatch.groupValues[1].toIntOrNull() ?: 1
                val itemWord = countMatch.groupValues[2]
                parsedGrams = when {
                    itemWord.startsWith("egg") -> count * 50
                    itemWord.startsWith("banana") -> count * 120
                    itemWord.startsWith("apple") -> count * 150
                    else -> count * 100
                }
            }

            // Find matching food key
            var matchedKey: String? = null
            var bestLength = 0
            for (key in FOOD_DATABASE.keys) {
                if (segment.contains(key) && key.length > bestLength) {
                    matchedKey = key
                    bestLength = key.length
                }
            }

            if (matchedKey != null) {
                val profile = FOOD_DATABASE[matchedKey]!!
                val factor = parsedGrams / 100.0
                items.add(
                    FoodItemEstimate(
                        name = matchedKey.split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) },
                        grams = parsedGrams,
                        calories = (profile.calories * factor).toInt(),
                        proteinG = (profile.protein * factor).toInt(),
                        carbsG = (profile.carbs * factor).toInt(),
                        fatG = (profile.fat * factor).toInt()
                    )
                )
            }
        }

        // If no specific foods identified, provide a balanced generic meal estimate
        if (items.isEmpty()) {
            val genericGrams = 350
            items.add(
                FoodItemEstimate(
                    name = "Whole Food Meal (Estimated)",
                    grams = genericGrams,
                    calories = 480,
                    proteinG = 35,
                    carbsG = 50,
                    fatG = 14
                )
            )
        }

        val totalCal = items.sumOf { it.calories }
        val totalP = items.sumOf { it.proteinG }
        val totalC = items.sumOf { it.carbsG }
        val totalF = items.sumOf { it.fatG }

        return EstimatedMeal(
            title = if (items.size == 1) items.first().name else "Meal (${items.size} components)",
            items = items,
            totalCalories = totalCal,
            totalProteinG = totalP,
            totalCarbsG = totalC,
            totalFatG = totalF,
            isEstimated = true
        )
    }
}
