package com.nuvio.app.features.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.roundToLong

internal fun Modifier.playerSurfaceCombinedGestures(
    gestureController: PlayerGestureController?,
    layoutSize: IntSize,
    sideGestureSystemEdgeExclusionPx: Float,
    playerControlsLockedState: State<Boolean>,
    touchGesturesEnabledState: State<Boolean>,
    isHoldToSpeedGestureActiveState: State<Boolean>,
    currentPositionMsState: State<Long>,
    currentDurationMsState: State<Long>,
    onSurfaceTap: State<(Offset) -> Unit>,
    onSurfaceDoubleTap: State<(Offset) -> Unit>,
    activateHoldToSpeedState: State<() -> Unit>,
    deactivateHoldToSpeedState: State<() -> Unit>,
    showHorizontalSeekPreviewState: State<(Long, Long) -> Unit>,
    showBrightnessFeedbackState: State<(Float) -> Unit>,
    showVolumeFeedbackState: State<(PlayerAudioLevel) -> Unit>,
    clearLiveGestureFeedbackState: State<() -> Unit>,
    revealLockedOverlayState: State<() -> Unit>,
    commitHorizontalSeekState: State<(Long) -> Unit>,
): Modifier =
    pointerInput(gestureController, layoutSize, sideGestureSystemEdgeExclusionPx) {
        var lastTapTime = 0L
        var lastTapPosition = Offset.Zero

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val downTime = down.uptimeMillis
            val downPos = down.position

            if (playerControlsLockedState.value) {
                var dragStarted = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        if (!dragStarted && (change.uptimeMillis - downTime) < 500) {
                            revealLockedOverlayState.value()
                        }
                        break
                    }
                    val delta = change.position - downPos
                    if (delta.getDistance() > viewConfiguration.touchSlop) {
                        dragStarted = true
                    }
                }
                return@awaitEachGesture
            }

            val controller = gestureController
            val width = size.width.toFloat().takeIf { it > 0f } ?: return@awaitEachGesture
            val height = size.height.toFloat().takeIf { it > 0f } ?: return@awaitEachGesture
            val sideGestureEdgeExclusionPx = sideGestureSystemEdgeExclusionPx
                .coerceAtMost(height * 0.25f)
            val isInSideGestureSystemEdge =
                downPos.y <= sideGestureEdgeExclusionPx ||
                    downPos.y >= height - sideGestureEdgeExclusionPx
            val region = when {
                isInSideGestureSystemEdge -> null
                downPos.x < width * PlayerLeftGestureBoundary -> PlayerSideGesture.Brightness
                downPos.x > width * PlayerRightGestureBoundary -> PlayerSideGesture.Volume
                else -> null
            }

            val initialBrightness = if (region == PlayerSideGesture.Brightness) {
                controller?.currentBrightness()
            } else {
                null
            }
            val initialVolume = if (region == PlayerSideGesture.Volume) {
                controller?.currentVolume()
            } else {
                null
            }

            var totalDx = 0f
            var totalDy = 0f
            var gestureMode: PlayerGestureMode? = null
            var verticalGestureActivationDy = 0f
            val horizontalSeekBaselineMs = currentPositionMsState.value
            var horizontalSeekPreviewMs = horizontalSeekBaselineMs
            var isHoldToSpeedActive = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                // Long press hold-to-speed trigger while finger is held down without dragging
                if (change.pressed && gestureMode == null && !isHoldToSpeedActive) {
                    val elapsed = change.uptimeMillis - downTime
                    if (elapsed >= viewConfiguration.longPressTimeoutMillis &&
                        abs(totalDx) < viewConfiguration.touchSlop &&
                        abs(totalDy) < viewConfiguration.touchSlop
                    ) {
                        isHoldToSpeedActive = true
                        activateHoldToSpeedState.value()
                    }
                }

                if (!change.pressed) {
                    if (isHoldToSpeedActive) {
                        deactivateHoldToSpeedState.value()
                    } else if (gestureMode == null) {
                        val tapTime = change.uptimeMillis
                        val isDoubleTap = (tapTime - lastTapTime) < 320L &&
                            (change.position - lastTapPosition).getDistance() < viewConfiguration.touchSlop * 2f

                        if (isDoubleTap) {
                            lastTapTime = 0L
                            lastTapPosition = Offset.Zero
                            onSurfaceDoubleTap.value(change.position)
                        } else {
                            lastTapTime = tapTime
                            lastTapPosition = change.position
                            onSurfaceTap.value(change.position)
                        }
                    }
                    break
                }

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!touchGesturesEnabledState.value) {
                    continue
                }

                if (gestureMode == null && !isHoldToSpeedActive) {
                    val verticalGestureActivationSlop = maxOf(
                        viewConfiguration.touchSlop * PlayerVerticalGestureTouchSlopMultiplier,
                        height * PlayerVerticalGestureMinHeightFraction,
                    )
                    val horizontalDominant =
                        abs(totalDx) > viewConfiguration.touchSlop &&
                            abs(totalDx) > abs(totalDy)
                    val verticalDominant =
                        abs(totalDy) > verticalGestureActivationSlop &&
                            abs(totalDy) > abs(totalDx) * PlayerVerticalGestureDominanceRatio

                    gestureMode = when {
                        horizontalDominant -> {
                            PlayerGestureMode.HorizontalSeek
                        }

                        verticalDominant && region == PlayerSideGesture.Brightness && initialBrightness != null -> {
                            verticalGestureActivationDy = totalDy
                            PlayerGestureMode.Brightness
                        }

                        verticalDominant && region == PlayerSideGesture.Volume && initialVolume != null -> {
                            verticalGestureActivationDy = totalDy
                            PlayerGestureMode.Volume
                        }

                        else -> null
                    }

                    if (gestureMode == null) {
                        continue
                    }
                }

                when (gestureMode) {
                    PlayerGestureMode.HorizontalSeek -> {
                        val sensitivitySeconds = when {
                            currentDurationMsState.value >= 3_600_000L -> 120f
                            currentDurationMsState.value >= 1_800_000L -> 90f
                            else -> 60f
                        }
                        val previewOffsetMs =
                            ((totalDx / width) * sensitivitySeconds * 1000f).roundToLong()
                        val unclampedPreviewMs = horizontalSeekBaselineMs + previewOffsetMs
                        horizontalSeekPreviewMs = currentDurationMsState.value
                            .takeIf { it > 0L }
                            ?.let { durationMs ->
                                unclampedPreviewMs.coerceIn(0L, durationMs)
                            }
                            ?: unclampedPreviewMs.coerceAtLeast(0L)
                        showHorizontalSeekPreviewState.value(
                            horizontalSeekPreviewMs,
                            horizontalSeekBaselineMs,
                        )
                        change.consume()
                    }

                    PlayerGestureMode.Brightness -> {
                        val activeTotalDy = totalDy - verticalGestureActivationDy
                        val gestureDeltaFraction =
                            (-activeTotalDy / height) * PlayerVerticalGestureSensitivity
                        controller?.setBrightness((initialBrightness ?: 0f) + gestureDeltaFraction)
                            ?.let(showBrightnessFeedbackState.value)
                        change.consume()
                    }

                    PlayerGestureMode.Volume -> {
                        val activeTotalDy = totalDy - verticalGestureActivationDy
                        val gestureDeltaFraction =
                            (-activeTotalDy / height) * PlayerVerticalGestureSensitivity
                        controller?.setVolume((initialVolume?.fraction ?: 0f) + gestureDeltaFraction)
                            ?.let(showVolumeFeedbackState.value)
                        change.consume()
                    }

                    null -> {}
                }
            }

            if (gestureMode == PlayerGestureMode.HorizontalSeek && !isHoldToSpeedActive) {
                commitHorizontalSeekState.value(horizontalSeekPreviewMs)
                clearLiveGestureFeedbackState.value()
            }
        }
    }

