package flasher

import kotlinx.serialization.Serializable

/** A single flashcard: a prompt side and its answer side. */
@Serializable
data class Card(
    val front: String,
    val back: String,
)

/** A named collection of cards the user studies as a unit. */
@Serializable
data class Deck(
    val id: String,
    val title: String,
    val cards: List<Card>,
)

/**
 * Lightweight deck metadata for the home list — enough to render a deck without loading its cards.
 * Produced by the build-time index generator and served as `decks/index.json`.
 */
@Serializable
data class DeckSummary(
    val id: String,
    val title: String,
    val cardCount: Int,
)
