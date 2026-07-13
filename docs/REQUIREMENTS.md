# Flasher — Behavioral Requirements

This document is the **behavioral contract** for Flasher: what the app must do, stated as
numbered, testable requirements. It exists to guide future development and to prevent accidental
regressions — if a change would violate a requirement here, that's either a bug or a deliberate
decision that must update this document in the same commit.

**What this document is not:**

- Architecture, toolchain, and build details live in [`CLAUDE.md`](../CLAUDE.md) and
  [`README.md`](../README.md).
- Deferred features and ideas live in [`TODO.md`](../TODO.md).
- The machine-readable deck-file schema is [`deck.schema.json`](deck.schema.json); the authoring
  guide for contributors is [`CONTRIBUTING-DECKS.md`](../CONTRIBUTING-DECKS.md).

**Document Conventions:**

- IDs are permanent: never renumber or reuse them. New requirements take the next number in their
  section.
- A dropped requirement stays listed, marked *(withdrawn — date, reason)*, so old references don't
  dangle.
- Wording edits that don't change meaning need no ceremony.
- Any change to user-visible behavior must update this document in the same commit.

---

## HOME — deck list

- **HOME-1** The home screen lists every bundled deck, ordered by the deck's maintainer-assigned
  `order` value.
- **HOME-2** Each deck entry shows the deck's title and its card count.
- **HOME-3** Selecting a deck opens that deck's options screen (OPT).
- **HOME-4** Only the lightweight deck index (id, title, card count) is loaded to render the home
  screen; a deck's cards are fetched the first time the deck is opened or resumed, then cached for
  the session (lazy loading).
- **HOME-5** If no decks are available (e.g. the index fails to load), the home screen still
  renders, with a "no decks" message instead of a broken page.
- **HOME-6** The home screen links out to the site root (the app lives under a subpath of a larger
  site) and to the deck-contribution guide on GitHub.

## OPT — deck options

- **OPT-1** The options screen shows the deck's title and card count, a shuffle toggle, a
  start-side toggle, and a Start action.
- **OPT-2** Shuffle defaults to **off** every time a deck is opened; it is a per-session choice
  (see PERSIST-6).
- **OPT-3** The start-side toggle chooses which face shows first: prompt (default) or answer.
- **OPT-4** Starting with shuffle on plays the deck in a random permutation; the deck's card data
  is never mutated — shuffle is only a play order.
- **OPT-5** A back action returns to the home screen.

## NAV — studying a deck

- **NAV-1** The study screen shows the current card's visible face, labeled as prompt or answer,
  plus a progress indicator in the form "current / total" (1-based).
- **NAV-2** Tapping/clicking the card (or pressing Space or Enter) is the **one-gesture walk**: if
  the card is on its starting face, it reveals the other face; once revealed, the same gesture
  advances to the next card. Repeating one gesture walks the whole deck.
- **NAV-3** The card displays a hint telling the user what the gesture will do next (flip vs.
  advance).
- **NAV-4** Prev/Next buttons, horizontal swipes, and Left/Right arrow keys move to the previous /
  next card. All input methods are equivalent ways of invoking the same actions.
- **NAV-5** Navigation never wraps. Advancing past the last card shows the Complete screen;
  going back from the first card does nothing (the Prev button is disabled there).
- **NAV-6** On the last card, the Next button reads as "Finish" (advancing completes the deck).
- **NAV-7** Moving to another card (prev or next) always shows that card's starting face, as
  chosen on the options screen — never a remembered flip state.
- **NAV-8** A swipe only navigates when it is predominantly horizontal and exceeds a minimum
  distance; vertical scrolling gestures must not trigger navigation.
- **NAV-9** Keyboard and swipe input act only while studying — they must have no effect on the
  Home, options, or Complete screens.
- **NAV-10** A Home action is always available while studying; it abandons the session (clearing
  the saved position, PERSIST-3) and returns to the deck list.

## COMP — deck complete

- **COMP-1** The Complete screen confirms the deck is finished, naming the deck and its card
  count.
- **COMP-2** If another deck follows in home-list order, the Complete screen offers to start it
  directly; taking that shortcut starts the next deck with default options (no shuffle, prompt
  first).
