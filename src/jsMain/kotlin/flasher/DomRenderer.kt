package flasher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.html.FlowContent
import kotlinx.html.a
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
 * screen from scratch. User events are dispatched to the bound [FlashcardController]; deck
 * selection is a suspending (lazy-loading) action, so it's launched on [scope].
 */
class DomRenderer(
    private val root: HTMLElement,
    private val scope: CoroutineScope,
) : Renderer {

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
        // A real link out to the site root, so you can leave the app (it lives under /apps/flasher/).
        div("top-bar") {
            a(href = "/", classes = "link site-link") { +"‹ breischl.dev" }
        }
        h1 { +"Flasher" }
        if (state.summaries.isEmpty()) {
            p("muted") { +"No decks available." }
        } else {
            div("deck-list") {
                state.summaries.forEach { summary ->
                    button(classes = "deck-item") {
                        onClickFunction = { scope.launch { controller.selectDeck(summary.id) } }
                        span("deck-title") { +summary.title }
                        span("deck-count") { +"${summary.cardCount} cards" }
                    }
                }
            }
        }
        // Point would-be contributors at the deck-authoring guide (hosted on GitHub; not bundled).
        a(href = "https://github.com/breischl/flasher/blob/main/CONTRIBUTING-DECKS.md", classes = "link contribute-link") {
            attributes["target"] = "_blank"
            attributes["rel"] = "noopener"
            +"Contribute a deck →"
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

        button(classes = "toggle ${if (state.answerFirst) "toggle-on" else ""}".trim()) {
            onClickFunction = { controller.toggleAnswerFirst() }
            +"Start on: ${if (state.answerFirst) "Answer" else "Prompt"}"
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
            onClickFunction = { controller.flipOrAdvance() }
            span("card-side") { +if (state.isFlipped) "answer" else "prompt" }
            span("card-text") { +if (state.isFlipped) card.back else card.front }
            // On the starting face the gesture reveals; once revealed, the same gesture advances.
            val revealed = state.isFlipped != state.answerFirst
            span("card-hint muted") { +if (revealed) "tap for next" else "tap to flip" }
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
            state.nextDeckSummary?.let { next ->
                button(classes = "primary") {
                    onClickFunction = { scope.launch { controller.startNextDeck() } }
                    +"Next deck: ${next.title} ›"
                }
            }
            button(classes = if (state.nextDeckSummary == null) "primary" else "link") {
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
