package flasher

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement

fun main() {
    val root = document.getElementById("app") as? HTMLElement ?: return

    registerServiceWorker()

    // A long-lived scope for the whole app session: it must outlive this startup coroutine so that
    // deck selection (launched from click handlers) still runs after main() has returned.
    val appScope = MainScope()
    appScope.launch {
        val repo = JsonDeckRepository()
        val index = runCatching { repo.loadIndex() }
            .getOrElse { error ->
                console.error("Failed to load deck index", error)
                emptyList()
            }

        val store = LocalStorageStore()
        val renderer = DomRenderer(root, scope = appScope)
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

/**
 * Registers the offline service worker (`sw.js`), but only in production builds. During
 * `jsBrowserDevelopmentRun` (including LAN testing from a phone) a cache-first SW would fight
 * hot-reload and serve stale assets, so we skip it there. Webpack replaces `process.env.NODE_ENV`
 * with a string literal at build time ("production" for the dist, "development" for the dev run).
 */
private fun registerServiceWorker() {
    // Webpack's DefinePlugin replaces `process.env.NODE_ENV` with a string literal at build time
    // ("production" for the dist, "development" for the dev run), so no `process` object exists at
    // runtime — do NOT guard with `typeof process`, or the expression evaluates to false and the
    // SW never registers.
    val nodeEnv = js("process.env.NODE_ENV")
    if (nodeEnv != "production") return
    if (window.navigator.asDynamic().serviceWorker == null) return
    window.navigator.asDynamic().serviceWorker.register("sw.js")
}
