package eu.heha.duplicatesfinder.ui.scroll

/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalDragOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalDragOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.JvmName
import kotlin.math.roundToInt

/**
 * [CompositionLocal] used to pass [ScrollbarStyle] down the tree.
 * This value is typically set in some "Theme" composable function
 * (DesktopTheme, MaterialTheme)
 */
val LocalScrollbarStyle = staticCompositionLocalOf { defaultScrollbarStyle() }

/**
 * Defines visual style of scrollbars (thickness, shapes, colors, etc).
 * Can be passed as a parameter of scrollbar through [LocalScrollbarStyle]
 */
@Immutable
data class ScrollbarStyle(
    val minimalHeight: Dp,
    val thickness: Dp,
    val shape: Shape,
    val hoverDurationMillis: Int,
    val unhoverColor: Color,
    val hoverColor: Color
)

/**
 * Simple default [ScrollbarStyle] without applying MaterialTheme.
 */
fun defaultScrollbarStyle() = ScrollbarStyle(
    minimalHeight = 16.dp,
    thickness = 8.dp,
    shape = RoundedCornerShape(4.dp),
    hoverDurationMillis = 300,
    unhoverColor = Color.Black.copy(alpha = 0.12f),
    hoverColor = Color.Black.copy(alpha = 0.50f)
)

/**
 * Vertical scrollbar that can be attached to some scrollable
 * component (ScrollableColumn, LazyColumn) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 *
 * @param adapter [BetterScrollbarAdapter] that will be used to
 * communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the bottom of the container.
 * It is usually used in pair with `LazyColumn(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Composable
fun VerticalScrollbar(
    adapter: BetterScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) = NewScrollbar(
    newAdapter = adapter,
    modifier,
    reverseLayout,
    style,
    interactionSource,
    isVertical = true
)

/**
 * Horizontal scrollbar that can be attached to some scrollable
 * component (Modifier.verticalScroll(), LazyRow) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         HorizontalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(),
 *         )
 *     }
 *
 * @param adapter [BetterScrollbarAdapter] that will be used to
 * communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 * @param reverseLayout reverse the direction of scrolling and layout, when `true`
 * and [LazyListState.firstVisibleItemIndex] == 0 then scrollbar
 * will be at the end of the container.
 * It is usually used in pair with `LazyRow(reverseLayout = true)`
 * @param style [ScrollbarStyle] to define visual style of scrollbar
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [DragInteraction.Start] when this Scrollbar is being dragged.
 */
@Composable
fun HorizontalScrollbar(
    adapter: BetterScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) = NewScrollbar(
    newAdapter = adapter,
    modifier,
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) !reverseLayout else reverseLayout,
    style,
    interactionSource,
    isVertical = false
)

@Composable
private fun NewScrollbar(
    newAdapter: BetterScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    style: ScrollbarStyle,
    interactionSource: MutableInteractionSource,
    isVertical: Boolean
) = OldOrNewScrollbar(
    oldOrNewAdapter = newAdapter,
    newScrollbarAdapterFactory = { adapter, _ -> adapter },
    modifier = modifier,
    reverseLayout = reverseLayout,
    style = style,
    interactionSource = interactionSource,
    isVertical = isVertical
)

private typealias NewScrollbarAdapterFactory<T> = (
    adapter: T,
    trackSize: Int,
) -> BetterScrollbarAdapter

/**
 * The actual implementation of the scrollbar.
 *
 * Takes the scroll adapter (old or new) and a function that converts it to the new scrollbar
 * adapter interface. This allows both the old (left for backwards compatibility) and new
 * implementations to use the same code.
 */
