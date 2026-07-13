// Flasher service worker — makes the app work offline (e.g. on a plane).
//
// Two independently-versioned caches so the frequent case (editing a deck) does NOT
// force users to re-download the large flasher.js bundle, and vice-versa:
//   - shell cache: index.html, flasher.js, styles.css  (keyed by __CODE_VERSION)
//   - deck cache:  decks/index.json + every deck        (keyed by __CONTENT_VERSION)
// The versions are build-injected hashes of each bucket's source inputs (see the
// generateSwVersion Gradle task), so a deploy invalidates exactly the bucket that changed.
//
// All URLs are relative so the SW works under the /apps/flasher/ subpath on breischl.dev.

importScripts("sw-version.js"); // defines self.__CODE_VERSION and self.__CONTENT_VERSION

const SHELL_CACHE = "flasher-shell-" + self.__CODE_VERSION;
const DECK_CACHE = "flasher-decks-" + self.__CONTENT_VERSION;

const SHELL_ASSETS = ["./", "index.html", "flasher.js", "styles.css"];

// Force the network (not the browser HTTP cache) when precaching. flasher.js keeps a
// stable filename, so without this a re-precache could pull a stale cached bundle.
async function precache(cacheName, urls) {
    const cache = await caches.open(cacheName);
    await Promise.all(
        urls.map(async (url) => {
            const response = await fetch(url, { cache: "reload" });
            if (response.ok) await cache.put(url, response);
        })
    );
}

async function deckUrls() {
    const response = await fetch("decks/index.json", { cache: "reload" });
    const summaries = await response.json();
    return ["decks/index.json", ...summaries.map((d) => "decks/" + d.id + ".json")];
}

self.addEventListener("install", (event) => {
    event.waitUntil(
        (async () => {
            // Only (re)precache a bucket whose version changed — if the cache already
            // exists, its inputs are unchanged, so skip the download.
            if (!(await caches.has(SHELL_CACHE))) {
                await precache(SHELL_CACHE, SHELL_ASSETS);
            }
            if (!(await caches.has(DECK_CACHE))) {
                await precache(DECK_CACHE, await deckUrls());
            }
            await self.skipWaiting();
        })()
    );
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        (async () => {
            // Drop only stale buckets; keep the two current ones.
            const keep = new Set([SHELL_CACHE, DECK_CACHE]);
            const names = await caches.keys();
            await Promise.all(names.filter((n) => !keep.has(n)).map((n) => caches.delete(n)));
            await self.clients.claim();
        })()
    );
});

self.addEventListener("fetch", (event) => {
    if (event.request.method !== "GET") return;
    event.respondWith(
        (async () => {
            const cached = await caches.match(event.request);
            if (cached) return cached;

            const response = await fetch(event.request);
            // Keep any lazily-opened deck cached for next time (offline resilience).
            if (response.ok && new URL(event.request.url).pathname.includes("/decks/")) {
                const cache = await caches.open(DECK_CACHE);
                cache.put(event.request, response.clone());
            }
            return response;
        })()
    );
});
