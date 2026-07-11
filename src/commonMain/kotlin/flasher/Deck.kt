package flasher

import kotlinx.serialization.Serializable

/** A single flashcard: a prompt side and its answer side. */
@Serializable
data class Card(
    val front: String,
    val back: String,
)

/**
 * A named collection of cards the user studies as a unit. [id] is the deck's stable identity
 * (its filename stem for bundled decks); it is not stored inside the deck's own JSON. Not
 * `@Serializable` on purpose — decks are decoded via [flasher.JsonDeckRepository]'s file DTO,
 * which supplies [id] from the filename.
 */
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
