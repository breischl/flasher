# TODO / Ideas

Nothing here is required — the app is complete and deployed. These are deliberately deferred
(YAGNI) features and minor follow-ups, recorded so they aren't lost.

## Deferred features

- **Mastery tracking** — mark cards "known" vs "still learning" per deck; surface a study subset.
- **In-app deck editing** — currently decks are bundled JSON only; no create/edit UI.
- **"Which side first"** option — per-deck choice of prompt vs answer as the starting face. The
  DeckOptions screen is the intended home for this (alongside the existing Shuffle toggle).
- **Persist shuffle** — shuffle state/order is intentionally not saved across sessions today.

## Minor follow-ups

- **Exclude `flasher.js.map`** from the published bundle if the source map isn't wanted on the live
  site (harmless, but adds weight). Would be a copy filter in `publish.yml`.
- **Bump GitHub Actions to `@v5`** (`actions/checkout`, `actions/setup-java`, `gradle/actions`) to
  clear the Node 20 deprecation warning.
- **Gradle 10 readiness** — a future Kotlin plugin release should drop the deprecated Gradle usages
  currently warned about.
