# Contributing a deck to Flasher

Flasher decks are plain JSON files. Anyone can author one — by hand, or by asking an AI agent to
build it — and submit it for inclusion. This guide is the contract: follow it and your deck will
pass validation and show up in the app.

If you're an agent: the machine-readable contract is [`docs/deck.schema.json`](docs/deck.schema.json).
Produce a file that conforms to it, then also honor the two cross-file rules below (which a schema
can't express).

## What a deck looks like

One JSON object per file, saved as `src/jsMain/resources/decks/<id>.json`:

```json
{
  "id": "greetings-introductions",
  "title": "Greetings & Introductions",
  "cards": [
    { "front": "Hi / Bye (informal)", "back": "Ciao (chow)" },
    { "front": "Good morning / Hello", "back": "Buongiorno (bwohn-JOR-noh)" },
    { "front": "My name is...", "back": "Mi chiamo... (mee KYAH-moh)" }
  ]
}
```

## The rules

| Field   | Required | Rules |
|---------|----------|-------|
| `id`    | yes      | Kebab-case (`^[a-z0-9]+(-[a-z0-9]+)*$`), **≤ 50 chars**. **Must equal the filename stem** — a deck with `"id": "food-basics"` must be the file `food-basics.json`. **Must be unique** across all decks. |
| `title` | yes      | Non-blank, **≤ 60 chars**. Shown in the home list and as the deck heading. |
| `cards` | yes      | Non-empty array. Each card is `{ "front": <string>, "back": <string> }`. |
| `front` / `back` | yes | Non-blank, **≤ 200 chars each**. Keep them short so they stay readable on a phone. |
| `order` | **no** — do not set it | Controls the deck's position in the home list. The maintainer assigns it at merge time; you can't know how your deck should sort relative to the existing ones, so leave it out. |

More things to know:

- **Plain text only.** HTML and Markdown are *not* interpreted — `<b>hi</b>` shows up literally as
  `<b>hi</b>`. Same for the `title`.
- **Strict JSON.** No comments, no trailing commas, keys and strings must be double-quoted.
- **No extra fields.** Stick to the fields above.

## Validate it locally (optional)

If you've cloned the repo, drop your file in `src/jsMain/resources/decks/` and run:

```
./gradlew validateDecks
```

It checks every rule above (including the filename/uniqueness rules) and prints a clear message for
anything wrong. The same check runs in CI, so an invalid deck can't reach the live site.

## Submitting your deck

**Open a GitHub Discussion:** https://github.com/breischl/flasher/discussions

Start a new discussion, describe the deck, and paste your JSON. (Posting a Discussion does require a
free GitHub account — that's the only step that does.) The maintainer will review it, assign the
`order`, and commit it. Once merged and deployed it's visible to everyone at
https://breischl.dev/apps/flasher/.
