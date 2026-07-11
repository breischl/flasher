package flasher

import kotlinx.coroutines.test.runTest
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
private val allDecks = listOf(greetings, colors)
private val index = allDecks.map { DeckSummary(it.id, it.title, it.cards.size) }

/** A lazy deck loader that records which decks were fetched, backed by the in-memory fixtures. */
private class RecordingLoader(private val decks: List<Deck> = allDecks) {
    val loaded = mutableListOf<String>()
    val load: suspend (String) -> Deck = { id ->
        loaded += id
        decks.first { it.id == id }
    }
}

class FlashcardControllerTest {

    private fun controller(
        store: SessionStore? = null,
        shuffle: (Int) -> List<Int> = { n -> (0 until n).shuffled() },
        loadDeck: suspend (String) -> Deck = RecordingLoader().load,
    ) = FlashcardController(index, loadDeck = loadDeck, store = store, shuffle = shuffle)

    @Test
    fun startsOnHomeWithEveryDeckSummary() = runTest {
        val c = controller()
        assertEquals(Screen.Home, c.state.screen)
        assertEquals(index, c.state.summaries)
    }

    @Test
    fun decksAreNotLoadedUntilSelected() = runTest {
        val loader = RecordingLoader()
        val c = controller(loadDeck = loader.load)
        assertTrue(loader.loaded.isEmpty(), "no deck should be fetched just to show Home")
        c.selectDeck("greetings")
        assertEquals(listOf("greetings"), loader.loaded)
    }

    @Test
    fun selectDeckLoadsItAndOpensOptions() = runTest {
        val c = controller()
        c.selectDeck("greetings")
        assertEquals(Screen.DeckOptions, c.state.screen)
        assertEquals(greetings, c.state.currentDeck)
        assertFalse(c.state.shuffleOn)
    }

    @Test
    fun selectUnknownDeckIsANoOp() = runTest {
        val c = controller()
        c.selectDeck("does-not-exist")
        assertEquals(Screen.Home, c.state.screen)
        assertNull(c.state.currentDeck)
    }

    @Test
    fun toggleShuffleFlipsTheFlag() = runTest {
        val c = controller()
        c.selectDeck("greetings")
        c.toggleShuffle()
        assertTrue(c.state.shuffleOn)
        c.toggleShuffle()
        assertFalse(c.state.shuffleOn)
    }

    @Test
    fun startEntersStudyAtFirstCardInNaturalOrder() = runTest {
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
    fun startWithShuffleUsesTheInjectedPermutation() = runTest {
        val c = controller(shuffle = { n -> (n - 1 downTo 0).toList() })
        c.selectDeck("greetings")
        c.toggleShuffle()
        c.start()
        assertEquals(listOf(2, 1, 0), c.state.order)
        assertEquals("please", c.state.currentCard?.front)
    }

    @Test
    fun toggleAnswerFirstFlipsTheFlagOnOptionsOnly() = runTest {
        val c = controller()
        c.toggleAnswerFirst() // on Home — no-op
        assertFalse(c.state.answerFirst)
        c.selectDeck("greetings")
        c.toggleAnswerFirst()
        assertTrue(c.state.answerFirst)
        c.toggleAnswerFirst()
        assertFalse(c.state.answerFirst)
    }

    @Test
    fun selectDeckResetsAnswerFirst() = runTest {
        val c = controller()
        c.selectDeck("greetings")
        c.toggleAnswerFirst()
        assertTrue(c.state.answerFirst)
        c.goHome()
        c.selectDeck("colors")
        assertFalse(c.state.answerFirst)
    }

    @Test
    fun startWithAnswerFirstBeginsOnTheAnswerSide() = runTest {
        val c = controller()
        c.selectDeck("greetings")
        c.toggleAnswerFirst()
        c.start()
        assertTrue(c.state.isFlipped)
    }

    @Test
    fun nextAndPrevResetToTheChosenStartingSide() = runTest {
        val c = controller()
        c.selectDeck("greetings")
        c.toggleAnswerFirst()
        c.start()
        c.flip() // now showing prompt
        assertFalse(c.state.isFlipped)
        c.next() // resets to answer-first
        assertTrue(c.state.isFlipped)
        c.flip()
        c.prev()
        assertTrue(c.state.isFlipped)
    }

    @Test
    fun flipTogglesTheCard() = runTest {
        val c = controller()
        c.selectDeck("greetings")
        c.start()
        c.flip()
        assertTrue(c.state.isFlipped)
        c.flip()
        assertFalse(c.state.isFlipped)
    }

    @Test
    fun nextAdvancesAndResetsFlip() = runTest {
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
    fun prevGoesBackAndClampsAtFirstCard() = runTest {
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
    fun advancingPastLastCardCompletesTheDeck() = runTest {
        val c = controller()
        c.selectDeck("colors") // 2 cards
        c.start()
        c.next()
        assertEquals(1, c.state.position)
        c.next()
        assertEquals(Screen.Complete, c.state.screen)
    }

    @Test
    fun navigationSavesNaturalIndexEvenWhenShuffled() = runTest {
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
    fun completingTheDeckClearsSavedPosition() = runTest {
        val store = FakeSessionStore()
        val c = controller(store = store)
        c.selectDeck("colors")
        c.start()
        c.next()
        c.next() // completes
        assertNull(store.saved)
    }

    @Test
    fun goHomeReturnsToDeckListAndClearsSavedPosition() = runTest {
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
    fun resumeJumpsIntoStudyAtSavedNaturalIndexUnshuffled() = runTest {
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
    fun resumeWithNoSavedPositionShowsHome() = runTest {
        val c = controller(store = FakeSessionStore(null))
        c.resume()
        assertEquals(Screen.Home, c.state.screen)
    }

    @Test
    fun resumeWithUnknownDeckShowsHome() = runTest {
        val store = FakeSessionStore(SavedPosition("does-not-exist", 0))
        val c = controller(store = store)
        c.resume()
        assertEquals(Screen.Home, c.state.screen)
    }

    @Test
    fun resumeWithStaleIndexShowsHome() = runTest {
        val store = FakeSessionStore(SavedPosition("colors", 99))
        val c = controller(store = store)
        c.resume()
        assertEquals(Screen.Home, c.state.screen)
    }
}
