package flasher

/**
 * Holds the [AppState] and applies user actions to it, notifying a listener on every change.
 * Pure Kotlin and fully testable: rendering, storage and shuffling are injected.
 *
 * @param shuffle produces a play order (a permutation of `0 until n`) for a deck of size `n`.
 */
class FlashcardController(
    private val initialDecks: List<Deck>,
    private val store: SessionStore? = null,
    private val shuffle: (Int) -> List<Int> = { n -> (0 until n).shuffled() },
    private val onChange: (AppState) -> Unit = {},
) {
    var state: AppState = AppState.home(initialDecks)
        private set

    private fun update(newState: AppState) {
        state = newState
        onChange(newState)
    }

    /** Open a deck's options screen from Home. */
    fun selectDeck(deckId: String) {
        val deck = initialDecks.firstOrNull { it.id == deckId } ?: return
        update(state.copy(screen = Screen.DeckOptions, currentDeck = deck, shuffleOn = false))
    }

    /** Toggle shuffle on the options screen. */
    fun toggleShuffle() {
        if (state.screen != Screen.DeckOptions) return
        update(state.copy(shuffleOn = !state.shuffleOn))
    }

    /** Begin studying the selected deck. */
    fun start() {
        val deck = state.currentDeck ?: return
        if (state.screen != Screen.DeckOptions) return
        val size = deck.cards.size
        val order = if (state.shuffleOn) shuffle(size) else (0 until size).toList()
        if (order.isEmpty()) {
            update(state.copy(screen = Screen.Complete, order = order, position = 0, isFlipped = false))
            store?.clear()
            return
        }
        update(state.copy(screen = Screen.Study, order = order, position = 0, isFlipped = false))
        saveCurrent()
    }

    /** Flip the current card between prompt and answer. */
    fun flip() {
        if (state.screen != Screen.Study) return
        update(state.copy(isFlipped = !state.isFlipped))
    }

    /** Advance to the next card, or finish the deck. */
    fun next() {
        if (state.screen != Screen.Study) return
        if (state.position >= state.total - 1) {
            update(state.copy(screen = Screen.Complete, isFlipped = false))
            store?.clear()
            return
        }
        update(state.copy(position = state.position + 1, isFlipped = false))
        saveCurrent()
    }

    /** Go back to the previous card. */
    fun prev() {
        if (state.screen != Screen.Study) return
        if (state.position == 0) return
        update(state.copy(position = state.position - 1, isFlipped = false))
        saveCurrent()
    }

    /** Return to the deck list, abandoning the current session. */
    fun goHome() {
        store?.clear()
        update(AppState.home(initialDecks))
    }

    /** Restore the last saved position, if any, otherwise show Home. */
    fun resume() {
        val saved = store?.load() ?: return showHome()
        val deck = initialDecks.firstOrNull { it.id == saved.deckId } ?: return showHome()
        if (saved.naturalIndex !in deck.cards.indices) return showHome()
        update(
            state.copy(
                screen = Screen.Study,
                currentDeck = deck,
                order = deck.cards.indices.toList(),
                position = saved.naturalIndex,
                isFlipped = false,
                shuffleOn = false,
            ),
        )
    }

    private fun showHome() = update(AppState.home(initialDecks))

    private fun saveCurrent() {
        val deck = state.currentDeck ?: return
        val naturalIndex = state.currentNaturalIndex ?: return
        store?.save(SavedPosition(deck.id, naturalIndex))
    }
}
