package com.anatdx.andronitor.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView

/**
 * Adds a light haptic tick after a descendant consumes a short tap.
 *
 * Listening on the final pointer pass is what lets one modifier at the root of
 * the theme cover every control: by then a real interaction has been consumed,
 * so a tap on empty space can be told apart from a tap on a button. Drags and
 * long presses are deliberately excluded so scrolling stays silent.
 */
@Composable
fun Modifier.clickHapticFeedback(): Modifier {
    val view = LocalView.current
    return pointerInput(view) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            val pointerId = down.id
            val start = down.position
            var consumed = down.isConsumed
            var movedPastSlop = false
            var released = false
            var releaseUptimeMillis = down.uptimeMillis

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                consumed = consumed || change.isConsumed
                if (!movedPastSlop) {
                    movedPastSlop = Offset(
                        x = change.position.x - start.x,
                        y = change.position.y - start.y,
                    ).getDistance() > viewConfiguration.touchSlop
                }

                if (change.changedToUpIgnoreConsumed()) {
                    released = true
                    releaseUptimeMillis = change.uptimeMillis
                    break
                }
                if (!change.pressed) break
            }

            val shortTap = releaseUptimeMillis - down.uptimeMillis <
                viewConfiguration.longPressTimeoutMillis
            if (released && consumed && !movedPastSlop && shortTap) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }
}
