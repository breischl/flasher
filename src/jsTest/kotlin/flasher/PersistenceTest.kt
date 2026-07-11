package flasher

import kotlinx.browser.localStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val persistDecks = listOf(
    Deck("verbs", "Verbs", listOf(Card("go", "ir"), Card("eat", "comer"), Card("see", "ver"))),
)

class PersistenceTest {

    private val key = "flasher.test.position"

    @BeforeTest
    fun clearBefore() = localStorage.removeItem(key)

    @AfterTest
    fun clearAfter() = localStorage.removeItem(key)

    @Test
    fun localStorageStoreRoundTrips() {
        val store = LocalStorageStore(key)
        assertNull(store.load())
        store.save(SavedPosition("verbs", 2))
        assertEquals(SavedPosition("verbs", 2), store.load())
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun reloadResumesAtTheSameCard() {
        val store = LocalStorageStore(key)

        // First "session": study to the third card.
        val first = FlashcardController(persistDecks, store = store)
        first.selectDeck("verbs")
        first.start()
        first.next()
        first.next() // position 2

        // Second "session" (page reload): a fresh controller resumes from the store.
        val second = FlashcardController(persistDecks, store = store)
        second.resume()
        assertEquals(Screen.Study, second.state.screen)
        assertEquals("see", second.state.currentCard?.front)
        assertEquals(2, second.state.position)
    }

    @Test
    fun goingHomeThenReloadingStartsAtHome() {
        val store = LocalStorageStore(key)
        val first = FlashcardController(persistDecks, store = store)
        first.selectDeck("verbs")
        first.start()
        first.next()
        first.goHome() // clears saved position

        val second = FlashcardController(persistDecks, store = store)
        second.resume()
        assertEquals(Screen.Home, second.state.screen)
    }
}
