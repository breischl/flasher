package flasher

import kotlinx.browser.document
import org.w3c.dom.EventInit
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val inputDecks = listOf(
    Deck("d", "D", listOf(Card("one", "uno"), Card("two", "dos"), Card("three", "tres"))),
)

class InputHandlerTest {

    private val mounted = mutableListOf<HTMLElement>()

    @AfterTest
    fun cleanup() {
        mounted.forEach { it.remove() }
        mounted.clear()
    }

    /** Mount a study session wired to an InputHandler on the root element. */
    private fun mountStudying(): Pair<HTMLElement, FlashcardController> {
        val root = document.createElement("div") as HTMLElement
        document.body!!.appendChild(root)
        mounted += root
        val renderer = DomRenderer(root, immediateScope())
        val controller = FlashcardController(
            summariesOf(inputDecks),
            loadDeck = loaderOf(inputDecks),
            onChange = renderer::render,
        )
        renderer.bind(controller)
        InputHandler(controller).attach(root)
        runSync { controller.selectDeck("d") }
        controller.start()
        return root to controller
    }

    private fun HTMLElement.key(key: String) {
        dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = key, bubbles = true, cancelable = true)))
    }

    private fun HTMLElement.touch(type: String, x: Double, y: Double, prop: String) {
        val event = Event(type, EventInit(bubbles = true, cancelable = true))
        val point = js("({})")
        point.clientX = x
        point.clientY = y
        event.asDynamic()[prop] = arrayOf(point)
        dispatchEvent(event)
    }

    @Test
    fun arrowRightAdvancesArrowLeftGoesBack() {
        val (root, controller) = mountStudying()
        root.key("ArrowRight")
        assertEquals(1, controller.state.position)
        root.key("ArrowLeft")
        assertEquals(0, controller.state.position)
    }

    @Test
    fun spaceFlipsTheCard() {
        val (root, controller) = mountStudying()
        assertTrue(!controller.state.isFlipped)
        root.key(" ")
        assertTrue(controller.state.isFlipped)
    }

    @Test
    fun keysAreIgnoredOutsideStudy() {
        val (root, controller) = mountStudying()
        controller.goHome()
        root.key("ArrowRight")
        assertEquals(Screen.Home, controller.state.screen)
    }

    @Test
    fun swipeLeftGoesToNextCard() {
        val (root, controller) = mountStudying()
        root.touch("touchstart", x = 220.0, y = 100.0, prop = "touches")
        root.touch("touchend", x = 120.0, y = 108.0, prop = "changedTouches")
        assertEquals(1, controller.state.position)
    }

    @Test
    fun swipeRightGoesToPreviousCard() {
        val (root, controller) = mountStudying()
        controller.next() // now on card 1
        root.touch("touchstart", x = 120.0, y = 100.0, prop = "touches")
        root.touch("touchend", x = 240.0, y = 96.0, prop = "changedTouches")
        assertEquals(0, controller.state.position)
    }

    @Test
    fun smallMovementIsATapNotASwipe() {
        val (root, controller) = mountStudying()
        root.touch("touchstart", x = 120.0, y = 100.0, prop = "touches")
        root.touch("touchend", x = 130.0, y = 104.0, prop = "changedTouches")
        assertEquals(0, controller.state.position) // unchanged
    }
}
