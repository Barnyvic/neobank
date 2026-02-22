# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start infrastructure (PostgreSQL + Redis)
docker-compose up -d

# Run application (dev profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build JAR
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=AuthServiceTest

# Run a single test method
mvn test -Dtest=AuthServiceTest#shouldRegisterNewUser

# Run integration tests
mvn test -Dtest=*IntegrationTest
```

API is at `http://localhost:8080`, Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Architecture

Feature-based package structure under `com.vaultpay`:

- **auth** — registration, login, JWT issuance, refresh token lifecycle
- **user** — profile management, KYC level, transaction PIN
- **wallet** — wallet creation, freeze/unfreeze, balance retrieval
- **ledger** — double-entry accounting core (LedgerAccount, JournalEntry, LedgerEntry)
- **transaction** — transfers, deposits, withdrawals; orchestrates ledger + events
- **paystack** — Paystack payment gateway integration and webhook handling
- **common** — shared exceptions, `ApiResponse<T>` wrapper, rate limiting, audit logging, utilities

Each module follows the same layered structure: `controller → service interface → service impl → repository`.

## Key Architectural Decisions

**Double-entry ledger**: Every financial operation (transfer, deposit, withdrawal) posts a balanced journal entry via `LedgerService`. `TransactionService` is the orchestrator — it validates balances, posts the journal entry, persists the `Transaction` record, and publishes a domain event. All of this is wrapped in a single `@Transactional` boundary.

**Stateless JWT security**: No HTTP sessions. `JwtAuthenticationFilter` validates `Authorization: Bearer <token>` on each request. Access tokens expire in 15 minutes; refresh tokens (7 days) are stored in Redis by `RefreshTokenStore`.

**Redis usage**: Three purposes — refresh token storage, rate limiting buckets (Bucket4j), and general caching.

**Rate limiting**: `RateLimitingFilter` (order `HIGHEST_PRECEDENCE + 10`) applies three tiers via Bucket4j + Redis: login (10 req/min), public (60 req/min), authenticated (120 req/min).

**Schema management**: Flyway handles all DDL. JPA is set to `validate` mode — never `update` or `create`. Add new migrations as `V{n}__description.sql` in `src/main/resources/db/migration/`.

**Idempotency**: `TransactionService` checks for an existing `reference` before processing to prevent duplicate operations.

## Conventions

- Request DTOs are Java records (immutable). Response DTOs are classes/records depending on context.
- All controller responses are wrapped in `ApiResponse<T>` from `common/dto`.
- Custom exceptions extend `BusinessException` and carry an `ErrorCode` enum value. `GlobalExceptionHandler` maps them to HTTP status codes.
- All entities extend `Auditable` which adds `createdAt`/`updatedAt` via JPA auditing.
- Unit tests use `@ExtendWith(MockitoExtension.class)` with `@Nested` classes grouping related scenarios.
- Integration tests use TestContainers with a real PostgreSQL instance.

## Environment Variables

Copy `.env.example` to `.env` before running. Required variables:

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `JWT_SECRET` | 256-bit secret for HMAC signing |
| `PAYSTACK_SECRET_KEY` | Paystack API key |
| `REDIS_HOST` / `REDIS_PORT` | Redis connection (defaults: localhost/6379) |
