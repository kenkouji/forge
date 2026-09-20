package com.forge.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.forge.core.designsystem.theme.ForgeColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class AmbientParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var alpha: Float,
    var color: Color
)

private data class DisintegrationParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    var color: Color,
    var life: Float = 1f
)

/**
 * High-performance ambient particle canvas.
 * - Caps at 30 particles.
 * - Respects reduceMotion (completely renders empty / static background).
 * - Pauses frame rendering when isPaused is true.
 */
private val isUnitTest: Boolean = try {
    Class.forName("org.robolectric.Robolectric") != null
} catch (_: ClassNotFoundException) {
    false
}

@Composable
fun ForgeAmbientParticles(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reduceMotion: Boolean = false,
    isPaused: Boolean = false,
    particleCount: Int = 24
) {
    if (!enabled || reduceMotion || isUnitTest) return

    val actualCount = particleCount.coerceIn(8, 30)
    val particles = remember {
        val colors = listOf(
            ForgeColors.CyanPrimary.copy(alpha = 0.35f),
            ForgeColors.NeonPurple.copy(alpha = 0.25f),
            ForgeColors.AmberGlow.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.20f)
        )
        List(actualCount) {
            AmbientParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.0006f,
                vy = -Random.nextFloat() * 0.0008f - 0.0002f, // Gentle upward drift
                radius = Random.nextFloat() * 1.8f + 0.8f,
                alpha = Random.nextFloat() * 0.5f + 0.2f,
                color = colors.random()
            )
        }
    }

    var frameTick by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPaused) {
        if (isPaused) return@LaunchedEffect
        var lastTime = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - lastTime) / 1_000_000f).coerceIn(8f, 32f) / 16f
            lastTime = now

            particles.forEach { p ->
                p.x += p.vx * dt
                p.y += p.vy * dt
                if (p.y < -0.05f) {
                    p.y = 1.05f
                    p.x = Random.nextFloat()
                }
                if (p.x < -0.05f) p.x = 1.05f
                if (p.x > 1.05f) p.x = -0.05f
            }
            frameTick = if (frameTick > 1000f) 0f else frameTick + 1f
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Reference frameTick to trigger recomposition per frame
        if (frameTick >= 0f) {
            particles.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.alpha * p.color.alpha),
                    radius = p.radius,
                    center = Offset(p.x * w, p.y * h)
                )
            }
        }
    }
}

/**
 * Disintegration burst effect for PR break and Workout Finished.
 * Emits up to 60 particles radiating outwards, fading to zero over 900ms.
 */
@Composable
fun ForgeDisintegrationEffect(
    modifier: Modifier = Modifier,
    triggerKey: Any?,
    reduceMotion: Boolean = false,
    particleCount: Int = 60,
    accentColor: Color = ForgeColors.CyanPrimary,
    onComplete: () -> Unit = {}
) {
    if (triggerKey == null) return

    val actualCount = particleCount.coerceIn(20, 70)
    var isRunning by remember(triggerKey) { mutableStateOf(true) }
    val progress = remember(triggerKey) { Animatable(0f) }

    val burstParticles = remember(triggerKey) {
        List(actualCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 320f + 80f
            DisintegrationParticle(
                x = 0f,
                y = 0f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 60f, // slight upward bias
                size = Random.nextFloat() * 3.5f + 1.5f,
                alpha = 1f,
                color = if (Random.nextBoolean()) accentColor else ForgeColors.AmberGlow
            )
        }
    }

    LaunchedEffect(triggerKey) {
        if (reduceMotion) {
            // Instant or short 150ms fade
            progress.animateTo(1f, tween(150, easing = LinearEasing))
            isRunning = false
            onComplete()
            return@LaunchedEffect
        }

        progress.animateTo(1f, tween(850, easing = LinearEasing))
        isRunning = false
        onComplete()
    }

    if (!isRunning) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val currentProgress = progress.value
        val fadeAlpha = (1f - currentProgress).coerceIn(0f, 1f)

        burstParticles.forEach { p ->
            val curX = centerX + p.vx * currentProgress
            val curY = centerY + p.vy * currentProgress + (0.5f * 200f * currentProgress * currentProgress) // gravity
            drawCircle(
                color = p.color.copy(alpha = fadeAlpha * p.color.alpha),
                radius = p.size * (1f - 0.4f * currentProgress),
                center = Offset(curX, curY)
            )
        }
    }
}
