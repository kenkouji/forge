package com.forge.domain.engine

import com.forge.data.local.entity.ExercisePersonalRecordEntity
import com.forge.data.local.entity.WorkoutSetEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgressionEngineTest {

    private fun createSet(
        setOrder: Int,
        weightKg: Double,
        reps: Int,
        rir: Int? = 2,
        isCompleted: Boolean = true
    ): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = "set_$setOrder",
            sessionId = "test_session",
            exerciseId = "barbell_bench_press",
            setOrder = setOrder,
            setType = "NORMAL",
            weightKg = weightKg,
            reps = reps,
            rir = rir,
            isCompleted = isCompleted
        )
    }

    @Test
    fun epley_1rm_calculation_is_accurate() {
        // 100 kg x 1 rep = 100 kg
        assertThat(ProgressionEngine.calculateEstimated1Rm(100.0, 1)).isEqualTo(100.0)

        // 100 kg x 10 reps = 100 * (1 + 10/30) = 133.3 kg
        assertThat(ProgressionEngine.calculateEstimated1Rm(100.0, 10)).isEqualTo(133.3)

        // 60 kg x 12 reps = 60 * (1 + 12/30) = 84.0 kg
        assertThat(ProgressionEngine.calculateEstimated1Rm(60.0, 12)).isEqualTo(84.0)

        // 0 weight returns 0
        assertThat(ProgressionEngine.calculateEstimated1Rm(0.0, 10)).isEqualTo(0.0)
    }

    @Test
    fun double_progression_increases_load_when_top_of_range_reached_with_good_rir() {
        val targetRange = 8..12
        // User hits 60 kg x 12/12/12 with 2 RIR
        val sets = listOf(
            createSet(1, 60.0, 12, rir = 2),
            createSet(2, 60.0, 12, rir = 2),
            createSet(3, 60.0, 12, rir = 2)
        )

        val recommendation = ProgressionEngine.computeRecommendation(
            exerciseId = "barbell_bench_press",
            targetRepRange = targetRange,
            recentSessionsSets = listOf(sets),
            equipmentId = "barbell",
            availableIncrementKg = 2.5
        )

        assertThat(recommendation.isLoadIncreased).isTrue()
        assertThat(recommendation.recommendedWeightKg).isEqualTo(62.5)
        assertThat(recommendation.recommendedRepRange).isEqualTo(8..10)
        assertThat(recommendation.rationale).contains("Progressing load to 62.5 kg")
    }

    @Test
    fun double_progression_maintains_load_when_rir_is_zero_despite_hitting_top_reps() {
        val targetRange = 8..12
        // User hits 60 kg x 12/12/12 but at 0 RIR (true failure)
        val sets = listOf(
            createSet(1, 60.0, 12, rir = 1),
            createSet(2, 60.0, 12, rir = 0),
            createSet(3, 60.0, 12, rir = 0)
        )

        val recommendation = ProgressionEngine.computeRecommendation(
            exerciseId = "barbell_bench_press",
            targetRepRange = targetRange,
            recentSessionsSets = listOf(sets),
            equipmentId = "barbell",
            availableIncrementKg = 2.5
        )

        // Do NOT increase load
        assertThat(recommendation.isLoadIncreased).isFalse()
        assertThat(recommendation.recommendedWeightKg).isEqualTo(60.0)
        assertThat(recommendation.rationale).contains("Consolidate at 60.0 kg")
    }

    @Test
    fun double_progression_recommends_adding_reps_when_inside_target_range() {
        val targetRange = 8..12
        // User hits 60 kg x 10, 10, 9 with 2 RIR
        val sets = listOf(
            createSet(1, 60.0, 10, rir = 2),
            createSet(2, 60.0, 10, rir = 2),
            createSet(3, 60.0, 9, rir = 2)
        )

        val recommendation = ProgressionEngine.computeRecommendation(
            exerciseId = "barbell_bench_press",
            targetRepRange = targetRange,
            recentSessionsSets = listOf(sets),
            equipmentId = "barbell",
            availableIncrementKg = 2.5
        )

        assertThat(recommendation.isLoadIncreased).isFalse()
        assertThat(recommendation.recommendedWeightKg).isEqualTo(60.0)
        assertThat(recommendation.rationale).contains("target +1 rep")
    }

    @Test
    fun intra_session_rep_collapse_flags_fatigue_and_maintains_load() {
        val targetRange = 8..12
        // User hits 60 kg x 10, 7, 5 (severe drop across sets)
        val sets = listOf(
            createSet(1, 60.0, 10, rir = 2),
            createSet(2, 60.0, 7, rir = 1),
            createSet(3, 60.0, 5, rir = 0)
        )

        val recommendation = ProgressionEngine.computeRecommendation(
            exerciseId = "barbell_bench_press",
            targetRepRange = targetRange,
            recentSessionsSets = listOf(sets),
            equipmentId = "barbell"
        )

        assertThat(recommendation.fatigueRiskDetected).isTrue()
        assertThat(recommendation.recommendedWeightKg).isEqualTo(60.0)
        assertThat(recommendation.rationale).contains("Reps dropped from 10 to 5")
    }

    @Test
    fun sustained_performance_decline_across_sessions_flags_fatigue_and_advises_deload() {
        val targetRange = 8..12
        // Previous session: 60 kg x 11, 11, 10 (avg 10.6 reps)
        val prevSets = listOf(
            createSet(1, 60.0, 11, rir = 2),
            createSet(2, 60.0, 11, rir = 2),
            createSet(3, 60.0, 10, rir = 2)
        )

        // Current session: 60 kg x 7, 6, 5 (avg 6.0 reps, drop > 4 reps)
        val currSets = listOf(
            createSet(1, 60.0, 7, rir = 0),
            createSet(2, 60.0, 6, rir = 0),
            createSet(3, 60.0, 5, rir = 0)
        )

        val recommendation = ProgressionEngine.computeRecommendation(
            exerciseId = "barbell_bench_press",
            targetRepRange = targetRange,
            recentSessionsSets = listOf(currSets, prevSets),
            equipmentId = "barbell"
        )

        assertThat(recommendation.fatigueRiskDetected).isTrue()
        assertThat(recommendation.isDeloadRecommended).isTrue()
        assertThat(recommendation.rationale).contains("Performance declined")
    }

    @Test
    fun personal_record_detection_identifies_weight_and_1rm_improvements() {
        val completedSet = createSet(1, weightKg = 100.0, reps = 5, isCompleted = true)
        val existingPr = ExercisePersonalRecordEntity(
            exerciseId = "barbell_bench_press",
            maxWeightKg = 95.0,
            maxRepsAtMaxWeight = 5,
            estimated1RmKg = 110.8,
            bestSetVolumeKg = 475.0
        )

        val pr = ProgressionEngine.evaluatePersonalRecord("barbell_bench_press", completedSet, existingPr)
        assertThat(pr).isNotNull()
        assertThat(pr?.isWeightPr).isTrue()
        assertThat(pr?.is1RmPr).isTrue()
        assertThat(pr?.isVolumePr).isTrue()
        assertThat(pr?.summary).contains("Max Weight: 100.0 kg")
    }
}
