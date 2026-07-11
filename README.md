# Flasher

A lightweight, mobile-first flashcard webapp for studying (the bundled decks teach Spanish, but
the model is language-agnostic). Pick a deck, optionally shuffle, then tap a card to flip between
prompt and answer and step through the deck. It's a **static site** — no backend — written in
Kotlin Multiplatform and compiled to JavaScript.

## Features

- Deck list → per-deck options (shuffle) → study → deck-complete flow
- Tap to flip; Prev/Next buttons, swipe gestures, and arrow keys
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

- `index.json` — an array of deck ids, in display order
- `<id>.json` — `{ "id", "title", "cards": [ { "front", "back" }, … ] }`

Add a file and list its id in `index.json`; no code changes or recompile of logic required.

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
