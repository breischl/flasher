package flasher

import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.js.onClickFunction
import kotlinx.html.p
import kotlinx.html.span
import org.w3c.dom.HTMLElement

/**
 * Renders [AppState] to the DOM with kotlinx.html. Stateless: every [render] rebuilds the
 * screen from scratch. User events are dispatched to the bound [FlashcardController].
 */
class DomRenderer(private val root: HTMLElement) : Renderer {

    private lateinit var controller: FlashcardController

    /** Wire up the controller whose actions this renderer invokes. */
    fun bind(controller: FlashcardController) {
        this.controller = controller
    }

    override fun render(state: AppState) {
        root.innerHTML = ""
        root.append {
            div("screen") {
                when (state.screen) {
                    Screen.Home -> homeScreen(state)
                    Screen.DeckOptions -> optionsScreen(state)
                    Screen.Study -> studyScreen(state)
                    Screen.Complete -> completeScreen(state)
                }
            }
        }
    }

    private fun FlowContent.homeScreen(state: AppState) {
        h1 { +"Flasher" }
        if (state.decks.isEmpty()) {
            p("muted") { +"No decks available." }
            return
        }
        div("deck-list") {
            state.decks.forEach { deck ->
                button(classes = "deck-item") {
                    onClickFunction = { controller.selectDeck(deck.id) }
                    span("deck-title") { +deck.title }
                    span("deck-count") { +"${deck.cards.size} cards" }
                }
            }
        }
    }

    private fun FlowContent.optionsScreen(state: AppState) {
        val deck = state.currentDeck ?: return
        topBar(backLabel = "‹ Decks")
        h1 { +deck.title }
        p("muted") { +"${deck.cards.size} cards" }

        button(classes = "toggle ${if (state.shuffleOn) "toggle-on" else ""}".trim()) {
            onClickFunction = { controller.toggleShuffle() }
            +"Shuffle: ${if (state.shuffleOn) "On" else "Off"}"
        }

        button(classes = "primary") {
            onClickFunction = { controller.start() }
            +"Start"
        }
    }

    private fun FlowContent.studyScreen(state: AppState) {
        val card = state.currentCard ?: return
        div("study-bar") {
            button(classes = "link") {
                onClickFunction = { controller.goHome() }
                +"‹ Home"
            }
            span("progress") { +"${state.position + 1} / ${state.total}" }
        }

        div("card ${if (state.isFlipped) "is-back" else "is-front"}") {
            onClickFunction = { controller.flip() }
            span("card-side") { +if (state.isFlipped) "answer" else "prompt" }
            span("card-text") { +if (state.isFlipped) card.back else card.front }
            span("card-hint muted") { +"tap to flip" }
        }

        div("nav") {
            button(classes = "nav-btn") {
                if (state.position == 0) attributes["disabled"] = "true"
                onClickFunction = { controller.prev() }
                +"‹ Prev"
            }
            button(classes = "nav-btn") {
                onClickFunction = { controller.next() }
                +if (state.position == state.total - 1) "Finish ›" else "Next ›"
            }
        }
    }

    private fun FlowContent.completeScreen(state: AppState) {
        div("complete") {
            h2 { +"Deck complete" }
            p("muted") { +"You went through all ${state.total} cards in ${state.currentDeck?.title}." }
            button(classes = "primary") {
                onClickFunction = { controller.goHome() }
                +"Back to home"
            }
        }
    }

    private fun FlowContent.topBar(backLabel: String) {
        div("top-bar") {
            button(classes = "link") {
                onClickFunction = { controller.goHome() }
                +backLabel
            }
        }
    }
}
