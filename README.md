# Flasher

A lightweight, mobile-first flashcard webapp for studying (the bundled decks teach Italian for
travel, graded from basics to full sentences, but the model is language-agnostic). Pick a deck,
optionally shuffle, then tap a card to flip between prompt and answer and step through the deck. It's a **static site** — no backend — written in
Kotlin Multiplatform and compiled to JavaScript.

## Features

- Deck list → per-deck options (shuffle, and which side shows first) → study → deck-complete flow
- One-gesture walk: tap/click (or Space/Enter) reveals the card, then the same gesture advances —
  step through a whole deck without switching gestures. Prev/Next buttons, swipe, and arrow keys too
- Remembers where you left off (localStorage) and drops you back into that card
- No images; pure CSS, adapts to light/dark, sized for phones

## Requirements

- JDK (a Java 22 toolchain is pinned via `.java-version` for jenv users)
- No global Gradle needed — use the wrapper (`./gradlew`)

## Develop

```sh
./gradlew jsBrowserDevelopmentRun --continuous   # serve with hot reload at http://localhost:8080
```

## Test

Domain logic and the browser UI are covered by unit + Karma/Chrome browser tests:

```sh
./gradlew allTests
```

## Build a deployable static site

```sh
./gradlew jsBrowserDistribution
```

The self-contained bundle is written to `build/dist/js/productionExecutable/`
(`index.html`, `flasher.js`, `styles.css`, `decks/`). Serve that directory from any static host
(GitHub Pages, Netlify, S3, …). To try it locally:

```sh
cd build/dist/js/productionExecutable && python3 -m http.server 8000
```

## Publishing

The app is deployed to **https://breischl.dev/apps/flasher/** by the
[`Publish`](.github/workflows/publish.yml) GitHub Actions workflow, which runs on every push to
`main` (and can be run manually via *Run workflow*). It builds the production distribution and
copies it into the [`breischl/breischl.github.io`](https://github.com/breischl/breischl.github.io)
Hugo site at `static/apps/flasher/`, then commits and pushes — which triggers that site's own
deploy to GitHub Pages.

The app ships as plain static files served verbatim (no Hugo theme wrapping); its relative asset
and `decks/` paths let it run from that subpath unchanged.

**Required secret:** the workflow needs the repository secret `BREISCHL_DEV_REPO_ACCESS_TOKEN` — a
GitHub token with `contents: write` on the `breischl.github.io` repo (the same PAT the keneth site
integration uses).

## Adding or editing decks

Decks are plain JSON under `src/jsMain/resources/decks/`:

- `<id>.json` — `{ "id", "title", "order", "cards": [ { "front", "back" }, … ] }` (the filename
  stem must equal `id`). `order` is an integer that controls the deck's position in the list.

Just add a file and rebuild — **`decks/index.json` is generated at build time** (by the
`generateDeckIndex` Gradle task) from the deck files, so there's no index to hand-maintain. No code
changes or recompile of logic required. Deck files are validated by the `validateDecks` Gradle task
(wired into `check` and the build), so a malformed deck fails the build rather than shipping broken.

**Contributing a deck?** See [`CONTRIBUTING-DECKS.md`](CONTRIBUTING-DECKS.md) for the format, rules,
and how to submit — the machine-readable contract is [`docs/deck.schema.json`](docs/deck.schema.json).

Decks load **lazily**: only the generated index (deck titles + card counts) is fetched at startup;
a deck's cards are fetched the first time you open it, then cached.

## Architecture

The domain is pure Kotlin in `commonMain` and knows nothing about the browser, so the UI can be
swapped without touching the logic:

- `Deck` / `Card` / `AppState` — data model (`commonMain`)
- `FlashcardController` — the state machine for every action (`commonMain`, fully unit-tested)
- `Renderer` / `SessionStore` / `DeckRepository` — the seams to the outside world (`commonMain`)
- `DomRenderer`, `InputHandler`, `LocalStorageStore`, `JsonDeckRepository` — browser
  implementations (`jsMain`)

`main()` loads decks, builds the controller, and connects it to the DOM renderer, input handler,
and localStorage.
