# Business Operations Mini Platform

A portfolio-grade Spring Boot MVP for a common small-business operations problem: purchasing, receiving, inventory, sales fulfillment, and audit history often live in disconnected spreadsheets or loosely coupled tools.

This project demonstrates a bounded business workflow rather than a generic CRUD application.

## What it proves

- Explicit purchase and sales state machines.
- Append-only inventory ledger; on-hand stock is derived, never stored as a mutable balance.
- Transactional state transition + inventory movement + audit writes.
- Stock is re-checked inside the fulfillment transaction.
- Duplicate sales-order lines for the same product are aggregated before the stock check, preventing oversell through line splitting.
- Flyway-owned schema migration and JDBC-based persistence without an ORM.
- Bean Validation, bounded JSON requests, generic API errors, and safe DOM rendering.
- Automated domain, service/API, migration, and browser E2E verification.
- CI and Dependabot configuration for Maven and npm dependencies.

## Business workflow

Purchase:

`DRAFT → SUBMITTED → APPROVED → RECEIVED`

Receiving an approved purchase order writes positive inventory ledger movements.

Sales:

`DRAFT → CONFIRMED → FULFILLED`

Fulfillment recalculates ledger-derived availability and writes negative movements only when the complete order can be fulfilled without driving stock below zero.

Every successful creation or transition creates an audit event.

## Stack

- Java 25 LTS
- Spring Boot 4.1.1
- Spring MVC
- Spring JDBC / `JdbcTemplate`
- Flyway
- H2 file database for the local demo
- Thymeleaf + vanilla JavaScript/CSS
- JUnit / Spring Boot Test / MockMvc
- Playwright 1.63.0
- Maven Wrapper
- GitHub Actions + Dependabot

## Architecture

```text
Browser dashboard
      │
      ▼
Spring MVC / JSON API
      │
      ▼
OperationsService  ── workflow invariants / transactions
      │
      ├── purchase & sales order tables
      ├── append-only inventory_movements
      └── audit_events
      │
      ▼
H2 + Flyway V1 schema
```

More detail: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Quick start

macOS / Linux / GitHub Actions:

```bash
./mvnw -B verify
./mvnw spring-boot:run
```

Windows:

```powershell
mvnw.cmd -B verify
mvnw.cmd spring-boot:run
```

Open `http://127.0.0.1:8080`.

All data is synthetic and local. No marketplace account, supplier account, customer system, credential, or production API is required.


## Configuration

The checked-in local defaults are in `src/main/resources/application.properties`. For a different local/demo environment, standard Spring Boot environment variables can override them, including `SERVER_PORT`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.

Do not commit real credentials. The default repository configuration uses local H2 demonstration storage only.

## Browser validation

Install E2E dependencies once:

```bash
npm ci
npx playwright install chromium
npm run test:e2e
```

The Playwright configuration defaults to `127.0.0.1`. A restricted local environment can set `E2E_HOST` and `E2E_PORT` to another reachable local interface without changing repository code. GitHub Actions uses the default loopback path.

## Verified local evidence

Current local closeout evidence includes:

- Maven build: PASS
- Automated tests: 10 / 10 PASS
- Flyway fresh-schema bootstrap: PASS
- Duplicate-receive / duplicate-fulfillment protection: PASS
- Duplicate-product-line oversell regression: PASS
- HTTP 400 / 409 / 413 behavior: PASS
- Stable camelCase JSON contract regression: PASS
- npm audit: 0 known vulnerabilities
- Playwright business flow: 1 / 1 PASS

The browser flow proves:

`create product → PO qty 10 → submit → approve → receive → on-hand 10 → SO qty 4 → confirm → fulfill → on-hand 6 → audit visible`

## API surface

- `GET /api/health`
- `GET /api/products`
- `POST /api/products`
- `GET /api/purchase-orders`
- `POST /api/purchase-orders`
- `POST /api/purchase-orders/{id}/submit`
- `POST /api/purchase-orders/{id}/approve`
- `POST /api/purchase-orders/{id}/receive`
- `GET /api/sales-orders`
- `POST /api/sales-orders`
- `POST /api/sales-orders/{id}/confirm`
- `POST /api/sales-orders/{id}/fulfill`
- `GET /api/inventory`
- `GET /api/audit`

## Design boundaries

This is a portfolio MVP, not production-ready ERP software. It intentionally does not include authentication/authorization, tenancy, reservation/allocation, distributed locking, accounting compliance, cloud deployment, or external supplier/customer integrations.

See:
- [Workflow rules](docs/WORKFLOW_RULES.md)
- [Security and limitations](docs/SECURITY_AND_LIMITATIONS.md)

## License

MIT.
## Handoff and support

- [Demo deployment and handoff](DEPLOYMENT.md)
- [Support](SUPPORT.md)
- [Security and limitations](docs/SECURITY_AND_LIMITATIONS.md)

