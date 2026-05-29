# VaultPay Neobank

VaultPay is a **digital banking backend** that lets users register, hold wallets in multiple currencies, move money between wallets, fund wallets via card/bank (Paystack), and withdraw to Nigerian bank accounts. Every monetary movement is recorded in a **double-entry ledger** so balances stay auditable and consistent with accounting rules.

The system is delivered as a **single Spring Boot monolith**: feature modules live in separate packages but run in one JVM, call each other through injected services, and use **Spring application events** for side effects (retries, refunds) without tangling the core write path.

---

## What problem it solves

Traditional “wallet balance” fields updated in place are hard to audit and easy to corrupt under concurrency or partial failures. VaultPay treats the **ledger as the source of truth**:

- Each user wallet gets a **ledger account** (asset) when the wallet is created.
- Transfers, deposits, and withdrawals post **balanced journal entries** (debits = credits).
- The wallet’s displayed balance is kept in sync from the ledger account (with pessimistic locking on writes).
- A **Paystack liability** system account represents money held at the gateway until funding settles or withdrawals complete.

That design supports reconciliation, reversals, and future extraction of the ledger into its own service without rewriting business rules.

---

## Core concepts

| Concept | Role |
|--------|------|
| **User** | Identity, profile, transaction PIN, KYC-related fields. Created at registration. |
| **Wallet** | Per-user, per-currency store of value (`NGN`, etc.). Has a unique wallet number for P2P transfers. |
| **Ledger account** | Accounting bucket tied to a wallet (or system name like `PAYSTACK_LIABILITY`). Balance updated only via journal entries. |
| **Journal entry** | One business event (transfer, deposit, withdrawal) with a unique reference and balanced debit/credit lines. |
| **Transaction** | User-facing record (`TRANSFER`, `DEPOSIT`, `WITHDRAWAL`, `REVERSAL`) with status, amount, wallets, and optional Paystack metadata. |
| **Paystack** | External PSP: initialize payments, verify webhooks, create transfer recipients, initiate bank payouts. |

**Transaction statuses** (simplified): `PENDING` (awaiting Paystack payment), `PENDING_EXTERNAL` (ledger debited, payout in flight), `COMPLETED`, `FAILED`, plus reversal-related states after refunds.

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 17, Spring Boot 3.2 |
| API | REST, Springdoc OpenAPI (Swagger) |
| Security | Spring Security, JWT access tokens, refresh tokens in **Redis** |
| Persistence | PostgreSQL 16, Spring Data JPA, Flyway migrations |
| Cache / rate limits | **Redis** (wallet balance cache, login attempt counters, transfer throttle keys, refresh tokens) |
| Payments | Paystack REST API + signed webhooks |
| Resilience | Resilience4j (retries, circuit breaker on Paystack HTTP) |
| Ops | Docker, Docker Compose (app + Postgres + Redis) |

---

## High-level system view

```
                +----------------------+
                |  Client Applications |
                |  Web App · Mobile    |
                +----------+-----------+
                           |
                           |  HTTPS + Bearer JWT
                           v
                +----------------------+
                |  VaultPay Monolith   |
                |  (Spring Boot)       |
                |                      |
                |  REST API /api/v1    |
                |  JWT · Security      |
                |                      |
                |  auth · user · wallet|
                |  ledger · transaction|
                |  paystack · common   |
                +----------+-----------+
                           |
         +-----------------+------------------+
         |                 |                  |
         v                 v                  v
+----------------+ +---------------+ +----------------------+
| PostgreSQL     | | Redis         | | Paystack API         |
| users · wallets| | refresh tokens| | card payments        |
| ledger · txn   | | balance cache | | bank transfers       |
| audit_log      | | throttles     | | signed webhooks      |
+----------------+ +---------------+ +----------+-----------+
                                                    |
                                                    v
                                         +----------------------+
                                         | Customer Banks       |
                                         | cards · NUBAN payout |
                                         +----------------------+
```

**How it fits together:** clients hit the monolith over HTTPS with JWTs. Domain services persist money and identity in **PostgreSQL** (ledger is source of truth), use **Redis** for tokens/cache/throttles, and talk to **Paystack** for deposits and withdrawals. Paystack sends async **webhooks** back into the same API (`POST /api/v1/paystack/webhook`) to complete or reverse transactions. P2P transfers stay entirely inside the monolith and database.

---

## Modules

| Module | Responsibility |
|--------|----------------|
| **auth** | Register, login, refresh, logout; JWT issuance; refresh token storage in Redis; login attempt limiting. |
| **user** | Profile, transaction PIN setup/verification. |
| **wallet** | Create/list wallets; link each wallet to a ledger account; balance reads (with Redis cache). |
| **ledger** | Post journal entries, enforce debits = credits, update account balances under lock, sync `Wallet.balance`. |
| **transaction** | Transfer, fund, withdraw, history; idempotency; wallet locks; throttle; funding completion from webhooks. |
| **paystack** | HTTP client to Paystack (resilient), webhook signature verification. |
| **common** | `ApiResponse`, exceptions, audit, reference generation, cache config, shared events. |

### Domain events

