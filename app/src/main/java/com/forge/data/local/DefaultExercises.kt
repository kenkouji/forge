package com.forge.data.local

import com.forge.data.local.entity.ExerciseAliasEntity
import com.forge.data.local.entity.ExerciseEntity

object DefaultExercises {
    val list = listOf(
        ExerciseEntity(
            id = "barbell_bench_press",
            name = "Barbell Bench Press",
            canonicalName = "Barbell Bench Press",
            movementPattern = "PUSH",
            mechanic = "COMPOUND",
            forceType = "PUSH",
            experienceLevel = "INTERMEDIATE",
            instructions = "Lie flat on the bench. Lower the barbell with control to your chest, then press explosively upward.",
            formCues = "Retract scapulae. Keep feet planted on the floor.",
            commonMistakes = "Bouncing the bar off the ribcage. Flaring elbows 90 degrees."
        ),
        ExerciseEntity(
            id = "incline_dumbbell_press",
            name = "Incline Dumbbell Press",
            canonicalName = "Incline Dumbbell Press",
            movementPattern = "PUSH",
            mechanic = "COMPOUND",
            forceType = "PUSH",
            experienceLevel = "INTERMEDIATE",
            instructions = "Set bench to 30 degrees incline. Press dumbbells upward directly above chest.",
            formCues = "Control the eccentric descent.",
            commonMistakes = "Setting incline too steep (shifting focus to front delts)."
        ),
        ExerciseEntity(
            id = "barbell_squat",
            name = "Barbell Back Squat",
            canonicalName = "Barbell Back Squat",
            movementPattern = "SQUAT",
            mechanic = "COMPOUND",
            forceType = "PUSH",
            experienceLevel = "INTERMEDIATE",
            instructions = "Bar rested on upper traps. Squat down until hips drop below knee crease, then drive upward.",
            formCues = "Chest tall, knees tracking over toes, brace core.",
            commonMistakes = "Knees caving inward (valgus collapse). Rounding lumbar spine."
        ),
        ExerciseEntity(
            id = "romanian_deadlift",
            name = "Romanian Deadlift",
            canonicalName = "Romanian Deadlift",
            movementPattern = "HINGE",
            mechanic = "COMPOUND",
            forceType = "PULL",
            experienceLevel = "INTERMEDIATE",
            instructions = "Hinge back at the hips with soft knees until feeling a stretch in hamstrings. Drive hips forward to stand.",
            formCues = "Keep bar close to shins. Neutral spine.",
            commonMistakes = "Bending lower back instead of hinging hips."
        ),
        ExerciseEntity(
            id = "pull_up",
            name = "Pull-Up",
            canonicalName = "Pull-Up",
            movementPattern = "PULL",
            mechanic = "COMPOUND",
            forceType = "PULL",
            experienceLevel = "INTERMEDIATE",
            instructions = "Overhand grip slightly wider than shoulders. Pull chest to the bar.",
            formCues = "Drive elbows down toward hips.",
            commonMistakes = "Kicking legs or using momentum (kipping)."
        ),
        ExerciseEntity(
            id = "barbell_bent_over_row",
            name = "Barbell Bent-Over Row",
            canonicalName = "Barbell Bent-Over Row",
            movementPattern = "PULL",
            mechanic = "COMPOUND",
            forceType = "PULL",
            experienceLevel = "INTERMEDIATE",
            instructions = "Hinged forward at 45 degrees. Pull bar smoothly into lower abdomen.",
            formCues = "Squeeze shoulder blades at top.",
            commonMistakes = "Standing too upright. Jerking the torso."
        ),
        ExerciseEntity(
            id = "overhead_barbell_press",
            name = "Overhead Barbell Press",
            canonicalName = "Overhead Barbell Press",
            movementPattern = "PUSH",
            mechanic = "COMPOUND",
            forceType = "PUSH",
            experienceLevel = "INTERMEDIATE",
            instructions = "Standing with feet hip-width. Press bar vertically overhead, locking out elbows with head through.",
            formCues = "Squeeze glutes and brace core.",
            commonMistakes = "Excessive lumbar arching."
        ),
        ExerciseEntity(
            id = "dumbbell_lateral_raise",
            name = "Dumbbell Lateral Raise",
            canonicalName = "Dumbbell Lateral Raise",
            movementPattern = "ISOLATION",
            mechanic = "ISOLATION",
            forceType = "PULL",
            experienceLevel = "BEGINNER",
            instructions = "Raise dumbbells out to the sides in scapular plane until parallel with floor.",
            formCues = "Lead with elbows. Slight forward torso lean.",
            commonMistakes = "Shrugging traps up to ears."
        ),
        ExerciseEntity(
            id = "dumbbell_bicep_curl",
            name = "Dumbbell Bicep Curl",
            canonicalName = "Dumbbell Bicep Curl",
            movementPattern = "ISOLATION",
            mechanic = "ISOLATION",
            forceType = "PULL",
            experienceLevel = "BEGINNER",
            instructions = "Curl dumbbells upward while supinating wrists.",
            formCues = "Pin elbows at sides.",
            commonMistakes = "Swinging torso backward for momentum."
        ),
        ExerciseEntity(
            id = "tricep_rope_pushdown",
            name = "Tricep Rope Pushdown",
            canonicalName = "Tricep Rope Pushdown",
            movementPattern = "ISOLATION",
            mechanic = "ISOLATION",
            forceType = "PUSH",
            experienceLevel = "BEGINNER",
            instructions = "Push rope attachment down, spreading ends apart at full contraction.",
            formCues = "Keep elbows tucked against ribs.",
            commonMistakes = "Letting elbows flare or travel forward."
        )
    )

    val aliases = listOf(
        ExerciseAliasEntity(exerciseId = "barbell_bench_press", alias = "Flat Bench"),
        ExerciseAliasEntity(exerciseId = "barbell_bench_press", alias = "BB Bench"),
        ExerciseAliasEntity(exerciseId = "barbell_bench_press", alias = "Bench Press"),
        ExerciseAliasEntity(exerciseId = "barbell_squat", alias = "Squat"),
        ExerciseAliasEntity(exerciseId = "barbell_squat", alias = "Back Squat"),
        ExerciseAliasEntity(exerciseId = "pull_up", alias = "Pullups"),
        ExerciseAliasEntity(exerciseId = "overhead_barbell_press", alias = "OHP"),
        ExerciseAliasEntity(exerciseId = "overhead_barbell_press", alias = "Military Press"),
        ExerciseAliasEntity(exerciseId = "dumbbell_lateral_raise", alias = "Side Lateral"),
        ExerciseAliasEntity(exerciseId = "dumbbell_lateral_raise", alias = "Lateral Raise")
    )
}
