# Security and limitations

This is a synthetic local portfolio demo. It has no real suppliers, marketplaces, accounts, credentials, outbound integrations, authentication, or authorization.

## Input and API boundaries

- Bean Validation constrains identifiers, quantities, text lengths, item counts, and monetary scale.
- JSON requests with a declared Content-Length above 64 KiB are rejected with HTTP 413 before controller binding.
- Malformed JSON and method/path validation failures return generic 4xx responses.
- Unexpected server errors return a generic 500 response without stack traces, paths, or configuration details.
- The browser UI renders user-provided values with DOM text nodes rather than `innerHTML`.

The JSON size guard is an application-level demo control. A production deployment should also enforce request limits at the reverse proxy/gateway and account for streaming or chunked bodies.

## Data and workflow boundaries

- H2 is used only as local demonstration storage; the H2 web console is not enabled.
- Inventory is derived from an append-only movement ledger.
- Sales fulfillment aggregates required quantity by product before checking on-hand inventory, so duplicate product lines cannot bypass the non-negative-stock invariant.
- State transition, inventory movement, and audit writes execute in Spring transactions.
- A production inventory system still needs stronger concurrency control (for example row locking, reservation/allocation, or equivalent isolation) when multiple fulfillment transactions can race.

## Non-production limitations

Production use would additionally require authentication, authorization, tenant isolation, durable backups, migration governance, rate limiting, observability, concurrency hardening, secure secrets handling, and operational monitoring.