@Composable
internal fun <T> OldOrNewScrollbar(
    oldOrNewAdapter: T,
    // We need an adapter factory because we can't convert an old to a new
    // adapter until we have the track/container size
    newScrollbarAdapterFactory: NewScrollbarAdapterFactory<T>,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    style: ScrollbarStyle,
    interactionSource: MutableInteractionSource,
    isVertical: Boolean,
) = with(LocalDensity.current) {
    val dragInteraction = remember { mutableStateOf<DragInteraction.Start?>(null) }
    DisposableEffect(interactionSource) {
        onDispose {
            dragInteraction.value?.let { interaction ->
                interactionSource.tryEmit(DragInteraction.Cancel(interaction))
                dragInteraction.value = null
            }
        }
    }

    var containerSize by remember { mutableStateOf(0) }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isHighlighted by remember {
        derivedStateOf {
            isHovered || dragInteraction.value is DragInteraction.Start
        }
    }

    val minimalHeight = style.minimalHeight.toPx()

    val adapter = remember(oldOrNewAdapter, containerSize) {
        newScrollbarAdapterFactory(oldOrNewAdapter, containerSize)
    }
    val coroutineScope = rememberCoroutineScope()
    val sliderAdapter = remember(
        adapter, containerSize, minimalHeight, reverseLayout, isVertical, coroutineScope
    ) {
        SliderAdapter(
            adapter,
            containerSize,
            minimalHeight,
            reverseLayout,
            isVertical,
            coroutineScope
        )
    }

    val scrollThickness = style.thickness.roundToPx()
    val measurePolicy = if (isVertical) {
        remember(sliderAdapter, scrollThickness) {
            verticalMeasurePolicy(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    } else {
        remember(sliderAdapter, scrollThickness) {
            horizontalMeasurePolicy(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    }

    val color by animateColorAsState(
        if (isHighlighted) style.hoverColor else style.unhoverColor,
        animationSpec = TweenSpec(durationMillis = style.hoverDurationMillis)
    )

    val isVisible = sliderAdapter.thumbSize < containerSize

    Layout(
        {
            Box(
                Modifier
                    .background(if (isVisible) color else Color.Transparent, style.shape)
                    .scrollbarDrag(
                        interactionSource = interactionSource,
                        draggedInteraction = dragInteraction,
                        sliderAdapter = sliderAdapter,
                    )
            )
        },
        modifier
            .hoverable(interactionSource = interactionSource)
            .scrollOnPressTrack(isVertical, reverseLayout, sliderAdapter),
        measurePolicy
    )
}

/**
 * Create and [remember] [BetterScrollbarAdapter] for
 * scrollable container with the given instance [ScrollState].
 */
@JvmName("rememberScrollbarAdapter2")
@Composable
fun rememberScrollbarAdapter(
    scrollState: ScrollState
): BetterScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * Create and [remember] [BetterScrollbarAdapter] for
 * lazy scrollable container with the given instance [LazyListState].
 */
@JvmName("rememberScrollbarAdapter2")
@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyListState,
): BetterScrollbarAdapter = remember(scrollState) {
    ScrollbarAdapter(scrollState)
}

/**
 * ScrollbarAdapter for Modifier.verticalScroll and Modifier.horizontalScroll
 *
 * [scrollState] is instance of [ScrollState] which is used by scrollable component
 *
 * Example:
 *     val state = rememberScrollState(0)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@JvmName("ScrollbarAdapter2")
fun ScrollbarAdapter(
    scrollState: ScrollState
): BetterScrollbarAdapter = ScrollableBetterScrollbarAdapter(scrollState)

/**
 * ScrollbarAdapter for lazy lists.
 *
 * [scrollState] is instance of [LazyListState] which is used by scrollable component
 *
 * Example:
 *     Box(Modifier.fillMaxSize()) {
 *         val state = rememberLazyListState()
 *
 *         LazyColumn(state = state) {
 *             ...
 *         }
 *
 *         VerticalScrollbar(
 *             adapter = rememberScrollbarAdapter(state)
 *             modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *         )
 *     }
 */
@JvmName("ScrollbarAdapter2")
fun ScrollbarAdapter(
    scrollState: LazyListState
): BetterScrollbarAdapter = LazyListBetterScrollbarAdapter(scrollState)

private val SliderAdapter.thumbPixelRange: IntRange
    get() {
        val start = position.roundToInt()
        val endExclusive = start + thumbSize.roundToInt()

        return (start until endExclusive)
    }

private val IntRange.size get() = last + 1 - first

private fun verticalMeasurePolicy(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int
) = MeasurePolicy { measurables, constraints ->
    setContainerSize(constraints.maxHeight)
    val pixelRange = sliderAdapter.thumbPixelRange
    val placeable = measurables.first().measure(
        Constraints.fixed(
            constraints.constrainWidth(scrollThickness),
            pixelRange.size
        )
    )
    layout(placeable.width, constraints.maxHeight) {
        placeable.place(0, pixelRange.first)
    }
}

private fun horizontalMeasurePolicy(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int
) = MeasurePolicy { measurables, constraints ->
    setContainerSize(constraints.maxWidth)
    val pixelRange = sliderAdapter.thumbPixelRange
    val placeable = measurables.first().measure(
        Constraints.fixed(
            pixelRange.size,
            constraints.constrainHeight(scrollThickness)
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(pixelRange.first, 0)
    }
}

private fun Modifier.scrollbarDrag(
    interactionSource: MutableInteractionSource,
    draggedInteraction: MutableState<DragInteraction.Start?>,
    sliderAdapter: SliderAdapter,
): Modifier = composed {
    val currentInteractionSource by rememberUpdatedState(interactionSource)
    val currentDraggedInteraction by rememberUpdatedState(draggedInteraction)
    val currentSliderAdapter by rememberUpdatedState(sliderAdapter)

    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val interaction = DragInteraction.Start()
            currentInteractionSource.tryEmit(interaction)
            currentDraggedInteraction.value = interaction
            currentSliderAdapter.onDragStarted()
            val isSuccess = drag(down.id) { change ->
                currentSliderAdapter.onDragDelta(change.positionChange())
                change.consume()
            }
            val finishInteraction = if (isSuccess) {
                DragInteraction.Stop(interaction)
            } else {
                DragInteraction.Cancel(interaction)
            }
            currentInteractionSource.tryEmit(finishInteraction)
            currentDraggedInteraction.value = null
        }
    }
}

private fun Modifier.scrollOnPressTrack(
    isVertical: Boolean,
    reverseLayout: Boolean,
    sliderAdapter: SliderAdapter,
) = composed {
    val coroutineScope = rememberCoroutineScope()
    val scroller = remember(sliderAdapter, coroutineScope, reverseLayout) {
        TrackPressScroller(coroutineScope, sliderAdapter, reverseLayout)
    }
    Modifier.pointerInput(scroller) {
        detectScrollViaTrackGestures(
            isVertical = isVertical,
            scroller = scroller
        )
    }
}

/**
 * Responsible for scrolling when the scrollbar track is pressed (outside the thumb).
 */
private class TrackPressScroller(
    private val coroutineScope: CoroutineScope,
    private val sliderAdapter: SliderAdapter,
    private val reverseLayout: Boolean,
) {

    /**
     * The current direction of scroll (1: down/right, -1: up/left, 0: not scrolling)
     */
    private var direction = 0

    /**
     * The currently pressed location (in pixels) on the scrollable axis.
     */
    private var offset: Float? = null

    /**
     * The job that keeps scrolling while the track is pressed.
     */
    private var job: Job? = null

    /**
     * Calculates the direction of scrolling towards the given offset (in pixels).
     */
    private fun directionOfScrollTowards(offset: Float): Int {
        val pixelRange = sliderAdapter.thumbPixelRange
        return when {
            offset < pixelRange.first -> if (reverseLayout) 1 else -1
            offset > pixelRange.last -> if (reverseLayout) -1 else 1
            else -> 0
        }
    }

    /**
     * Scrolls once towards the current offset, if it matches the direction of the current gesture.
     */
    private suspend fun scrollTowardsCurrentOffset() {
        offset?.let {
            val currentDirection = directionOfScrollTowards(it)
            if (currentDirection != direction)
                return
            with(sliderAdapter.adapter) {
                scrollTo(scrollOffset + currentDirection * viewportSize)
            }
        }
    }

    /**
     * Starts the job that scrolls continuously towards the current offset.
     */
    private fun startScrolling() {
        job?.cancel()
        job = coroutineScope.launch {
            scrollTowardsCurrentOffset()
            delay(DelayBeforeSecondScrollOnTrackPress)
            while (true) {
                scrollTowardsCurrentOffset()
                delay(DelayBetweenScrollsOnTrackPress)
            }
        }
    }

    /**
     * Invoked on the first press for a gesture.
     */
    fun onPress(offset: Float) {
        this.offset = offset
        this.direction = directionOfScrollTowards(offset)

        if (direction != 0)
            startScrolling()
    }

    /**
     * Invoked when the pointer moves while pressed during the gesture.
     */
    fun onMovePressed(offset: Float) {
        this.offset = offset
    }

    /**
     * Cleans up when the gesture finishes.
     */
    private fun cleanupAfterGesture() {
        job?.cancel()
        direction = 0
        offset = null
    }

    /**
     * Invoked when the button is released.
     */
    fun onRelease() {
        cleanupAfterGesture()
    }

    /**
     * Invoked when the gesture is cancelled.
     */
    fun onGestureCancelled() {
        cleanupAfterGesture()
        // Maybe revert to the initial position?
    }

}

/**
 * Detects the pointer events relevant for the "scroll by pressing on the track outside the thumb"
 * gesture and calls the corresponding methods in the [scroller].
 */
private suspend fun PointerInputScope.detectScrollViaTrackGestures(
    isVertical: Boolean,
    scroller: TrackPressScroller
) {
    fun Offset.onScrollAxis() = if (isVertical) y else x

    awaitEachGesture {
        val down = awaitFirstDown()
        scroller.onPress(down.position.onScrollAxis())

        while (true) {
            val drag =
                if (isVertical)
                    awaitVerticalDragOrCancellation(down.id)
                else
                    awaitHorizontalDragOrCancellation(down.id)

            if (drag == null) {
                scroller.onGestureCancelled()
                break
            } else if (!drag.pressed) {
                scroller.onRelease()
                break
            } else
                scroller.onMovePressed(drag.position.onScrollAxis())
        }
    }
}

/**
 * The delay between the 1st and 2nd scroll while the scrollbar track is pressed outside the thumb.
 */
internal const val DelayBeforeSecondScrollOnTrackPress: Long = 300L

/**
 * The delay between each subsequent (after the 2nd) scroll while the scrollbar track is pressed
 * outside the thumb.
 */
internal const val DelayBetweenScrollsOnTrackPress: Long = 100L
