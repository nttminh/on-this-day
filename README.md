# On This Day

A Spring Boot web app that pulls **real historical events** from the Wikipedia
[**On This Day**](https://api.wikimedia.org/wiki/Feed_API/Reference/On_this_day) API,
curates and tags them, and displays them in a **Giphy-style** browsing UI — a colorful
masonry grid with infinite scroll and full detail pages.

> Portfolio project demonstrating Spring Boot, WebClient, Spring Data JPA, scheduling,
> caching, a curated REST API, a server-rendered frontend, tests, OpenAPI docs, and Docker.
> No hand-entered data — everything comes from a live public API.

---

## Features

- **Live data** from the Wikipedia "On This Day" feed (events, selected, births, deaths, holidays).
- **Daily scheduled fetch** (`@Scheduled`) plus **fetch-on-miss** — visiting a never-seen date
  fetches and stores it on demand.
- **Persistence** with Spring Data JPA — H2 for local dev, PostgreSQL in Docker.
- **Curation / scoring** — ranks entries by image, summary, provenance, and notable years.
- **Tagging** — deterministic keyword themes (science, space, war, politics, disaster, …),
  a "firsts" theme, feed-type tags, and decade buckets (e.g. `1960s`) — powers browse-by-tag
  and related-events queries.
- **Dedupe** — near-duplicate entries across feeds collapse to the best-scored variant;
  a SHA-256 `externalKey` prevents duplicate rows across re-fetches.
- **REST API** with pagination for infinite scroll and RFC-7807 error responses.
- **Caching** (`@Cacheable`) so the Wikipedia API isn't hammered.
- **Frontend** (Thymeleaf + vanilla JS): masonry grid, tag chips, infinite scroll,
  and a full detail page with related sections.
- **OpenAPI / Swagger UI**, **actuator health**, and a **Docker / docker-compose** setup.

---

## Quick start

### Option A — run locally (H2, zero setup)

Requires JDK 21+.

```bash
./mvnw spring-boot:run        # or: mvn spring-boot:run
```

Open **http://localhost:8080**. On startup the app fetches today's real events from
Wikipedia (if reachable) and stores them in a local H2 file database (`./data`).

No internet access? Enable the bundled real-event seed so the UI has content:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--onthisday.seed.enabled=true"
```

### Option B — run with Docker (PostgreSQL)

Requires Docker + Docker Compose.

```bash
docker compose up --build
```

This starts PostgreSQL and the app (profile `docker`). Open **http://localhost:8080**.
The `docker` profile loads the real-event seed by default so the demo has content even if
the container can't reach Wikipedia; set `onthisday.seed.enabled=false` for pure live data.

---

## Endpoints

### Web (Thymeleaf)

| Path | Description |
| --- | --- |
| `/` | Today — masonry grid with infinite scroll |
| `/date/{month}/{day}` | Browse any date (optional `?tag=science`) |
| `/event/{id}` | Full detail page with related events |

### REST API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/digest/today` | Curated top picks for today |
| `GET` | `/api/digest/{month}/{day}` | Browse a date, paginated (`?tag=&page=&size=`) |
| `GET` | `/api/digest/{month}/{day}/tags` | Distinct tags for a date (chip row) |
| `GET` | `/api/event/{id}` | Single event detail |
| `GET` | `/api/event/{id}/related` | Related events (same year + shared theme) |

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health:** http://localhost:8080/actuator/health
- **H2 console** (local profile): http://localhost:8080/h2-console

Example:

```bash
curl "http://localhost:8080/api/digest/7/20?size=5" | jq
curl "http://localhost:8080/api/event/1/related" | jq
```

---

## Architecture

```
Wikipedia API ──(WebClient)──> WikipediaOnThisDayClient
                                     │
        ScheduledFetchJob ───────────┤        (daily @Scheduled + startup)
        DigestService (fetch-on-miss)┤
                                     ▼
                 FetchService ── EventMapper ─ TaggingService ─ ScoringService
                                     │  (map, tag, score, dedupe, upsert)
                                     ▼
                              EventRepository (JPA) ── H2 / PostgreSQL
                                     ▲
        REST controllers ── DigestService (@Cacheable) ────┘
        Thymeleaf views  ──────┘
```

### Package layout

```
com.portfolio.onthisday
├── OnThisDayApplication        # entry point (@EnableScheduling, @EnableCaching)
├── client/                     # WikipediaOnThisDayClient (WebClient wrapper)
├── config/                     # WebClient bean, OpenAPI metadata
├── domain/                     # Event entity, FeedType, EventRepository
├── dto/                        # records mapping the Wikipedia JSON
├── service/                    # FetchService, DigestService, EventMapper,
│                               #   ScoringService, TaggingService, DevDataSeeder
├── scheduler/                  # ScheduledFetchJob
├── api/                        # REST controllers, response DTOs, error handling
└── web/                        # ViewController (Thymeleaf pages)
```

---

## Configuration

Key settings in `src/main/resources/application.yml` (override via env vars or
`--flags`):

| Property | Default | Description |
| --- | --- | --- |
| `wikipedia.api.base-url` | `https://api.wikimedia.org` | Wikimedia REST API root |
| `wikipedia.api.path-template` | `/feed/v1/wikipedia/en/onthisday/{type}/{month}/{day}` | Feed path (switch endpoints without code changes — see below) |
| `wikipedia.api.user-agent` | `OnThisDay/0.1 (...)` | Descriptive User-Agent (requested by Wikimedia) |
| `onthisday.fetch.cron` | `0 0 6 * * *` | Daily fetch schedule |
| `onthisday.fetch.on-startup` | `true` | Fetch today's data on startup if missing |
| `onthisday.digest.top-size` | `30` | Size of the "top picks" digest |
| `onthisday.seed.enabled` | `false` | Load the bundled real-event seed (offline/demo) |

### Two interchangeable Wikipedia endpoints

Both expose the same data; switch by changing `base-url` + `path-template` together:

1. **api.wikimedia.org** (default): `path-template: /feed/v1/wikipedia/en/onthisday/{type}/{month}/{day}`
2. **en.wikipedia.org REST v1** (classic, always unauthenticated):
   `base-url: https://en.wikipedia.org`, `path-template: /api/rest_v1/feed/onthisday/{type}/{month}/{day}`

If option 1 returns 401/403 for your network, use option 2.

---

## Testing

```bash
mvn test
```

Covers DTO mapping, all custom JPQL (against embedded H2), the fetch pipeline
(dedupe + upsert, mocked client), tagging and scoring heuristics, and the REST
web slice (JSON shape + validation). The suite is fully offline — no live API calls.

---

## Build order (project journal)

1. ✅ Basic Spring Boot app + WebClient call to Wikipedia API, print to console
2. ✅ Map JSON to DTOs/entities, add persistence layer (JPA + H2/Postgres)
3. ✅ Scheduled daily fetch job (`@Scheduled`) + fetch-on-miss
4. ✅ REST endpoints on top of stored data
5. ✅ Curation/scoring + tagging logic
6. ✅ Pagination for infinite scroll
7. ✅ Frontend (masonry grid + detail page)
8. ✅ Polish: tests, Swagger docs, Docker

### Deploying to Render / Railway (free tier)

The Docker image is self-contained. Point the platform at this repo (it will use the
`Dockerfile`), provision a managed PostgreSQL instance, and set `SPRING_PROFILES_ACTIVE=docker`
plus `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` to the database's values.

---

## Data source & license

Content from the [Wikimedia "On This Day" API](https://api.wikimedia.org/wiki/Feed_API/Reference/On_this_day)
(CC BY-SA). This project is provided under the MIT license.