| Event | Published when | Consumer behaviour |
|-------|----------------|-------------------|
| `WalletFundedEvent` | Funding completes | Extension point (notifications, analytics). |
| `TransactionCompletedEvent` | Transfer / withdrawal completes | Extension point. |
| `WithdrawalFailedEvent` | Paystack transfer initiation fails | `WithdrawalRetryListener` retries with backoff (max 3), then refunds. |
| `TransactionReversedEvent` | Refund/reversal completes | Extension point. |

---

## Data model (PostgreSQL)

Flyway migrations under `src/main/resources/db/migration/`:

| Migration | Tables / purpose |
|-----------|------------------|
| V1 | `users` |
| V2 | `wallets` |
| V3 | `ledger_accounts`, `journal_entries`, `ledger_entries` |
| V4 | `transactions` |
| V5 | `audit_log` |
| V7 | `roles` (RBAC) |
| V8+ | Reversal fields, optimistic locking (`version`) on transactions |

**Ledger invariant:** every `journal_entry` has lines whose debits and credits sum to the same amount; `LedgerService` rejects unbalanced posts.

**System account:** `PAYSTACK_LIABILITY` represents funds in transit at Paystack (funding in, withdrawals out).

---

## API surface (overview)

Base path: `/api/v1`. Protected routes require `Authorization: Bearer <access_token>` unless noted.

| Area | Endpoints (examples) |
|------|----------------------|
| **Auth** | `POST /auth/register`, `/login`, `/refresh`, `/logout` |
| **Users** | `GET/PUT /users/me`, `POST /users/me/transaction-pin` |
| **Wallets** | `POST /wallets`, `GET /wallets`, `GET /wallets/{id}`, `GET /wallets/{id}/balance` |
| **Transactions** | `POST /transactions/transfer`, `/fund`, `/withdraw`; `GET /transactions/{reference}`, `/wallet/{walletId}` |
| **Paystack** | `POST /paystack/webhook` (unsigned from Paystack; signature header required) |

Interactive docs when running: [Swagger UI](http://localhost:8080/swagger-ui.html) · [OpenAPI JSON](http://localhost:8080/api-docs).

---

## Resilience and safety

| Mechanism | Purpose |
|-----------|---------|
| **DB transactions** | Transfer, fund completion, withdraw, and refunds run in `@Transactional` boundaries so ledger + transaction rows stay aligned. |
| **Idempotency keys** | `transfer`, `fund`, `withdraw` accept a client key; duplicates return the original transaction. |
| **Pessimistic locks** | Wallet/ledger accounts locked during balance-changing operations. |
| **Per-wallet transaction lock** | Prevents overlapping withdraw/transfer on the same wallet. |
| **Transfer throttle (Redis)** | Blocks identical amount + recipient retries within 30 seconds. |
| **Paystack resilience** | Timeouts, retries, circuit breaker on outbound HTTP (`ResilienceConfig`). |
| **Webhook verification** | HMAC signature check before any state change; funding/withdrawal handlers are idempotent by reference. |
| **Withdrawal retries** | Async listener re-attempts failed Paystack transfers; auto-reversal after max attempts. |
| **Structured errors** | `GlobalExceptionHandler` maps domain exceptions to consistent `ApiResponse` JSON. |

---

## Scalability notes

- **Stateless API tier:** JWT + Redis refresh tokens; no sticky sessions required.
- **Database as source of truth:** All balances ultimately derive from ledger accounts in PostgreSQL.
- **Redis for ephemeral state:** Safe to lose Redis without losing money (tokens re-login, cache repopulates); do not store balances only in Redis.
- **Module boundaries:** Packages are organized for a possible future split (e.g. ledger service) but today deploy as one artifact.

---

## Project structure

```
src/main/java/com/vaultpay/
├── auth/           # JWT, register/login, Redis refresh tokens
├── user/           # Profile, transaction PIN
├── wallet/         # Wallet lifecycle, balance cache
├── ledger/         # Double-entry accounting core
├── transaction/    # Transfers, deposits, withdrawals, refunds, listeners
├── paystack/       # Gateway client + webhook entrypoint
└── common/         # Config, exceptions, events, utilities, audit

src/main/resources/
├── application.yml
└── db/migration/   # Flyway SQL
```

---

## Getting started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose (recommended: app + Postgres + Redis)

### Run with Docker Compose

```bash
cp .env.example .env
# Set PAYSTACK_SECRET_KEY and JWT_SECRET in .env
docker-compose up -d
```

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Postgres: `localhost:5432` (db `vaultpay`)
- Redis: `localhost:6379`

### Run locally (without Docker for the app)

1. Start PostgreSQL on port 5432 (database `vaultpay`) and Redis on `6379`.
2. Configure environment:

```bash
cp .env.example .env
```

3. Build and run:

```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Configuration (`.env` / environment)

| Variable | Purpose |
|----------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | Signing key for access tokens (use a strong secret in production) |
| `PAYSTACK_SECRET_KEY` | Paystack API + webhook HMAC verification |
| `REDIS_HOST`, `REDIS_PORT` | Redis (set in Compose / `application.yml` for dev) |
| `SERVER_PORT`, `SPRING_PROFILES_ACTIVE` | Server binding and profile |

---

## License

Proprietary — All rights reserved.
