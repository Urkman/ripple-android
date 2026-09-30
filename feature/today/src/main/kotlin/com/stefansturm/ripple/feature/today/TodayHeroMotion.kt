package com.stefansturm.ripple.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.core.domain.TodaySnapshot
import kotlin.math.PI
import kotlin.math.sin

internal data class TodayHeroMotionState(
    val level: Float,
    val tiltRadians: Float,
    val pourProgress: Float?,
    val pourAddedMl: Int,
    val surfaceAmplitude: Float,
    val isAnimating: Boolean,
    val displayedConsumedMl: Milliliters
)

private enum class HeroPhase { IDLE, POURING, UNDOING }

@Composable
internal fun rememberTodayHeroMotion(
    snapshot: TodaySnapshot,
    visualEvent: TodayVisualEvent?,
    tiltRadians: Float,
    reducedMotion: Boolean,
    onAnimationStateChanged: (Boolean) -> Unit
): TodayHeroMotionState {
    val initialLevel = progress(snapshot.consumedMl.value, snapshot.goalMl.value)
    var level by remember { mutableFloatStateOf(initialLevel) }
    var phase by remember { mutableStateOf(HeroPhase.IDLE) }
    var animationSequence by remember { mutableLongStateOf(0L) }
    var animationStartLevel by remember { mutableFloatStateOf(initialLevel) }
    var animationTargetLevel by remember { mutableFloatStateOf(initialLevel) }
    var displayedConsumedMl by remember { mutableIntStateOf(snapshot.consumedMl.value) }
    var animationStartConsumedMl by remember { mutableIntStateOf(snapshot.consumedMl.value) }
    var animationTargetConsumedMl by remember { mutableIntStateOf(snapshot.consumedMl.value) }
    var activeAddedMl by remember { mutableIntStateOf(0) }
    var pourProgress by remember { mutableStateOf<Float?>(null) }
    var surfaceAmplitude by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visualEvent?.sequence, reducedMotion) {
        val event = visualEvent ?: return@LaunchedEffect
        val nextTarget = progress(event.targetConsumedMl.value, event.targetGoalMl.value)
        animationStartLevel = level
        animationTargetLevel = nextTarget
        animationStartConsumedMl = displayedConsumedMl
        animationTargetConsumedMl = event.targetConsumedMl.value
        val targetDeltaMl = (event.targetConsumedMl.value - displayedConsumedMl).coerceAtLeast(0)
        activeAddedMl = when {
            event.kind == TodayVisualEventKind.ADD -> maxOf(event.amountMl.value, targetDeltaMl)
            else -> 0
        }
        if (reducedMotion) {
            level = nextTarget
            displayedConsumedMl = event.targetConsumedMl.value
            pourProgress = null
            surfaceAmplitude = 0f
            phase = HeroPhase.IDLE
        } else {
            phase = if (event.kind == TodayVisualEventKind.ADD) HeroPhase.POURING else HeroPhase.UNDOING
            animationSequence += 1L
        }
    }

    LaunchedEffect(animationSequence) {
        if (animationSequence == 0L || reducedMotion) return@LaunchedEffect
        val runningPhase = phase
        val startLevel = animationStartLevel
        val endLevel = animationTargetLevel
        val startConsumed = animationStartConsumedMl
        val endConsumed = animationTargetConsumedMl
        val addedMl = activeAddedMl
        val amountT = ((addedMl - 50) / 700f).coerceIn(0f, 1f)
        val activePourMs = 400f + 300f * amountT
        val streamLeadInMs = 140f
        val streamFadeMs = 140f
        val streamDurationMs = streamLeadInMs + activePourMs + streamFadeMs
        val animationDurationMs = if (runningPhase == HeroPhase.POURING) streamDurationMs + 900f else 450f
        var elapsedMs = 0f
        var lastFrameNanos = 0L

        while (elapsedMs < animationDurationMs) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos != 0L) {
                elapsedMs += ((frameNanos - lastFrameNanos) / 1_000_000f).coerceAtMost(80f)
            }
            lastFrameNanos = frameNanos
            val normalized = (elapsedMs / animationDurationMs).coerceIn(0f, 1f)
            if (runningPhase == HeroPhase.POURING) {
                val levelProgress = ((elapsedMs - streamLeadInMs) / (activePourMs + streamFadeMs)).coerceIn(0f, 1f)
                level = lerp(startLevel, endLevel, levelProgress)
                displayedConsumedMl = startConsumed
                pourProgress = (elapsedMs / streamDurationMs).coerceIn(0f, 1f)
                surfaceAmplitude = if (elapsedMs < streamLeadInMs + activePourMs) {
                    -0.025f
                } else {
                    val settleProgress = ((elapsedMs - streamDurationMs) / 900f).coerceIn(0f, 1f)
                    val amplitude = sin(settleProgress * PI.toFloat() * 2f) * (1f - settleProgress)
                    amplitude * if (endLevel < 0.15f) 0.02f else 0.06f
                }
            } else {
                level = lerp(startLevel, endLevel, easeInOut(normalized))
                displayedConsumedMl = if (normalized < 1f) startConsumed else endConsumed
                pourProgress = null
                surfaceAmplitude = 0f
            }
        }

        level = endLevel
        displayedConsumedMl = endConsumed
        pourProgress = null
        surfaceAmplitude = 0f
        activeAddedMl = 0
        phase = HeroPhase.IDLE
    }

    LaunchedEffect(phase) {
        onAnimationStateChanged(phase != HeroPhase.IDLE)
    }

    return TodayHeroMotionState(
        level = level.coerceIn(0f, 1f),
        tiltRadians = if (reducedMotion) 0f else tiltRadians,
        pourProgress = if (reducedMotion) null else pourProgress,
        pourAddedMl = activeAddedMl,
        surfaceAmplitude = if (reducedMotion) 0f else surfaceAmplitude,
        isAnimating = phase != HeroPhase.IDLE,
        displayedConsumedMl = Milliliters(displayedConsumedMl)
    )
}

private fun progress(consumedMl: Int, goalMl: Int): Float =
    if (goalMl <= 0) 0f else (consumedMl.toFloat() / goalMl).coerceIn(0f, 1f)

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction.coerceIn(0f, 1f)

private fun easeInOut(value: Float): Float = value * value * (3f - 2f * value)
