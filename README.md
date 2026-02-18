# VaultPay Neobank

Production-ready digital banking backend with double-entry ledger, user wallets, and Paystack payment integration.

## Tech Stack

- Java 17 + Spring Boot 3.2
- PostgreSQL 16
- Flyway (database migrations)
- Spring Security + JWT (authentication)
- Paystack API (payment gateway)
- Springdoc OpenAPI (Swagger docs)
- Docker + Docker Compose

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose (for local PostgreSQL)

### Run with Docker Compose

```bash
cp .env.example .env
docker-compose up -d
```

The app starts at `http://localhost:8080` and Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Run Locally (without Docker)

1. Start a PostgreSQL instance on port 5432 with database `vaultpay`.
2. Copy and configure environment variables:

```bash
cp .env.example .env
```

3. Build and run:

```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Architecture

For module boundaries, communication flows, resilience, and scalability, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Project Structure

```
src/main/java/com/vaultpay/
├── auth/           Authentication & JWT
├── user/           User management & KYC
├── wallet/         Wallet lifecycle & balance
├── ledger/         Double-entry ledger (accounting core)
├── transaction/    Transfers, deposits, withdrawals
├── paystack/       Paystack gateway integration
└── common/         Shared config, exceptions, utilities
```

## API Documentation

Once the app is running, visit:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## License

Proprietary - All rights reserved.
