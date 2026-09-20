package com.forge.domain.engine

import com.forge.data.local.entity.ExercisePersonalRecordEntity
import com.forge.data.local.entity.WorkoutSetEntity
import kotlin.math.roundToInt

data class ProgressionRecommendation(
    val exerciseId: String,
    val recommendedWeightKg: Double,
    val recommendedRepRange: IntRange,
    val rationale: String,
    val isDeloadRecommended: Boolean = false,
    val fatigueRiskDetected: Boolean = false,
    val isLoadIncreased: Boolean = false
)

data class DetectedPr(
    val exerciseId: String,
    val isWeightPr: Boolean = false,
    val isRepPr: Boolean = false,
    val is1RmPr: Boolean = false,
    val isVolumePr: Boolean = false,
    val estimated1RmKg: Double = 0.0,
    val summary: String = ""
)

object ProgressionEngine {

    /**
     * Calculates the estimated 1-Rep Max using the Epley formula:
     * 1RM = weight * (1 + reps / 30.0)
     * Valid for working sets between 1 and 12 reps.
     */
    fun calculateEstimated1Rm(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        if (reps > 15) {
            // High-rep isolation is not a reliable 1RM measurement; return conservative estimate
            return (weightKg * (1.0 + 15.0 / 30.0) * 10.0).roundToInt() / 10.0
        }
        val estimate = weightKg * (1.0 + (reps / 30.0))
        return (estimate * 10.0).roundToInt() / 10.0
    }

    /**
     * Evaluates whether a completed set establishes any new Personal Records.
     */
    fun evaluatePersonalRecord(
        exerciseId: String,
        completedSet: WorkoutSetEntity,
        existingPr: ExercisePersonalRecordEntity?
    ): DetectedPr? {
        if (!completedSet.isCompleted || completedSet.weightKg <= 0.0 || completedSet.reps <= 0) {
            return null
        }

        val weight = completedSet.weightKg
        val reps = completedSet.reps
        val setVolume = weight * reps
        val est1Rm = calculateEstimated1Rm(weight, reps)

        var isWeightPr = false
        var isRepPr = false
        var is1RmPr = false
        var isVolumePr = false

        if (existingPr == null) {
            return DetectedPr(
                exerciseId = exerciseId,
                isWeightPr = true,
                isRepPr = true,
                is1RmPr = true,
                isVolumePr = true,
                estimated1RmKg = est1Rm,
                summary = "Initial PR: ${weight} kg × $reps reps (Est. 1RM: $est1Rm kg)"
            )
        }

        if (weight > existingPr.maxWeightKg) {
            isWeightPr = true
        } else if (weight == existingPr.maxWeightKg && reps > existingPr.maxRepsAtMaxWeight) {
            isRepPr = true
        }

        if (est1Rm > existingPr.estimated1RmKg + 0.1) {
            is1RmPr = true
        }

        if (setVolume > existingPr.bestSetVolumeKg) {
            isVolumePr = true
        }

        if (isWeightPr || isRepPr || is1RmPr || isVolumePr) {
            val details = mutableListOf<String>()
            if (isWeightPr) details.add("Max Weight: ${weight} kg")
            if (isRepPr) details.add("Rep PR: ${reps} reps at ${weight} kg")
            if (is1RmPr) details.add("Est. 1RM: ${est1Rm} kg")
            if (isVolumePr) details.add("Set Volume: ${setVolume.roundToInt()} kg")

            return DetectedPr(
                exerciseId = exerciseId,
                isWeightPr = isWeightPr,
                isRepPr = isRepPr,
                is1RmPr = is1RmPr,
                isVolumePr = isVolumePr,
                estimated1RmKg = est1Rm,
                summary = details.joinToString(" • ")
            )
        }

        return null
    }

