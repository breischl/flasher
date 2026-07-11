package flasher

import kotlinx.serialization.Serializable

/** Renders [AppState] to some surface (DOM, canvas, text…). The one platform-specific seam. */
fun interface Renderer {
    fun render(state: AppState)
}

/** Where the user last was, so we can drop them back into a deck on the next visit. */
@Serializable
data class SavedPosition(val deckId: String, val naturalIndex: Int)

/** Persists the study position across sessions (localStorage in the browser). */
interface SessionStore {
    fun save(position: SavedPosition)
    fun load(): SavedPosition?
    fun clear()
}

/**
 * Loads the bundled decks (from JSON resources in the browser). The index is loaded up front so the
 * home list can render; each deck's cards are fetched lazily, only when that deck is opened.
 */
interface DeckRepository {
    suspend fun loadIndex(): List<DeckSummary>
    suspend fun loadDeck(id: String): Deck
}
