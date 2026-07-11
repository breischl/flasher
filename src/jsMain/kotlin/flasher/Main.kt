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
        val renderer = Renderer { state -> renderStub(root, state) }
        val controller = FlashcardController(decks, store = store, onChange = renderer::render)

        // Expose for manual driving from the browser console during early development.
        window.asDynamic().flasher = controller

        controller.resume()
    }
}

/** Temporary text-only renderer (replaced by DomRenderer in Step 4). */
private fun renderStub(root: HTMLElement, state: AppState) {
    val lines = buildString {
        appendLine("screen: ${state.screen}")
        appendLine("decks: ${state.decks.map { it.id }}")
        appendLine("currentDeck: ${state.currentDeck?.id}")
        appendLine("shuffleOn: ${state.shuffleOn}")
        appendLine("position: ${state.position} / ${state.total}")
        appendLine("isFlipped: ${state.isFlipped}")
        appendLine("card: ${state.currentCard?.front} | ${state.currentCard?.back}")
        appendLine()
        appendLine("console: flasher.selectDeck('greetings'); flasher.start(); flasher.flip(); flasher.next()")
    }
    root.textContent = ""
    val pre = document.createElement("pre") as HTMLElement
    pre.textContent = lines
    root.appendChild(pre)
}
