package flasher

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement

fun main() {
    val root = document.getElementById("app") as? HTMLElement ?: return

    MainScope().launch {
        val repo = JsonDeckRepository()
        val index = runCatching { repo.loadIndex() }
            .getOrElse { error ->
                console.error("Failed to load deck index", error)
                emptyList()
            }

        val store = LocalStorageStore()
        val renderer = DomRenderer(root, scope = this)
        val controller = FlashcardController(
            index,
            loadDeck = repo::loadDeck,
            store = store,
            onChange = renderer::render,
        )
        renderer.bind(controller)
        InputHandler(controller).attach(window)

        // Expose for manual driving from the browser console during early development.
        window.asDynamic().flasher = controller

        controller.resume()
    }
}
