package flasher

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val testDecks = listOf(
    Deck("greetings", "Greetings", listOf(Card("hello", "hola"), Card("bye", "adiós"))),
    Deck("colors", "Colors", listOf(Card("red", "rojo"))),
)

class DomRendererTest {

    private val mounted = mutableListOf<HTMLElement>()

    @AfterTest
    fun cleanup() {
        mounted.forEach { it.remove() }
        mounted.clear()
    }

    private fun mount(): Pair<HTMLElement, FlashcardController> {
        val root = document.createElement("div") as HTMLElement
        document.body!!.appendChild(root)
        mounted += root
        val renderer = DomRenderer(root)
        val controller = FlashcardController(testDecks, onChange = renderer::render)
        renderer.bind(controller)
        controller.resume() // no store -> Home
        return root to controller
    }

    private fun HTMLElement.click(selector: String) {
        val el = querySelector(selector) as? HTMLElement
        assertNotNull(el, "no element matching '$selector'")
        el.click()
    }

    @Test
    fun homeListsEveryDeckWithCount() {
        val (root, _) = mount()
        val items = root.querySelectorAll(".deck-item")
        assertEquals(2, items.length)
        val text = root.textContent!!
        assertContains(text, "Greetings")
        assertContains(text, "2 cards")
        assertContains(text, "Colors")
    }

    @Test
    fun clickingADeckOpensItsOptions() {
        val (root, controller) = mount()
        root.click(".deck-item") // first deck = greetings
        assertEquals(Screen.DeckOptions, controller.state.screen)
        assertContains(root.textContent!!, "Greetings")
        assertNotNull(root.querySelector(".toggle"))
        assertNotNull(root.querySelector(".primary"))
    }

    @Test
    fun shuffleToggleFlipsLabelAndState() {
        val (root, controller) = mount()
        root.click(".deck-item")
        assertContains(root.textContent!!, "Shuffle: Off")
        root.click(".toggle")
        assertTrue(controller.state.shuffleOn)
        assertContains(root.textContent!!, "Shuffle: On")
    }

    @Test
    fun startMovesIntoStudy() {
        val (root, controller) = mount()
        root.click(".deck-item")
        root.click(".primary") // Start
        assertEquals(Screen.Study, controller.state.screen)
    }

    @Test
    fun backFromOptionsReturnsHome() {
        val (root, controller) = mount()
        root.click(".deck-item")
        root.click(".link") // ‹ Decks
        assertEquals(Screen.Home, controller.state.screen)
    }
}
