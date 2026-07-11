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

/** Loads the bundled decks (from JSON resources in the browser). */
interface DeckRepository {
    suspend fun loadDecks(): List<Deck>
}
