package com.nuvio.app.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.nuvio.app.isDesktop

/**
 * Enhanced horizontal scroll modifier for Nuvio-Kai.
 *
 * On mobile/touch devices, applies standard horizontal scroll.
 * On desktop devices, adds:
 * 1. Left-click & drag scrolling via mouse.
 * 2. Mouse-wheel scrolling (vertical mouse wheel rolls horizontally without requiring Shift).
 */
fun Modifier.nuvioHorizontalScroll(
    state: ScrollState,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    var modifier = this.horizontalScroll(state)
    if (!isDesktop) return modifier

    // 1. Mouse click-and-drag panning
    modifier = modifier.nuvioDesktopDragScroll(state)

    // 2. Mouse wheel vertical-to-horizontal scrolling
    modifier = modifier.pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull()
                val scrollDelta = change?.scrollDelta
                if (scrollDelta != null && (scrollDelta.y != 0f || scrollDelta.x != 0f)) {
                    val delta = if (scrollDelta.x != 0f) scrollDelta.x else scrollDelta.y
                    state.dispatchRawDelta(delta * 35f)
                    change.consume()
                }
            }
        }
    }

    return modifier
}
