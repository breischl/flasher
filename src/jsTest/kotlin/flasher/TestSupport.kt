package flasher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The deck index (summaries) derived from a set of full decks, for tests. */
fun summariesOf(decks: List<Deck>): List<DeckSummary> =
    decks.map { DeckSummary(it.id, it.title, it.cards.size) }

/** An immediate lazy loader over in-memory decks — never actually suspends. */
fun loaderOf(decks: List<Deck>): suspend (String) -> Deck =
    { id -> decks.first { it.id == id } }

/**
 * A scope whose `launch`es run eagerly to completion, provided the coroutine body never truly
 * suspends. Since [loaderOf] resolves synchronously, deck selection/resume complete before the
 * next line of a test runs — keeping the DOM tests straightforwardly synchronous.
 */
fun immediateScope(): CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

/** Run a suspend action synchronously (bodies backed by [loaderOf] never actually suspend). */
fun runSync(block: suspend () -> Unit) {
    immediateScope().launch { block() }
}
