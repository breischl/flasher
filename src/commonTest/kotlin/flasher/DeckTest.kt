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
                Card(front = "hello", back = "hola"),
                Card(front = "goodbye", back = "adiós"),
            ),
        )

        assertEquals("greetings", deck.id)
        assertEquals(2, deck.cards.size)
        assertEquals("hola", deck.cards.first().back)
    }
}
