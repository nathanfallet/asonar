# scraping

Browser-automation scrapers that fetch keyword data from the two stores, built on
[kdriver](https://github.com/nathanfallet/kdriver) (Kotlin CDP).

Planned:
- **AppleSearchAdsScraper** — drives `app-ads.apple.com`, replays the internal GraphQL
  `recommendationV2.getKeywordPopularities(adamId, storefronts, keywordTerms)` to read the real
  0–100 popularity index, plus per-keyword rank / top-10 snapshots.
- **PlayScraper** — the Google Play equivalent for keyword popularity / ranking.

Each scraper writes into the `*_repositories` so history accumulates over time.

The browser profile and any fetched payloads live under `./data/` at the repo root, which is
**gitignored** — nothing scraped is ever committed.
