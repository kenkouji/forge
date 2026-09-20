package com.forge.domain.engine

import java.util.UUID

data class ParsedSetInfo(
    val sets: Int = 1,
    val repsMin: Int,
    val repsMax: Int,
    val weightKg: Double? = null,
    val rir: Int? = null,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val isDropSet: Boolean = false,
    val isAmrap: Boolean = false,
    val restSeconds: Int? = null,
    val notes: String? = null
) {
    val reps: Int get() = repsMax
    val setType: String get() = when {
        isWarmup -> "WARMUP"
        isDropSet -> "DROP_SET"
        else -> "NORMAL"
    }
}

data class ParsedExerciseItem(
    val exerciseName: String,
    val matchedExerciseId: String? = null,
    val sets: List<ParsedSetInfo> = emptyList(),
    val setsInfo: ParsedSetInfo = sets.firstOrNull() ?: ParsedSetInfo(1, 10, 10)
)

data class ParsedDayRoutine(
    val dayName: String, // e.g., "Monday", "Push Day"
    val dayOfWeek: Int?, // 1 = Monday ... 7 = Sunday if specified
    val focus: String,
    val exercises: List<ParsedExerciseItem>
)

data class ParsedWorkoutProgram(
    val routines: List<ParsedDayRoutine>,
    val rawText: String
) {
    val exercises: List<ParsedExerciseItem> get() = routines.flatMap { it.exercises }
}

typealias ParsedWorkout = ParsedWorkoutProgram

/**
 * WorkoutTextParser
 * Strictly deterministic parser for user-pasted routines.
 * CRITICAL RULE: Zero value invention. If weight or RIR is unspecified, it remains NULL.
 */
class WorkoutTextParser {

    companion object {
        fun parse(text: String): ParsedWorkoutProgram = WorkoutTextParser().parse(text)
    }

    private val dayOfWeekRegex = Regex(
        "(?i)\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\\b"
    )

    private val dayHeaderRegex = Regex(
        "(?i)^(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun|day\\s*\\d+|session\\s*\\d+|push|pull|legs|upper|lower)"
    )

    // Patterns like: "3x10", "3 x 10", "3x8-12", "3 sets of 10", "4 x 6-8 @ 80kg"
    private val setsRepsRegex = Regex(
        "(?i)(\\d+)\\s*(?:x|×|sets?\\s*(?:of)?)\\s*(\\d+)(?:\\s*[-–]\\s*(\\d+))?(?:\\s*reps?)?(?:\\s*@?\\s*(\\d+(?:\\.\\d+)?)\\s*kg)?"
    )

    private val weightRegex = Regex("(?i)\\b(\\d+(?:\\.\\d+)?)\\s*kg\\b")
    private val rirRegex = Regex("(?i)\\b(?:rir|rir:)\\s*(\\d+)\\b")
    private val rpeRegex = Regex("(?i)\\b(?:rpe|rpe:)\\s*(\\d+(?:\\.\\d+)?)\\b")
    private val restRegex = Regex("(?i)\\b(\\d+)\\s*(?:s|sec|seconds?)\\s*(?:rest)?\\b")
    private val warmupRegex = Regex("(?i)\\b(warmup|warm-up|warm\\s*up)\\b")
    private val dropsetRegex = Regex("(?i)\\b(dropset|drop-set|drop\\s*set|drop)\\b")
    private val amrapRegex = Regex("(?i)\\b(amrap|to\\s*failure|failure)\\b")

