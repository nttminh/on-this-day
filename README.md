# On This Day

A Spring Boot web app that pulls real historical events from the Wikipedia
[**On This Day**](https://api.wikimedia.org/wiki/Feed_API/Reference/On_this_day) API
and displays them in a Giphy-style browsing UI. No hand-entered data — everything comes
from a live public API.

> Portfolio project demonstrating Spring Boot, WebClient, JPA, scheduling, caching, and a
> curated REST API behind a playful frontend.

## Data source

[Wikimedia "On This Day" REST API](https://api.wikimedia.org/wiki/Feed_API/Reference/On_this_day)
(no API key required):

```
https://api.wikimedia.org/feed/v1/wikipedia/en/onthisday/{type}/{month}/{day}
```

`{type}` is one of `events`, `selected`, `births`, `deaths`, or `holidays`.

## Tech stack

- Java 21, Spring Boot 3.4
- Spring WebFlux `WebClient` — calls the Wikipedia API
- Spring Web (MVC) — REST endpoints (later steps)
- Maven build

## Build order

This project is being built up in steps:

1. **✅ Basic Spring Boot app + WebClient call to Wikipedia API, print to console** ← *you are here*
2. Map JSON to DTOs/entities, add persistence layer (Spring Data JPA + Postgres/H2)
3. Add scheduled daily fetch job (`@Scheduled`)
4. Build REST endpoints on top of stored data
5. Add curation/scoring + tagging logic
6. Pagination for infinite scroll
7. Frontend (masonry grid + detail page)
8. Polish: tests, Swagger docs, deploy

## Running (step 1)

Requires JDK 21+.

```bash
./mvnw spring-boot:run      # or: mvn spring-boot:run
```

On startup the app calls the Wikipedia API for **today's** date and prints the events to
the console, e.g.:

```
=== On This Day: 2026-08-26 ===
Fetched 42 events for 2026-08-26. Showing them below:

[1920] The Nineteenth Amendment to the United States Constitution ...
        read: https://en.wikipedia.org/wiki/Nineteenth_Amendment_...
        image: https://upload.wikimedia.org/...
```

### Configuration

See `src/main/resources/application.yml`:

| Property | Description |
| --- | --- |
| `wikipedia.api.base-url` | Wikimedia REST API root (default `https://api.wikimedia.org`) |
| `wikipedia.api.user-agent` | Descriptive User-Agent sent with every request (requested by Wikimedia) |

## Project layout

```
src/main/java/com/portfolio/onthisday
├── OnThisDayApplication.java          # Spring Boot entry point
├── client/WikipediaOnThisDayClient.java   # WebClient wrapper over the feed API
├── config/WikipediaClientConfig.java  # WebClient bean (base URL + User-Agent)
├── dto/                               # Records mapping the API JSON
│   ├── OnThisDayResponse.java
│   ├── OnThisDayEvent.java
│   └── WikiPage.java
└── runner/TodayEventsConsoleRunner.java   # Step 1: print today's events on startup
```

## Testing

```bash
mvn test
```

The DTO mapping test runs offline (no network). The console runner is disabled under the
`test` profile so the suite never makes live API calls.
