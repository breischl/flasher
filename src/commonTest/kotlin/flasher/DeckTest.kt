package flasher

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeckTest {
    @Test
    fun buildsADeck() {
        val deck = Deck(
            id = "greetings",
            title = "Greetings",
            cards = listOf(
                Card(front = "hello", back = "ciao"),
                Card(front = "goodbye", back = "arrivederci"),
            ),
        )

        assertEquals("greetings", deck.id)
        assertEquals(2, deck.cards.size)
        assertEquals("ciao", deck.cards.first().back)
    }

    @Test
    fun cardImagesDefaultToNull() {
        val card = Json.decodeFromString<Card>("""{"front":"hello","back":"ciao"}""")
        assertNull(card.frontImage)
        assertNull(card.backImage)
    }

    @Test
    fun cardDecodesImageFields() {
        val card = Json.decodeFromString<Card>(
            """{"front":"cat","frontImage":"cat.svg","back":"il gatto","backImage":"gatto.webp"}"""
        )
        assertEquals("cat.svg", card.frontImage)
        assertEquals("gatto.webp", card.backImage)
    }
}
