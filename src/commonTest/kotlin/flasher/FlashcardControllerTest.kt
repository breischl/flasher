package flasher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Records what the controller persists, so we can assert on it. */
private class FakeSessionStore(var saved: SavedPosition? = null) : SessionStore {
    override fun save(position: SavedPosition) { saved = position }
    override fun load(): SavedPosition? = saved
    override fun clear() { saved = null }
}

private fun deck(id: String, vararg fronts: String) =
    Deck(id, id.replaceFirstChar { it.uppercase() }, fronts.map { Card(it, "$it-back") })

private val greetings = deck("greetings", "hello", "goodbye", "please")
private val colors = deck("colors", "red", "blue")
private val decks = listOf(greetings, colors)

class FlashcardControllerTest {

    private fun controller(
        store: SessionStore? = null,
        shuffle: (Int) -> List<Int> = { n -> (0 until n).shuffled() },
    ) = FlashcardController(decks, store = store, shuffle = shuffle)

    @Test
    fun startsOnHomeWithAllDecks() {
        val c = controller()
        assertEquals(Screen.Home, c.state.screen)
        assertEquals(decks, c.state.decks)
    }

    @Test
    fun selectDeckOpensOptions() {
        val c = controller()
        c.selectDeck("greetings")
        assertEquals(Screen.DeckOptions, c.state.screen)
        assertEquals(greetings, c.state.currentDeck)
        assertFalse(c.state.shuffleOn)
    }

    @Test
    fun toggleShuffleFlipsTheFlag() {
        val c = controller()
        c.selectDeck("greetings")
        c.toggleShuffle()
        assertTrue(c.state.shuffleOn)
        c.toggleShuffle()
        assertFalse(c.state.shuffleOn)
    }

    @Test
    fun startEntersStudyAtFirstCardInNaturalOrder() {
        val c = controller()
        c.selectDeck("greetings")
        c.start()
        assertEquals(Screen.Study, c.state.screen)
        assertEquals(listOf(0, 1, 2), c.state.order)
        assertEquals(0, c.state.position)
        assertFalse(c.state.isFlipped)
        assertEquals("hello", c.state.currentCard?.front)
    }

    @Test
    fun startWithShuffleUsesTheInjectedPermutation() {
        val c = controller(shuffle = { n -> (n - 1 downTo 0).toList() })
        c.selectDeck("greetings")
        c.toggleShuffle()
        c.start()
        assertEquals(listOf(2, 1, 0), c.state.order)
        assertEquals("please", c.state.currentCard?.front)
    }

    @Test
    fun flipTogglesTheCard() {
        val c = controller()
        c.selectDeck("greetings")
        c.start()
        c.flip()
        assertTrue(c.state.isFlipped)
        c.flip()
        assertFalse(c.state.isFlipped)
    }

    @Test
    fun nextAdvancesAndResetsFlip() {
        val c = controller()
        c.selectDeck("greetings")
        c.start()
        c.flip()
        c.next()
        assertEquals(1, c.state.position)
        assertEquals("goodbye", c.state.currentCard?.front)
        assertFalse(c.state.isFlipped)
    }

    @Test
    fun prevGoesBackAndClampsAtFirstCard() {
        val c = controller()
        c.selectDeck("greetings")
        c.start()
        c.next()
        c.flip()
        c.prev()
        assertEquals(0, c.state.position)
        assertFalse(c.state.isFlipped)
        c.prev() // already at first — no-op
        assertEquals(0, c.state.position)
        assertEquals(Screen.Study, c.state.screen)
    }

    @Test
    fun advancingPastLastCardCompletesTheDeck() {
        val c = controller()
        c.selectDeck("colors") // 2 cards
        c.start()
        c.next()
        assertEquals(1, c.state.position)
        c.next()
        assertEquals(Screen.Complete, c.state.screen)
    }

    @Test
    fun navigationSavesNaturalIndexEvenWhenShuffled() {
        val store = FakeSessionStore()
        val c = controller(store = store, shuffle = { n -> (n - 1 downTo 0).toList() })
        c.selectDeck("greetings")
        c.toggleShuffle()
        c.start()
        // order = [2,1,0]; position 0 -> natural index 2
        assertEquals(SavedPosition("greetings", 2), store.saved)
        c.next() // position 1 -> natural index 1
        assertEquals(SavedPosition("greetings", 1), store.saved)
    }

    @Test
    fun completingTheDeckClearsSavedPosition() {
        val store = FakeSessionStore()
        val c = controller(store = store)
        c.selectDeck("colors")
        c.start()
        c.next()
        c.next() // completes
        assertNull(store.saved)
    }

    @Test
    fun goHomeReturnsToDeckListAndClearsSavedPosition() {
        val store = FakeSessionStore(SavedPosition("greetings", 1))
        val c = controller(store = store)
        c.selectDeck("greetings")
        c.start()
        c.goHome()
        assertEquals(Screen.Home, c.state.screen)
        assertNull(c.state.currentDeck)
        assertNull(store.saved)
    }

    @Test
    fun resumeJumpsIntoStudyAtSavedNaturalIndexUnshuffled() {
        val store = FakeSessionStore(SavedPosition("greetings", 2))
        val c = controller(store = store)
        c.resume()
        assertEquals(Screen.Study, c.state.screen)
        assertEquals(greetings, c.state.currentDeck)
        assertEquals(listOf(0, 1, 2), c.state.order)
        assertEquals(2, c.state.position)
        assertEquals("please", c.state.currentCard?.front)
        assertFalse(c.state.isFlipped)
    }

    @Test
    fun resumeWithNoSavedPositionShowsHome() {
        val c = controller(store = FakeSessionStore(null))
        c.resume()
        assertEquals(Screen.Home, c.state.screen)
    }

    @Test
    fun resumeWithUnknownDeckShowsHome() {
        val store = FakeSessionStore(SavedPosition("does-not-exist", 0))
        val c = controller(store = store)
        c.resume()
        assertEquals(Screen.Home, c.state.screen)
    }

    @Test
    fun resumeWithStaleIndexShowsHome() {
        val store = FakeSessionStore(SavedPosition("colors", 99))
        val c = controller(store = store)
        c.resume()
        assertEquals(Screen.Home, c.state.screen)
    }
}
