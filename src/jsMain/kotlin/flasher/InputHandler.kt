package flasher

import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.abs

/**
 * Translates keyboard and touch-swipe gestures into controller actions while studying.
 * Attaches to an [EventTarget] (the window in the app) so it can be pointed at a test root.
 *
 * - ArrowLeft / ArrowRight → prev / next
 * - Space / Enter → flip
 * - horizontal swipe → prev / next
 */
class InputHandler(
    private val controller: FlashcardController,
    private val swipeThreshold: Double = 50.0,
) {
    private var startX = 0.0
    private var startY = 0.0

    fun attach(target: EventTarget) {
        target.addEventListener("keydown", { onKey(it as KeyboardEvent) })
        target.addEventListener("touchstart", { onTouchStart(it) })
        // Non-passive so a handled swipe can suppress the synthesized click.
        target.addEventListener("touchend", { onTouchEnd(it) }, js("({ passive: false })"))
    }

    private fun onKey(event: KeyboardEvent) {
        if (controller.state.screen != Screen.Study) return
        when (event.key) {
            "ArrowLeft" -> controller.prev()
            "ArrowRight" -> controller.next()
            " ", "Spacebar", "Enter" -> controller.flip()
            else -> return
        }
        event.preventDefault()
    }

    private fun onTouchStart(event: Event) {
        val touch = event.asDynamic().touches[0] ?: return
        startX = (touch.clientX as Number).toDouble()
        startY = (touch.clientY as Number).toDouble()
    }

    private fun onTouchEnd(event: Event) {
        if (controller.state.screen != Screen.Study) return
        val touch = event.asDynamic().changedTouches[0] ?: return
        val dx = (touch.clientX as Number).toDouble() - startX
        val dy = (touch.clientY as Number).toDouble() - startY
        if (abs(dx) < swipeThreshold || abs(dx) < abs(dy)) return
        event.preventDefault()
        if (dx < 0) controller.next() else controller.prev()
    }
}
