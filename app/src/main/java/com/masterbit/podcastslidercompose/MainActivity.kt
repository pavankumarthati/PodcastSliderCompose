package com.masterbit.podcastslidercompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.masterbit.podcastslidercompose.ui.theme.PodcastSliderComposeTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PodcastSliderComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    PodcastSlider(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PodcastSlider(
    modifier: Modifier = Modifier,
    state: PodcastSliderState = rememberPodcastSliderState(),
    barColor: Color = MaterialTheme.colors.onSurface,
    minAlpha:Float = .25f,
    barWidth: Dp = 2.dp,
    barHeight: Dp = 24.dp,
    noSegments: Int = 12,
    currentIndicatorValue: @Composable (Int) -> Unit = {
        val value = "${it / 10}.${it % 10}x"
        Text(value, style = MaterialTheme.typography.h6)
    },
    indicatorValue: @Composable (Int) -> Unit = {
        if (it % 5 == 0) {
            val value = "${it / 10}. ${it % 10}"
            Text(value)
        }
    }
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        currentIndicatorValue(state.currentValue.roundToInt())
        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
        BoxWithConstraints(
            modifier = Modifier.drag(state, noSegments).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            val segmentWidth = maxWidth / noSegments
            val segmentWidthPx = constraints.maxWidth / noSegments

            val halfSegments = (noSegments + 1) / 2

            val start = (state.currentValue - halfSegments).roundToInt().coerceAtLeast(state.range.start)
            val end = (state.currentValue + halfSegments).roundToInt().coerceAtMost(state.range.endInclusive)

            val maxOffset = constraints.maxWidth / 2
            for(i: Int in start..end) {
                val offset = (i - state.currentValue) * segmentWidthPx
                val alpha = 1 - ((1 - minAlpha) * (offset/ maxOffset).absoluteValue)
                Column(
                    modifier = Modifier
                        .width(segmentWidth)
                        .graphicsLayer(
                            alpha = alpha,
                            translationX = offset
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(barHeight)
                            .background(color = barColor),
                    )
                    indicatorValue(i)
                }
            }
        }
    }

}

@Composable
fun rememberPodcastSliderState(
    currentValue: Float = 10f,
    range: ClosedRange<Int> = 5..30
): PodcastSliderState {
    val state = rememberSaveable(saver = PodcastSliderStateImpl.Saver, init = {
        PodcastSliderStateImpl(currentValue, range)
    })
    return state
}

@Composable
fun Modifier.drag(
    state: PodcastSliderState,
    noSegments: Int
) = pointerInput(Unit) {
    val decay = SplineBasedFloatDecayAnimationSpec(this)
    val segmentWidthPx = size.width / noSegments
    coroutineScope {
        while (true) {
            val pointerId = awaitPointerEventScope { awaitFirstDown().id }

            state.stop()

            val tracker = VelocityTracker()
            awaitPointerEventScope {
                horizontalDrag(pointerId) {
                    val targetValue = state.currentValue - (it.positionChange().x / segmentWidthPx)

                    tracker.addPosition(it.uptimeMillis, it.position)
                    // this is to handle user drag (from pointer down to drag until pointer up)
                    launch {
                        state.snapTo(targetValue)
                    }
                }
            }

            // this is to handle the decay of the value to target value after user finger is lifted.

            val velocity = tracker.calculateVelocity().x / noSegments

            val targetValue = decay.getTargetValue(state.currentValue, -velocity)

            launch {
                // -velocity, velocity are treated same (observation)
                delay(3000)
                state.decayTo(velocity = -velocity, targetValue)
            }


        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PodcastSliderComposeTheme {
        Surface(color = MaterialTheme.colors.background) {
            PodcastSlider(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}