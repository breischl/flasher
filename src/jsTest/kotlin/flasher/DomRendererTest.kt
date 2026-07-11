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
        val renderer = DomRenderer(root, immediateScope())
        val controller = FlashcardController(
            summariesOf(testDecks),
            loadDeck = loaderOf(testDecks),
            onChange = renderer::render,
        )
        renderer.bind(controller)
        runSync { controller.resume() } // no store -> Home
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

    private fun HTMLElement.startFirstDeck() {
        click(".deck-item")
        click(".primary")
    }

    @Test
    fun studyShowsPromptThenAnswerOnTap() {
        val (root, _) = mount()
        root.startFirstDeck() // greetings, card 0 = hello/hola
        assertContains(root.textContent!!, "hello")
        assertContains(root.textContent!!, "1 / 2")
        root.click(".card")
        assertContains(root.textContent!!, "hola")
    }

    @Test
    fun prevIsDisabledOnFirstCard() {
        val (root, _) = mount()
        root.startFirstDeck()
        val prev = root.querySelector(".nav-btn") as HTMLElement
        assertTrue(prev.hasAttribute("disabled"))
    }

    @Test
    fun nextAdvancesToSecondCard() {
        val (root, controller) = mount()
        root.startFirstDeck()
        // Next is the second .nav-btn
        (root.querySelectorAll(".nav-btn").item(1) as HTMLElement).click()
        assertEquals(1, controller.state.position)
        assertContains(root.textContent!!, "bye")
    }

    @Test
    fun finishingCompletesTheDeckAndBackGoesHome() {
        val (root, controller) = mount()
        root.startFirstDeck() // 2 cards
        val next = { (root.querySelectorAll(".nav-btn").item(1) as HTMLElement).click() }
        next() // -> card 1
        next() // -> complete
        assertEquals(Screen.Complete, controller.state.screen)
        assertContains(root.textContent!!, "Deck complete")
        root.click(".primary")
        assertEquals(Screen.Home, controller.state.screen)
    }
}
