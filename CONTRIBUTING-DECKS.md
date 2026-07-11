# Contributing a deck to Flasher

Flasher decks are plain JSON files. Anyone can author one — by hand, or by asking an AI agent to
build it — and submit it for inclusion. This guide is the contract: follow it and your deck will
pass validation and show up in the app.

If you're an agent: the machine-readable contract is [`docs/deck.schema.json`](docs/deck.schema.json).

## Deck quick start

A JSON file, named using kebab-case (e.g. `food-basics.json`), with contents like:

```json
{
  "title": "Greetings & Introductions",
  "cards": [
    { "front": "Hi / Bye (informal)", "back": "Ciao (chow)" },
    { "front": "Good morning / Hello", "back": "Buongiorno (bwohn-JOR-noh)" },
    { "front": "My name is...", "back": "Mi chiamo... (mee KYAH-moh)" }
  ]
}
```

## Detailed rules

Filenames must use use kebab-case (e.g. `food-basics.json`), and be ≤ 50 characters long.

| Field   | Required | Rules |
|---------|----------|-------|
| `title` | yes      | Non-blank, **≤ 60 chars**. Shown in the home list and as the deck heading. |
| `cards` | yes      | Non-empty array. Each card is `{ "front": <string>, "back": <string> }`. |
| `front` / `back` | yes | Non-blank, **≤ 200 chars each**. Keep them short so they stay readable on a phone. |
| `order` | **no** — do not set it | Controls the deck's position in the home list. The maintainer assigns it at merge time; you can't know how your deck should sort relative to the existing ones, so leave it out. |

- **Plain text only.** HTML and Markdown are *not* supported anywhere.
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

A few options:
1. **Send it directly to the author** if you already have contact info
2. **Open a PR**, and add the new deck to `src/jsMain/resources/decks`
3. **Open a GitHub Discussion:** Create a (free) Github account, start a [new discussion](https://github.com/breischl/flasher/discussions), describe the deck, and paste your JSON.