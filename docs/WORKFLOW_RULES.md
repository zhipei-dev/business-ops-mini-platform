# Workflow Rules

## Purchase orders

State machine:

```text
DRAFT → SUBMITTED → APPROVED → RECEIVED
```

Rules:

- only `DRAFT` can be submitted;
- only `SUBMITTED` can be approved;
- only `APPROVED` can be received;
- `RECEIVED` is terminal in the MVP;
- receipt can happen only once;
- a successful receipt appends positive inventory movements;
- every successful creation/transition writes an audit event;
- an illegal transition returns HTTP 409 and does not write a successful ledger/audit event.

## Sales orders

State machine:

```text
DRAFT → CONFIRMED → FULFILLED
```

Rules:

- only `DRAFT` can be confirmed;
- only `CONFIRMED` can be fulfilled;
- `FULFILLED` is terminal in the MVP;
- fulfillment can happen only once;
- all lines are grouped by product before the stock check;
- ledger-derived on-hand stock is recalculated inside the transaction;
- if any aggregated product quantity is insufficient, fulfillment fails with HTTP 409;
- failed fulfillment does not append successful inventory movements or audit events;
- successful fulfillment appends negative inventory movements.

## Inventory

Inventory is a derived value:

```text
on hand = SUM(all movement deltas for the product)
```

No mutable balance column exists.

Movement examples:

- purchase receipt: `+10`
- sales fulfillment: `-4`
- resulting on-hand: `6`

## Validation boundaries

- IDs and quantities must be positive.
- Monetary values use `BigDecimal` and are limited to two fractional digits in request validation.
- Text and list sizes are bounded.
- Declared JSON bodies above 64 KiB are rejected with HTTP 413.
- Not-found resources return 404.
- Illegal workflow transitions and insufficient inventory return 409.
- Invalid/malformed request data returns generic 400 responses.

## Concurrency limitation

The MVP recalculates inventory inside a transaction, but it does not implement reservation, row-level locking strategy, or distributed coordination. A production multi-worker system must add an appropriate concurrency-control model before treating the non-negative-stock invariant as safe under concurrent fulfillment.