    fun parse(text: String, canonicalExercises: List<Pair<String, String>> = emptyList()): ParsedWorkoutProgram {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val routines = mutableListOf<ParsedDayRoutine>()

        var currentDayName = "Session 1"
        var currentFocus = "Full Body"
        var currentDayOfWeek: Int? = null
        val currentDayExercises = mutableListOf<ParsedExerciseItem>()

        var currentExerciseName: String? = null
        val currentExerciseSets = mutableListOf<ParsedSetInfo>()

        fun finalizeExercise() {
            val name = currentExerciseName ?: return
            if (currentExerciseSets.isNotEmpty()) {
                val matchedId = matchCanonicalExercise(name, canonicalExercises)
                currentDayExercises.add(
                    ParsedExerciseItem(
                        exerciseName = name,
                        matchedExerciseId = matchedId,
                        sets = currentExerciseSets.toList(),
                        setsInfo = currentExerciseSets.first()
                    )
                )
                currentExerciseSets.clear()
            }
            currentExerciseName = null
        }

        fun finalizeDay() {
            finalizeExercise()
            if (currentDayExercises.isNotEmpty()) {
                routines.add(
                    ParsedDayRoutine(
                        dayName = currentDayName,
                        dayOfWeek = currentDayOfWeek,
                        focus = currentFocus,
                        exercises = currentDayExercises.toList()
                    )
                )
                currentDayExercises.clear()
            }
        }

        for (line in lines) {
            val hasSetsReps = setsRepsRegex.containsMatchIn(line)
            val isDayHeader = !hasSetsReps && (dayHeaderRegex.containsMatchIn(line) || (line.contains("+") && !line.contains("kg")))

            if (isDayHeader) {
                finalizeDay()
                val cleanHeader = line.trimEnd(':').trim()
                currentDayName = cleanHeader
                currentDayOfWeek = extractDayOfWeek(cleanHeader)
                currentFocus = extractFocus(cleanHeader)
            } else if (hasSetsReps) {
                val match = setsRepsRegex.find(line)!!
                val prefix = line.substring(0, match.range.first).trim().trimEnd(',', '-', '·', ':').trim()

                if (prefix.isNotEmpty()) {
                    // Single line with exercise name and sets: "Pull-ups 3x10 RIR 2"
                    finalizeExercise()
                    val sets = parseSetLine(line)
                    val matchedId = matchCanonicalExercise(prefix, canonicalExercises)
                    currentDayExercises.add(
                        ParsedExerciseItem(
                            exerciseName = prefix,
                            matchedExerciseId = matchedId,
                            sets = sets,
                            setsInfo = sets.firstOrNull() ?: ParsedSetInfo(1, 10, 10)
                        )
                    )
                } else if (currentExerciseName != null) {
                    // Line starting with set info under an exercise header: "1x5 60kg warmup"
                    val sets = parseSetLine(line)
                    currentExerciseSets.addAll(sets)
                }
            } else if (line.endsWith(":") || !hasSetsReps) {
                // Exercise header: "Deadlift:"
                finalizeExercise()
                currentExerciseName = line.trimEnd(':').trim()
            }
        }
        finalizeDay()

        if (routines.isEmpty() && currentDayExercises.isNotEmpty()) {
            routines.add(
                ParsedDayRoutine(
                    dayName = "Workout",
                    dayOfWeek = 1,
                    focus = "Full Body",
                    exercises = currentDayExercises.toList()
                )
            )
        }

        return ParsedWorkoutProgram(routines = routines, rawText = text)
    }

    private fun parseSetLine(line: String): List<ParsedSetInfo> {
        val match = setsRepsRegex.find(line) ?: return emptyList()
        val setsCount = match.groupValues[1].toIntOrNull() ?: 1
        val repsMin = match.groupValues[2].toIntOrNull() ?: 10
        val repsMax = match.groupValues[3].toIntOrNull() ?: repsMin

        // ZERO INVENTED WEIGHT: only assign weight if explicitly present
        val weightKg = match.groupValues.getOrNull(4)?.toDoubleOrNull()
            ?: weightRegex.find(line)?.groupValues?.get(1)?.toDoubleOrNull()

        val rir = rirRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
        val rpe = rpeRegex.find(line)?.groupValues?.get(1)?.toDoubleOrNull()
        val rest = restRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()

        val isWarmup = warmupRegex.containsMatchIn(line)
        val isDropSet = dropsetRegex.containsMatchIn(line)
        val isAmrap = amrapRegex.containsMatchIn(line)

        val singleSet = ParsedSetInfo(
            sets = setsCount,
            repsMin = repsMin,
            repsMax = repsMax,
            weightKg = weightKg,
            rir = rir,
            rpe = rpe,
            isWarmup = isWarmup,
            isDropSet = isDropSet,
            isAmrap = isAmrap,
            restSeconds = rest
        )
        return List(setsCount) { singleSet }
    }

    private fun extractDayOfWeek(header: String): Int? {
        val match = dayOfWeekRegex.find(header)?.value?.lowercase() ?: return null
        return when {
            match.startsWith("mon") -> 1
            match.startsWith("tue") -> 2
            match.startsWith("wed") -> 3
            match.startsWith("thu") -> 4
            match.startsWith("fri") -> 5
            match.startsWith("sat") -> 6
            match.startsWith("sun") -> 7
            else -> null
        }
    }

    private fun extractFocus(header: String): String {
        val parts = header.split(":", "-", "–").map { it.trim() }
        return if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1]
        } else {
            header
        }
    }

    private fun matchCanonicalExercise(
        query: String,
        canonicalExercises: List<Pair<String, String>>
    ): String? {
        val normalizedQuery = query.lowercase().replace("-", " ").replace("_", " ").trim()
        return canonicalExercises.firstOrNull { (_, name) ->
            val normName = name.lowercase().replace("-", " ").replace("_", " ").trim()
            normName == normalizedQuery || normName.contains(normalizedQuery) || normalizedQuery.contains(normName)
        }?.first
    }
}
