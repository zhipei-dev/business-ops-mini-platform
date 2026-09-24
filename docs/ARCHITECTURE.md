# Architecture

## Purpose

The application is a single-process Spring MVC business-system MVP. The design keeps the runtime deliberately small so the important engineering choices are visible: workflow invariants, transaction boundaries, inventory accounting, auditability, migrations, and API behavior.

## Runtime structure

```text
Thymeleaf page + vanilla JS
           │
           │ fetch JSON
           ▼
OperationsController
           │
           ▼
OperationsService
   │          │          │
   ▼          ▼          ▼
orders   inventory    audit
tables   movements    events
   \          │          /
    \         ▼         /
      JdbcTemplate
          │
          ▼
       H2 database
          ▲
          │
      Flyway V1
```

The application does not use JPA/Hibernate. SQL and transaction boundaries stay explicit through `JdbcTemplate` and Spring transactions.

## Inventory model

`inventory_movements` is append-only.

A purchase receipt adds positive movements. A sales fulfillment adds negative movements. The application never stores a mutable `on_hand` column; current stock is derived with:

```sql
SUM(inventory_movements.quantity_delta)
```

This makes every stock change attributable to a business event and avoids balance/ledger drift inside the MVP.

## Transaction boundaries

Purchase receipt:

1. validate current workflow state;
2. transition to `RECEIVED`;
3. append positive movement rows;
4. append audit event;
5. commit as one transaction.

Sales fulfillment:

1. validate `CONFIRMED` state;
2. aggregate requested quantity by product across all order lines;
3. recalculate ledger-derived on-hand quantity;
4. reject the entire operation if any product is insufficient;
5. append negative movement rows;
6. transition to `FULFILLED`;
7. append audit event;
8. commit as one transaction.

The aggregate-by-product step is important: repeated lines for the same SKU cannot bypass the non-negative-stock invariant.

## API and validation

Controllers expose a bounded JSON API. Bean Validation covers positive identifiers/quantities, text lengths, money scale, and order item counts. A request filter rejects declared JSON bodies larger than 64 KiB before controller binding.

The error layer distinguishes expected business/client errors from unexpected failures without returning stack traces, local paths, or configuration.

## Database lifecycle

Flyway migration `V1__initial_schema.sql` owns the schema. Local runtime uses H2 file storage; automated integration tests use an isolated in-memory H2 database.

H2 is a demonstration choice, not a production database recommendation.

## Browser layer

The dashboard uses safe DOM APIs and `textContent` for values returned from the API. It does not render user values via `innerHTML`.

Playwright launches the application with an isolated in-memory database and verifies the complete purchase-to-sale workflow.

## Production evolution

A production successor would require authentication, authorization, tenant isolation, stronger concurrency control/reservation semantics, a durable production database, migration governance, rate limiting, observability, backups, and deployment controls.
