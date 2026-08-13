# Book Illustration Studio

Turns a book's text into character portraits and chapter illustrations using the Gemini API.

The application is organized into five sequential steps, each run manually by the user:

**Style → Characters → Portraits → Chapters → Illustrations**

## Quick Start

```bash
cp .env.example .env
# Edit .env and set GEMINI_API_KEY

./start.sh
```

### Services

| Service    | URL                                         |
| ---------- | ------------------------------------------- |
| Backend    | http://localhost:8080                       |
| Frontend   | http://localhost:5173                       |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |

## Tests

```bash
./test.sh
```

Runs the backend unit tests using **JUnit + Mockito**.

Tests do not make real Gemini API calls, so they do not consume API quota.

See [`TESTING.md`](TESTING.md) for the testing strategy and latest test report.

## Prerequisites

* Java 21
* Maven, or the bundled `./mvnw`
* Node.js 20+
* npm
* Docker + Docker Compose
* A Gemini API key

Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey).

## Environment Variables

See `.env.example` for the complete configuration.

The only required environment variable is:

```env
GEMINI_API_KEY=your_api_key_here
```

Postgres credentials are pre-configured for local development in `docker-compose.yml`. These are not production secrets; the database is intended for a throwaway local container.

## Architecture

### Backend

* **Spring Boot**
* **Java 21**
* **PostgreSQL**
* **Flyway** for database migrations
* **JPA / Hibernate**
* `ddl-auto=validate`

Database migrations are the source of truth. Hibernate does **not** automatically create or modify the database schema.

### Frontend

* **React**
* **Vite**
* **TypeScript**

### Storage

Book text and generated images are stored on the local filesystem:

```text
./data/books
./data/images
```

They are served through dedicated backend API endpoints.

No S3 or CDN is used, in accordance with the assignment's scope constraints.

### Gemini Integration

The application communicates with Gemini through the **REST Interactions API**, rather than the Gemini SDK.

There are two independent conversation chains per project:

```text
Text Chain
Book Upload
    ↓
Style
    ↓
Characters
    ↓
Chapters

Image Chain
Portrait Setup
    ↓
Portraits
    ↓
Illustration Setup
    ↓
Illustrations
```

The book's content is sent to Gemini only once. Subsequent requests reuse the conversation through `previous_interaction_id`.

This avoids repeatedly sending the same book content for every step.

### Pipeline State

Each project tracks two independent pieces of state:

* `status` — the last successfully completed step
* `step_state` — the current execution state:

  * `IDLE`
  * `RUNNING`
  * `FAILED`

A timestamp is also stored to detect a step that became stranded while a Gemini request was in progress, for example after a server restart.

This allows the application to recover without requiring manual database intervention.

See [`DECISIONS.md`](DECISIONS.md) for the detailed reasoning behind this design.

### Concurrency

The application uses **JPA optimistic locking** with `@Version`.

An explicit `RUNNING` check is also performed inside the same transaction before starting a Gemini call.

This prevents duplicate execution caused by:

* Refreshing the page
* Opening the project in multiple tabs
* Double-clicking a step
* Multiple requests being submitted concurrently

As a result, the same Gemini step cannot be accidentally triggered multiple times.

## AI-Assisted Development

This project was built with AI coding assistance using **Claude** through chat-driven iterative development.

The project includes:

* [`AGENTS.md`](AGENTS.md) — project context and instructions provided to the AI assistant
* [`DECISIONS.md`](DECISIONS.md) — record of important implementation decisions, including cases where AI-generated output was incorrect and had to be corrected

These files document both the development process and the reasoning behind the final implementation.

## Known Limitations

See the **"If we had one more day"** section in [`DECISIONS.md`](DECISIONS.md) for known limitations and potential improvements.
