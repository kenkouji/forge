package com.forge.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Immutable

/**
 * Centralized FORGE motion tokens.
 *
 * Spring tuning constants, durations and easings for the premium motion
 * language. Springs should feel physical — never bouncy or slow. Use these
 * with androidx.compose.animation.core.spring<T>(dampingRatio, stiffness).
 */
@Immutable
object ForgeMotion {
    // Easings
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val StandardEasing = FastOutSlowInEasing

    // Durations (ms)
    const val DurationInstant = 100
    const val DurationQuick = 180
    const val DurationShort = 240
    const val DurationMedium = 360
    const val DurationLong = 500
    const val DurationExtended = 700

    // Page / sheet transitions
    const val PageTransitionDuration = 360
    const val SheetTransitionDuration = 300
    const val NavIndicatorDuration = 300

    // Spring tuning constants (pass to spring<T>())
    const val SpringDampingNone = Spring.DampingRatioNoBouncy
    const val SpringDampingLow = Spring.DampingRatioLowBouncy
    const val SpringDampingMedium = Spring.DampingRatioMediumBouncy
    const val SpringStiffnessHigh = Spring.StiffnessHigh
    const val SpringStiffnessMedium = Spring.StiffnessMedium
    const val SpringStiffnessLow = Spring.StiffnessMediumLow
    const val SpringStiffnessVeryLow = Spring.StiffnessVeryLow
}
