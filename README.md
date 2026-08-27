# asonar

**A**pp **S**tore **O**ptimization radar — a self-hosted, agent-friendly ASO engine. It fetches the
**real keyword popularity** (Apple's 0–100 search index) and **ranking** data from the App Store,
tracks it over time, scores which keywords are actually worth targeting, and exposes all of it over a
web UI, a REST API and an **MCP** server so an AI agent can run the keyword-optimization loop for you.

It reads the internal Apple Search Ads popularity endpoint through browser automation, snapshots
popularity / rank / top-of-results per keyword into a local database, and turns that into
recommendations — the useful parts of a tool like AppFigures, running locally and open source.

> Currently App Store only; the architecture is multi-store (Play Store is a matter of adding the
> sources + a Play scorer).

## How it works

1. You **track keywords** (a term, a store, a country) — via the web UI, the API, or by asking an
   agent through MCP.
2. Adding a keyword **queues a background fetch**: asonar searches the store, records the
   top-of-results (who ranks, who puts the term in their title/subtitle, each app's review count and
   **30-day review velocity**), reads the keyword's real Apple popularity, and notes where *your* app
   ranks. Fetches are age-gated so refreshing is cheap.
3. asonar **scores each keyword as an opportunity** (see [Scoring](#scoring)) and tells you, per app,
   which keywords are worth chasing and which are walls.
4. You act on it — write titles/subtitles/keyword fields — and re-measure. It's a loop, not a one-shot.

> ⏳ **Give it a few days.** The scoring leans on *30-day review velocity* (the best proxy for the
> download velocity that drives rank). Until a keyword's competitors have a couple of days of history,
> its verdict is `UNKNOWN` and asonar keeps re-fetching it. Real verdicts emerge as the data accrues.

## Quick start

Bring up MySQL + RabbitMQ (the fetch queue), then run the app:

```sh
docker compose up -d          # MySQL 8.4 (:3306) + RabbitMQ (:5672, mgmt :15672)
./gradlew :app:run            # boots on :8080
curl localhost:8080/health    # -> OK
```

Open **http://localhost:8080** — that's the web UI (keywords, apps, and the MCP guide).

Config is namespaced `ASONAR_*` (so it never clashes with generic `DB_*` in your shell), see
`app/src/main/resources/application.conf`:

- `ASONAR_PORT` (default `8080`)
- `ASONAR_DB_PROTOCOL` — `mysql` (default, uses the compose stack) or `h2` for a zero-server local file
  DB under `ASONAR_DB_DIR` (RabbitMQ is still required for fetching)
- `ASONAR_ASA_PROFILE_DIR` — where the Apple Search Ads browser profile is kept (see below)

### Apple Search Ads login (one-off)

Popularity comes from the Apple Search Ads booking endpoint, which needs a logged-in Apple session.
The first popularity fetch opens a real Chrome window (via [kdriver](https://github.com/cdpdriver/kdriver)) —
**log in once**; the session persists in `ASONAR_ASA_PROFILE_DIR`, so later fetches are headless-ish
and reuse it. Stop the app gracefully (don't `pkill` the Chrome) to keep the session.

## Using it

- **Web** — `http://localhost:8080`
  - `/keywords` — every tracked keyword + its latest popularity; add keywords here.
  - `/apps` → `/apps/{id}` — a tracked app's ranking coverage: the opportunity **recommendations**,
    an interactive **rank-over-time chart** (filter by country / Top-N / period), and the full
    keyword table.
  - `/mcp-guide` — how to connect the MCP server to Claude Code / Desktop.
- **REST API**
  - `GET/POST /api/keywords`, `GET/DELETE /api/keywords/{id}`, `POST /api/keywords/{id}/refresh`,
    `GET /api/keywords/{id}/popularity` · `/top-apps` · `/ranks/{appId}`
  - `GET/POST /api/apps`, `GET/DELETE /api/apps/{id}`
  - `GET /api/app-coverage?appId=` — a tracked app's ranking coverage
  - `GET /api/keyword-opportunities?appId=` — the scored recommendations
  - `GET /api/app-ratings?...` — an app's ratings history + 30-day velocity
- **MCP** — `POST http://localhost:8080/mcp` (Streamable HTTP). Tools:
  - apps: `register_app`, `list_apps`, `get_app`, `delete_app`
  - keywords: `track_keyword`, `untrack_keyword`, `list_keywords`, `get_keyword`, `refresh_keyword`
  - history: `get_keyword_popularity_history`, `get_keyword_top_apps`, `get_keyword_ranks`, `get_app_ratings`
  - the brain: `get_app_coverage`, `get_keyword_opportunities`

## Scoring

Each keyword is scored per app into a verdict — **YES** / **YES_BUT** / **NO** / **RESERVE** /
**UNKNOWN** — plus a 0–100 score and a plain-language comment. The model:

- **Popularity** — Apple's 0–100 search index. `5` is the floor (barely searched); below that a keyword
  is parked (`RESERVE`) no matter how winnable.
- **Wall strength** — how strongly the top-of-results holds the term: for each competitor,
  `title-usage × review-strength`, position-weighted (the top counts far more, and a lone but total
  defender still walls it). **Review-strength is the 30-day review velocity** — a stale giant is
  beatable, a fast climber isn't; a big *old* review total is deliberately not used (ratings have no
  direct rank correlation). Without that velocity yet → `UNKNOWN`.
- **Our velocity vs theirs** — if you gain reviews faster than the leaders, you can climb even a real wall.

The scorer is a pure, unit-tested function selected per store, so weights/thresholds can be re-tuned
without re-fetching.

## Architecture

Clean architecture, Kotlin + Ktor, four Gradle modules — dependencies point inward only:

| Module | Holds | Depends on |
|---|---|---|
| `domain` | Models, repository interfaces, use cases (the business rules + scoring) | — |
| `infrastructure` | Exposed tables + repositories, DB factories, kdriver / iTunes scrapers, RabbitMQ | `domain` |
| `presentation` | Ktor HTTP layer: web routes, REST, MCP tools, serialization | `domain` |
| `app` | Ktor entrypoint, Koin wiring, configuration | all three |

Stack: Kotlin 2.3 · Ktor 3.4 · Koin 4.1 (DI) · Exposed 1.2 (ORM) · HikariCP · MySQL 8.4 / H2 ·
RabbitMQ (kourier) · kdriver (Chrome/CDP) · kotlinx serialization/datetime/coroutines · Kover.

Snapshots are append-only; reads assemble the current picture from history. Writes are batched (one
transaction per fetch) and the read paths batch-load per keyword, so it stays fast as the history grows.

## Data & privacy

Everything scraped — the browser profile and fetched payloads — lives under `./data/` (or your
`ASONAR_DB_DIR` / `ASONAR_ASA_PROFILE_DIR`), which is gitignored. **Nothing scraped is ever committed.**