    /**
     * Deterministic Double Progression Engine.
     * Computes the next session's conservative recommendation based on actual logged set history.
     */
    fun computeRecommendation(
        exerciseId: String,
        targetRepRange: IntRange = 8..12,
        recentSessionsSets: List<List<WorkoutSetEntity>>, // Most recent session first
        equipmentId: String? = null,
        availableIncrementKg: Double = 2.5
    ): ProgressionRecommendation {
        if (recentSessionsSets.isEmpty() || recentSessionsSets.first().none { it.isCompleted }) {
            val baseWeight = if (equipmentId == "bodyweight") 0.0 else 20.0
            return ProgressionRecommendation(
                exerciseId = exerciseId,
                recommendedWeightKg = baseWeight,
                recommendedRepRange = targetRepRange,
                rationale = "Starting baseline for $targetRepRange reps."
            )
        }

        val lastSessionSets = recentSessionsSets.first().filter { it.isCompleted && it.setOrder > 0 && it.setType != "WARMUP" }
        if (lastSessionSets.isEmpty()) {
            val baseWeight = if (equipmentId == "bodyweight") 0.0 else 20.0
            return ProgressionRecommendation(
                exerciseId = exerciseId,
                recommendedWeightKg = baseWeight,
                recommendedRepRange = targetRepRange,
                rationale = "No valid completed working sets found in last session."
            )
        }

        val lastWorkingWeight = lastSessionSets.map { it.weightKg }.maxOrNull() ?: 0.0
        val repsList = lastSessionSets.map { it.reps }
        val rirList = lastSessionSets.mapNotNull { it.rir }
        val avgRir = if (rirList.isNotEmpty()) rirList.average() else 2.0
        val minRir = rirList.minOrNull() ?: 2

        val targetTop = targetRepRange.last
        val targetBottom = targetRepRange.first
        val allHitTop = repsList.all { it >= targetTop }

        // 1. Fatigue & Performance Decline Check across sessions
        if (recentSessionsSets.size >= 2) {
            val prevSessionSets = recentSessionsSets[1].filter { it.isCompleted && it.setOrder > 0 && it.setType != "WARMUP" }
            if (prevSessionSets.isNotEmpty()) {
                val prevAvgReps = prevSessionSets.map { it.reps }.average()
                val currAvgReps = repsList.average()
                val prevWeight = prevSessionSets.map { it.weightKg }.maxOrNull() ?: 0.0

                // Drop of > 2.5 reps or declining load across 2+ sessions indicates sustained fatigue
                if (lastWorkingWeight <= prevWeight && (prevAvgReps - currAvgReps) >= 2.5) {
                    return ProgressionRecommendation(
                        exerciseId = exerciseId,
                        recommendedWeightKg = lastWorkingWeight,
                        recommendedRepRange = targetRepRange,
                        rationale = "Performance declined from ${(prevAvgReps * 10).roundToInt() / 10.0} avg reps down to ${(currAvgReps * 10).roundToInt() / 10.0} reps. Maintaining load to recover work capacity.",
                        fatigueRiskDetected = true,
                        isDeloadRecommended = (prevAvgReps - currAvgReps) >= 4.0
                    )
                }
            }
        }

        // 2. Double Progression: Upper threshold reached with sufficient RIR
        if (allHitTop) {
            if (minRir >= 2) {
                // Determine load jump based on equipment
                val loadJump = when (equipmentId) {
                    "dumbbell" -> if (availableIncrementKg < 2.0) 2.0 else availableIncrementKg
                    "bodyweight" -> if (lastWorkingWeight == 0.0) 2.5 else availableIncrementKg
                    else -> availableIncrementKg
                }
                val newWeight = lastWorkingWeight + loadJump
                val resetRepRange = targetBottom..(targetBottom + 2)
                val repsSummary = repsList.joinToString("/")

                return ProgressionRecommendation(
                    exerciseId = exerciseId,
                    recommendedWeightKg = newWeight,
                    recommendedRepRange = resetRepRange,
                    rationale = "Completed $lastWorkingWeight kg for $repsSummary reps with ~$avgRir RIR. Progressing load to $newWeight kg × $resetRepRange.",
                    isLoadIncreased = true
                )
            } else {
                // Hit top reps but RIR was 0 or 1 (very hard / failure)
                return ProgressionRecommendation(
                    exerciseId = exerciseId,
                    recommendedWeightKg = lastWorkingWeight,
                    recommendedRepRange = targetRepRange,
                    rationale = "Completed top of rep range ($targetTop reps) but with high effort ($minRir RIR). Consolidate at $lastWorkingWeight kg before adding weight."
                )
            }
        }

        // 3. Inside rep range: progress reps first
        val allAboveBottom = repsList.all { it >= targetBottom }
        if (allAboveBottom) {
            val repsSummary = repsList.joinToString("/")
            return ProgressionRecommendation(
                exerciseId = exerciseId,
                recommendedWeightKg = lastWorkingWeight,
                recommendedRepRange = targetRepRange,
                rationale = "Logged $repsSummary at $lastWorkingWeight kg. Stay at $lastWorkingWeight kg and target +1 rep on working sets."
            )
        }

        // 4. Below rep range or steep drop-off within session
        val firstSetReps = repsList.firstOrNull() ?: 0
        val lastSetReps = repsList.lastOrNull() ?: 0
        if (firstSetReps - lastSetReps >= 4) {
            return ProgressionRecommendation(
                exerciseId = exerciseId,
                recommendedWeightKg = lastWorkingWeight,
                recommendedRepRange = targetRepRange,
                rationale = "Reps dropped from $firstSetReps to $lastSetReps across sets. Maintaining $lastWorkingWeight kg to build volume tolerance.",
                fatigueRiskDetected = true
            )
        }

        return ProgressionRecommendation(
            exerciseId = exerciseId,
            recommendedWeightKg = lastWorkingWeight,
            recommendedRepRange = targetRepRange,
            rationale = "Reps were below target range ($targetBottom–$targetTop). Maintain $lastWorkingWeight kg until hitting at least $targetBottom reps on all sets."
        )
    }
}
