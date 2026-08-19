# asonar

**A**pp **S**tore **O**ptimization radar — a self-hosted backend that fetches **real keyword
popularity** (the 0–100 index) and **ranking** data from the app stores, tracks it over time, and
suggests keywords worth targeting.

It reads the internal Apple Search Ads popularity endpoint (and the Google Play equivalent) through
browser automation, snapshots popularity / rank / top-10 per keyword into a local database, and
exposes the history so you can see what's moving before and after a release.

## Architecture

Clean architecture, Kotlin + Ktor, in four Gradle modules — dependencies point inward only:

| Module | Holds | Depends on |
|---|---|---|
| `domain` | Models, repository interfaces, services, use cases (the business rules) | — |
| `infrastructure` | Exposed tables + repository implementations, database factories, kdriver scrapers | `domain` |
| `presentation` | Ktor HTTP layer: routes, serialization, error handling | `domain` |
| `app` | Ktor entrypoint, Koin wiring, configuration | all three |

Stack: Kotlin 2.3 · Ktor 3.4 · Koin 4.1 (DI) · Exposed 1.2 (ORM) · HikariCP · H2 / MySQL ·
kotlinx serialization/datetime/coroutines · Kover (coverage).

## Running

```sh
./gradlew :app:run
```

Boots on `:8080` against a **zero-config local H2 file database** under `./data/` (gitignored), so
no external server is needed. Probe it:

```sh
curl localhost:8080/health   # -> OK
```

To run against MySQL instead, set `DB_PROTOCOL=mysql` plus `DB_HOST` / `DB_NAME` / `DB_USER` /
`DB_PASSWORD` (see `app/src/main/resources/application.conf`).

## Status

Scaffold: the skeleton boots with a health probe and the database plumbing wired. The keyword
entities (tables + repositories), the kdriver scrapers, and the tracking/suggestion use cases are
built next.

## Data

Everything scraped — the browser profile and fetched payloads — lives under `./data/`, which is
gitignored. Nothing scraped is ever committed.
