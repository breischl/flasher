package flasher

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement

fun main() {
    val root = document.getElementById("app") as? HTMLElement ?: return

    MainScope().launch {
        val decks = runCatching { JsonDeckRepository().loadDecks() }
            .getOrElse { error ->
                console.error("Failed to load decks", error)
                emptyList()
            }

        val store = LocalStorageStore()
        val renderer = DomRenderer(root)
        val controller = FlashcardController(decks, store = store, onChange = renderer::render)
        renderer.bind(controller)

        // Expose for manual driving from the browser console during early development.
        window.asDynamic().flasher = controller

        controller.resume()
    }
}