- **COMP-3** The Complete screen always offers a return to home.
- **COMP-4** Starting a deck with zero cards goes straight to the Complete screen rather than
  breaking. (Bundled decks can't be empty per FORMAT-4, but the app must tolerate it.)

## PERSIST — remembering your place

- **PERSIST-1** While studying, the app continuously saves the current position — deck id plus the
  card's **natural** (unshuffled) index — to browser localStorage.
- **PERSIST-2** On startup, if a saved position exists and is valid, the app resumes directly into
  the study screen at that card. Resume uses natural order with default options (no shuffle,
  prompt first) and the card unflipped.
- **PERSIST-3** The saved position is cleared when the deck is completed or the user leaves via
  Home; there is nothing to resume afterward.
- **PERSIST-4** An invalid saved position — unknown deck id, out-of-range index, or unparseable
  data — falls back to the home screen; it must never crash or wedge the app.

## OFFLINE — working without a connection

- **OFFLINE-1** After the first visit with a connection, the app works fully offline: the app
  shell and **all** decks are precached by a service worker, and this survives reloads and tab
  eviction.
- **OFFLINE-2** The app shell and the decks are cached in two independently versioned buckets: a
  deck edit must not force re-downloading the (large) app bundle, and an app change must not force
  re-downloading the decks.
- **OFFLINE-3** Cache versions are derived from the content at build time; publishing a change
  invalidates exactly the affected bucket, with no manual version bumping.
- **OFFLINE-4** Updating a cached asset must fetch from the network, not the browser's HTTP cache
  (the JS bundle keeps a stable filename, so a stale HTTP-cached copy is a real hazard).
- **OFFLINE-5** The service worker is active only in production builds; the dev server must never
  serve stale cached assets.
- **OFFLINE-6** Decks fetched lazily at runtime are added to the deck cache, so anything viewed
  online stays available offline even before precaching finishes.

## FORMAT — deck files

The authoritative machine-readable contract is [`deck.schema.json`](deck.schema.json); the
authoring guide is [`CONTRIBUTING-DECKS.md`](../CONTRIBUTING-DECKS.md). Behaviorally:

- **FORMAT-1** A deck is a single JSON file bundled under `decks/`; its **id is the filename
  stem** (kebab-case, ≤ 50 chars) and is not stored inside the file. Adding a deck requires no
  code change.
- **FORMAT-2** A deck file contains `title` (non-blank, ≤ 60 chars), optional maintainer-assigned
  integer `order`, and `cards` — and nothing else.
- **FORMAT-3** Each card is `{ "front", "back" }`, both non-blank and ≤ 200 chars. Content is
  plain text: HTML/Markdown is never interpreted.
- **FORMAT-4** `cards` must be a non-empty array.
- **FORMAT-5** The deck index (`decks/index.json`) is generated at build time from the deck files,
  sorted by `order`; it is never hand-maintained.
- **FORMAT-6** An invalid deck file fails the build (and therefore CI and publish) with a clear
  message — a malformed deck must never reach the live site.
- **FORMAT-7** `deck.schema.json` and the build-time validation must be kept in sync; a change to
  the deck contract updates both (and this section).

## UX — presentation and platform

- **UX-1** The app is mobile-first: layout, touch targets, and card text sizing are designed for
  phones, while remaining usable on desktop.
- **UX-2** The app adapts to the platform's light/dark color scheme preference.
- **UX-3** The UI uses no images; presentation is pure CSS.
- **UX-4** The app is a static site: plain files servable verbatim from any static host, with no
  backend and no server-side rendering.
- **UX-5** All asset and deck URLs are relative, so the app runs unchanged from a subpath (it is
  deployed under `/apps/flasher/`).

## Non-goals

Deliberate exclusions. Do not "helpfully" add these; adding one is a scope decision for the
maintainer, not an improvement.

- **No backend, accounts, or sync** — the app is a static site; all state is in the browser.
- **No installable-PWA polish** (web app manifest, icons, install prompt) — offline support via
  the service worker is in scope; installability is not.
- **No rich card content** — no HTML, Markdown, images, or audio in cards; plain text only.
