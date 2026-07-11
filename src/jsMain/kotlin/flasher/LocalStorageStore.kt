package flasher

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

/** Persists the study position in the browser's localStorage as a small JSON blob. */
class LocalStorageStore(
    private val key: String = "flasher.position",
) : SessionStore {

    private val json = Json

    override fun save(position: SavedPosition) {
        localStorage[key] = json.encodeToString(position)
    }

    override fun load(): SavedPosition? {
        val raw = localStorage[key] ?: return null
        return runCatching { json.decodeFromString<SavedPosition>(raw) }.getOrNull()
    }

    override fun clear() {
        localStorage.removeItem(key)
    }
}
