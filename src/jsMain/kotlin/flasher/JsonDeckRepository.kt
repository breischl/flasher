package flasher

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.browser.window

/**
 * Loads bundled decks from static JSON under `decks/`: an `index.json` array of deck ids,
 * then one `<id>.json` per deck. Runs entirely client-side via `fetch`.
 */
class JsonDeckRepository(
    private val basePath: String = "decks",
) : DeckRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadDecks(): List<Deck> {
        val ids = json.decodeFromString<List<String>>(fetchText("$basePath/index.json"))
        return ids.map { id -> json.decodeFromString<Deck>(fetchText("$basePath/$id.json")) }
    }

    private suspend fun fetchText(path: String): String {
        val response = window.fetch(path).await()
        if (!response.ok) error("Failed to load $path (HTTP ${response.status})")
        return response.text().await()
    }
}
