package com.masterbit.podcastslidercompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import kotlin.math.roundToInt

@Stable
interface PodcastSliderState {
    val currentValue: Float
    val range: ClosedRange<Int>

    suspend fun snapTo(currentValue: Float)
    suspend fun decayTo(velocity: Float, currentValue: Float)
    suspend fun stop()
}

class PodcastSliderStateImpl(
    currentValue: Float,
    override val range: ClosedRange<Int>
): PodcastSliderState {

    private val decayAnimationSpec = FloatSpringSpec(
        Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    private val floatRange = range.start.toFloat()..range.endInclusive.toFloat()
    private val animatable = Animatable(currentValue)

    override val currentValue: Float
        get() = animatable.value

    override suspend fun snapTo(currentValue: Float) {
        animatable.snapTo(currentValue.coerceIn(floatRange))
    }

    override suspend fun decayTo(velocity: Float, currentValue: Float) {
        val targetValue = currentValue.roundToInt().coerceIn(range)
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = decayAnimationSpec,
            initialVelocity = velocity
        )
    }

    override suspend fun stop() {
        animatable.stop()
    }

    companion object {
        val Saver = Saver<PodcastSliderStateImpl, List<Any>>(save = {
            listOf(it.currentValue, it.range.start, it.range.endInclusive)
        }, restore = {
            PodcastSliderStateImpl(it[0] as Float, (it[1] as Int).. (it[2] as Int))
        })
    }
}