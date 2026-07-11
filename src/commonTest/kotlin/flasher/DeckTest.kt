package flasher

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
