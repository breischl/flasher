package flasher

import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.browser.window

/**
 * Relative URL base for bundled deck content: `<base>/<id>.json` is a deck, and `<base>/<id>/<file>`
 * is one of that deck's card images.
 */
const val DECKS_BASE_PATH = "decks"

/**
 * Loads bundled decks from static JSON under `decks/`: a build-generated `index.json` of
 * [DeckSummary] objects, then one `<id>.json` per deck fetched lazily on demand. Loaded decks are
 * memoized so re-opening a deck doesn't re-fetch. Runs entirely client-side via `fetch`.
 */
class JsonDeckRepository(
    private val basePath: String = DECKS_BASE_PATH,
) : DeckRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, Deck>()

    override suspend fun loadIndex(): List<DeckSummary> =
        json.decodeFromString<List<DeckSummary>>(fetchText("$basePath/index.json"))

    override suspend fun loadDeck(id: String): Deck = cache.getOrPut(id) {
        val file = json.decodeFromString<DeckFile>(fetchText("$basePath/$id.json"))
        Deck(id = id, title = file.title, cards = file.cards)
    }

    private suspend fun fetchText(path: String): String {
        val response = window.fetch(path).await()
        if (!response.ok) error("Failed to load $path (HTTP ${response.status})")
        return response.text().await()
    }

    /** On-disk shape of a deck file. `id` is not stored — it's the filename stem (the fetch key). */
    @Serializable
    private data class DeckFile(val title: String, val cards: List<Card>)
}
