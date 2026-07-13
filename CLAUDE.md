# Flasher — notes for Claude

A mobile-first flashcard webapp: Kotlin Multiplatform compiled to JS, deployed as a static site.
Start with `README.md` for the user-facing overview. This file captures the non-obvious things.

## Toolchain quirks (important)

- **Java 22 is pinned** via `.java-version` (jenv). Do NOT run the build on Java 25 — Gradle can't
  run on it here. Java 22 is what CI (`.github/workflows/publish.yml`) uses too.
- **Gradle 9.6.1**, **Kotlin 2.4.0** (latest stable at last check). Runtime libs:
  kotlinx-serialization/coroutines 1.11.0, kotlinx-html 0.12.0.
- Gradle prints a "deprecated features / incompatible with Gradle 10" notice — it comes from the
  Kotlin plugin, not our `build.gradle.kts`. Not ours to fix.

## Key commands

- Tests (unit + Karma/Chrome browser tests): `./gradlew allTests`  (or `check`)
- Dev server: `./gradlew jsBrowserDevelopmentRun --continuous`, then open http://localhost:8080
- Production static bundle: `./gradlew jsBrowserDistribution` → `build/dist/js/productionExecutable/`
- LAN access (e.g. phone) is enabled via `webpack.config.d/devServer.js` (binds `0.0.0.0`,
  `allowedHosts: all`). The typed DevServer DSL has no `host` param in this Kotlin version — use
  the webpack.config.d override, not build.gradle.kts.

## Architecture

Domain is pure Kotlin in `commonMain` and knows nothing about the browser, so the UI can be
swapped without touching logic (design goal: "graduate" to a richer renderer later).

- `commonMain`: `Deck`/`Card`/`DeckSummary`/`AppState`, `FlashcardController` (the state machine,
  unit-tested), and the `Renderer` / `SessionStore` / `DeckRepository` seams.
- `jsMain`: `DomRenderer` (kotlinx.html), `InputHandler` (keys + swipe), `LocalStorageStore`,
  `JsonDeckRepository` (fetches `decks/*.json`). `main()` wires it all together.
- Decks are bundled JSON under `src/jsMain/resources/decks/`; each file has `title`, `order`, and
  `cards` — the **deck id is the filename stem** (not a field in the file). `JsonDeckRepository`
  decodes a `DeckFile` DTO (title+cards) and supplies `id` from the fetch key; `Deck`/`DeckSummary`
  still carry `id` as a domain field. Adding a deck needs no code change.
- **Lazy loading**: `DeckRepository` has `loadIndex()` (fetched once at startup → the home list of
  `DeckSummary`) and `loadDeck(id)` (fetched only when a deck is opened/resumed, then memoized).
  `selectDeck`/`resume` on the controller are `suspend`; `DomRenderer` launches selection on an
  injected `CoroutineScope`. Note the offline service worker (see the Offline bullet below)
  precaches *all* decks in the background, so lazy loading governs first-paint on the critical path
  while the SW handles offline durability — complementary, not redundant; nothing to remove here.
- **Generated index**: `decks/index.json` is NOT checked in — the `generateDeckIndex` Gradle task
  (in `build.gradle.kts`, uses `groovy.json.JsonSlurper`) reads the deck files, sorts by `order`,
  and writes `{id,title,cardCount}` into `build/generated/deckIndex/decks/`, which is wired into the
  jsMain resources and made a `dependsOn` of `jsProcessResources`.
- Navigation is wrap-free (past the last card → Complete screen). Persistence stores
  `{deckId, naturalIndex}` (shuffle-independent); shuffle state is NOT persisted.
- **Offline**: a service worker (`src/jsMain/resources/sw.js`) precaches the app + all decks so it
  works with no connection (e.g. on a plane) and survives reloads/tab eviction. It uses **two
  independently-versioned caches** — shell (`index.html`/`flasher.js`/`styles.css`) and decks — so a
  deck edit does NOT re-download the ~800 KB bundle, and vice-versa. Versions are build-injected
  hashes of each bucket's source inputs, emitted to `sw-version.js` by the `generateSwVersion` task
  (a `dependsOn` of `jsProcessResources`); no manual version bumping. `flasher.js` keeps its stable
  filename — precache uses `fetch({cache:'reload'})` to avoid a stale HTTP-cached bundle.
  Registration (`Main.kt`) is **gated to production** via webpack's `NODE_ENV`, so the dev server
  never caches stale assets. No installable-PWA polish (manifest/icons) — deliberately out of scope.
- **Deck validation**: `validateDecks` (a Groovy task in `build.gradle.kts`, no deps) checks every
  deck file — kebab-case filename stem (≤50; it's the id, so uniqueness is free), no stray `id`
  field, required `title` (≤60), non-empty `cards` with non-blank `front`/`back` (≤200). It's a
  `dependsOn` of `generateDeckIndex` and of `check`, so a bad deck fails the build/CI/publish instead
  of shipping. The authoring contract for outside contributors is `CONTRIBUTING-DECKS.md` + the
  machine-readable `docs/deck.schema.json` (kept in sync with the task by hand); submissions come in
  via GitHub Discussions. Note `order` is maintainer-assigned — contributors are told not to set it.

## Testing convention

TDD for domain logic (tests in `commonTest`). Browser-facing code (`DomRenderer`, `InputHandler`,
`LocalStorageStore`) is covered by Karma/Chrome tests in `jsTest` that mount into a real DOM.

## Publishing

`.github/workflows/publish.yml` runs on push to `main` (+ manual dispatch): builds the dist and
pushes it into the `breischl/breischl.github.io` Hugo site at `static/apps/flasher/`, which
triggers that site's own Pages deploy. Live at https://breischl.dev/apps/flasher/.

- Two-hop deploy: flasher publish → commit to site repo → site `deploy.yml` → Pages. Two separate
  Actions runs, short delay.
- Requires the repo secret `BREISCHL_DEV_REPO_ACCESS_TOKEN` (a token with `contents: write` on
  `breischl.github.io`).
- The app is served as plain static files (no Hugo theme). Its relative asset/`decks/` paths let
  it run under the `/apps/flasher/` subpath unchanged — keep paths relative.

See `TODO.md` for deferred features and follow-ups.
