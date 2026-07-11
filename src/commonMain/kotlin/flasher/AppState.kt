package flasher

/** Which screen the app is currently showing. */
enum class Screen { Home, DeckOptions, Study, Complete }

/**
 * The complete UI state, rendered as a pure function by any [Renderer].
 *
 * [order] holds card indices into [currentDeck]'s `cards` list in play order (natural or
 * shuffled); [position] is the cursor into [order]. Keeping the ordering separate from the deck
 * lets shuffle be a view of the same cards without mutating them.
 */
data class AppState(
    val screen: Screen,
    val summaries: List<DeckSummary>,
    val currentDeck: Deck? = null,
    val order: List<Int> = emptyList(),
    val position: Int = 0,
    val isFlipped: Boolean = false,
    val shuffleOn: Boolean = false,
    val answerFirst: Boolean = false,
) {
    /** Number of cards in the deck being studied. */
    val total: Int get() = order.size

    /** The natural-order index of the current card, or null when not studying. */
    val currentNaturalIndex: Int? get() = order.getOrNull(position)

    /** The summary of the deck that follows the current one in index order, or null if it's last. */
    val nextDeckSummary: DeckSummary?
        get() {
            val id = currentDeck?.id ?: return null
            val pos = summaries.indexOfFirst { it.id == id }
            return if (pos < 0) null else summaries.getOrNull(pos + 1)
        }

    /** The card currently on screen, or null when not studying. */
    val currentCard: Card?
        get() {
            val deck = currentDeck ?: return null
            val naturalIndex = currentNaturalIndex ?: return null
            return deck.cards.getOrNull(naturalIndex)
        }

    companion object {
        /** Initial state before any deck is chosen. */
        fun home(summaries: List<DeckSummary>) = AppState(screen = Screen.Home, summaries = summaries)
    }
}
