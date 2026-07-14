# TODO / Ideas

Nothing here is required. These are deliberately deferred features and minor follow-ups, recorded so they aren't lost.

## Deferred features

- **Smaller JS bundle download** - It's currently rather large for the actual functionality.
- **Mastery tracking** — mark cards "known" vs "still learning" per deck; surface a study subset.
- **Persist shuffle** — shuffle state/order is intentionally not saved across sessions today.

## Minor follow-ups

- **Exclude `flasher.js.map`** from the published bundle if the source map isn't wanted on the live
  site (harmless, but adds weight). Would be a copy filter in `publish.yml`.
- **Bump GitHub Actions to `@v5`** (`actions/checkout`, `actions/setup-java`, `gradle/actions`) to
  clear the Node 20 deprecation warning.
- **Gradle 10 readiness** — a future Kotlin plugin release should drop the deprecated Gradle usages
  currently warned about.
