package flasher

import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testDecks = listOf(
    Deck("greetings", "Greetings", listOf(Card("hello", "hola"), Card("bye", "adiós"))),
    Deck("colors", "Colors", listOf(Card("red", "rojo"))),
)

private val imageDecks = listOf(
    Deck(
        "animals", "Animals",
        listOf(
            Card("cat", "il gatto", frontImage = "cat.svg"),
            Card("dog", "il cane", backImage = "dog.svg"),
        ),
    ),
)

class DomRendererTest {

    private val mounted = mutableListOf<HTMLElement>()

    @AfterTest
    fun cleanup() {
        mounted.forEach { it.remove() }
        mounted.clear()
    }

    private fun mount(decks: List<Deck> = testDecks): Pair<HTMLElement, FlashcardController> {
        val root = document.createElement("div") as HTMLElement
        document.body!!.appendChild(root)
        mounted += root
        val renderer = DomRenderer(root, immediateScope())
        val controller = FlashcardController(
            summariesOf(decks),
            loadDeck = loaderOf(decks),
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
    fun homeHasALinkOutToTheSiteRoot() {
        val (root, _) = mount()
        val link = root.querySelector(".site-link") as? org.w3c.dom.HTMLAnchorElement
        assertNotNull(link, "expected a site-root link on Home")
        assertEquals("/", link.getAttribute("href"))
    }

    @Test
    fun homeLinksToTheCreditsPage() {
        val (root, _) = mount()
        // Relative href, so it resolves under the /apps/flasher/ subpath (HOME-7).
        assertNotNull(root.querySelector("a[href='credits.html']"), "expected a Credits link on Home")
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

    /**
     * Regression guard for the wiring bug where the renderer was handed the startup coroutine's own
     * scope: once that coroutine completed, deck-selection launches fired into a dead scope and did
     * nothing. Mirrors main() — set everything up inside a startup coroutine on a long-lived
     * app-scope, let it finish, then click a deck and confirm it still loads.
     */
    @Test
    fun deckClickLoadsDeckAfterStartupCoroutineCompletes() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        document.body!!.appendChild(root)
        mounted += root

        val appScope = MainScope()
        lateinit var controller: FlashcardController
        appScope.launch {
            val renderer = DomRenderer(root, scope = appScope)
            controller = FlashcardController(
                summariesOf(testDecks),
                loadDeck = loaderOf(testDecks),
                onChange = renderer::render,
            )
            renderer.bind(controller)
            controller.resume() // no store -> Home
        }.join() // startup coroutine has now completed

        (root.querySelector(".deck-item") as HTMLElement).click()
        delay(50) // let the launched selectDeck coroutine run
        assertEquals(Screen.DeckOptions, controller.state.screen)
        assertContains(root.textContent!!, "Greetings")
    }

    @Test
    fun startSideToggleFlipsLabelAndState() {
        val (root, controller) = mount()
        root.click(".deck-item")
        assertContains(root.textContent!!, "Start on: Prompt")
        // Second toggle button is the "Start on" one (first is Shuffle).
        (root.querySelectorAll(".toggle").item(1) as HTMLElement).click()
        assertTrue(controller.state.answerFirst)
        assertContains(root.textContent!!, "Start on: Answer")
    }

    @Test
    fun answerFirstShowsTheAnswerSideOnTheFirstCard() {
        val (root, _) = mount()
        root.click(".deck-item") // greetings, card 0 = hello/hola
        (root.querySelectorAll(".toggle").item(1) as HTMLElement).click() // Start on: Answer
        root.click(".primary") // Start
        assertContains(root.textContent!!, "hola")
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
    fun tappingRevealsTheAnswerThenAdvancesToTheNextCard() {
        val (root, controller) = mount()
        root.startFirstDeck() // greetings, card 0 = hello/hola
        assertContains(root.textContent!!, "hello")
        assertContains(root.textContent!!, "1 / 2")
        root.click(".card") // reveal
        assertContains(root.textContent!!, "hola")
        assertEquals(0, controller.state.position)
        root.click(".card") // advance — not flip back
        assertEquals(1, controller.state.position)
        assertContains(root.textContent!!, "bye")
        assertContains(root.textContent!!, "2 / 2")
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
        root.startFirstDeck() // greetings, 2 cards (has a following deck, so Back is a link)
        val next = { (root.querySelectorAll(".nav-btn").item(1) as HTMLElement).click() }
        next() // -> card 1
        next() // -> complete
        assertEquals(Screen.Complete, controller.state.screen)
        assertContains(root.textContent!!, "Deck complete")
        root.click(".link") // Back to home
        assertEquals(Screen.Home, controller.state.screen)
    }

    @Test
    fun completeScreenOffersTheNextDeckAndStartsIt() {
        val (root, controller) = mount()
        root.startFirstDeck() // greetings; next deck in order is colors
        val next = { (root.querySelectorAll(".nav-btn").item(1) as HTMLElement).click() }
        next(); next() // -> complete
        assertContains(root.textContent!!, "Next deck: Colors")
        root.click(".primary") // the "Next deck" button
        assertEquals(Screen.Study, controller.state.screen)
        assertEquals("colors", controller.state.currentDeck?.id)
    }

    @Test
    fun lastDeckCompleteHasNoNextDeckButton() {
        val (root, controller) = mount()
        (root.querySelectorAll(".deck-item").item(1) as HTMLElement).click() // colors (last deck)
        root.click(".primary") // Start
        (root.querySelectorAll(".nav-btn").item(1) as HTMLElement).click() // 1 card -> complete
        assertEquals(Screen.Complete, controller.state.screen)
        assertFalse(root.textContent!!.contains("Next deck"))
        assertContains(root.textContent!!, "Back to home")
    }

    private fun HTMLElement.cardImage(): HTMLImageElement? =
        querySelector(".card .card-image") as? HTMLImageElement

    @Test
    fun imageSideShowsTheImageWithItsTextAsAltOnly() {
        val (root, _) = mount(imageDecks)
        root.startFirstDeck() // animals, card 0 front = cat.svg
        val img = root.cardImage()
        assertNotNull(img, "expected an image on the front face")
        assertEquals("decks/animals/cat.svg", img.getAttribute("src"))
        assertEquals("cat", img.getAttribute("alt"))
        assertNull(root.querySelector(".card .card-text"), "image side should not show its text")
        assertFalse(root.querySelector(".card")!!.textContent!!.contains("cat"))
    }

    @Test
    fun textSideOfAnImageCardStillRendersText() {
        val (root, _) = mount(imageDecks)
        root.startFirstDeck()
        root.click(".card") // reveal the back: text "il gatto"
        assertNull(root.cardImage())
        assertEquals("il gatto", root.querySelector(".card .card-text")?.textContent)
    }

    @Test
    fun backImageShowsWhenRevealed() {
        val (root, _) = mount(imageDecks)
        root.startFirstDeck()
        root.click(".card") // reveal card 0
        root.click(".card") // advance to card 1 (dog, back image)
        assertEquals("dog", root.querySelector(".card .card-text")?.textContent)
        root.click(".card") // reveal
        assertEquals("decks/animals/dog.svg", root.cardImage()?.getAttribute("src"))
        assertEquals("il cane", root.cardImage()?.getAttribute("alt"))
    }

    @Test
    fun answerFirstShowsTheBackImageFirst() {
        val (root, controller) = mount(imageDecks)
        root.click(".deck-item")
        (root.querySelectorAll(".toggle").item(1) as HTMLElement).click() // Start on: Answer
        root.click(".primary")
        controller.next() // card 1 = dog, whose back is an image
        assertEquals("decks/animals/dog.svg", root.cardImage()?.getAttribute("src"))
    }
}
