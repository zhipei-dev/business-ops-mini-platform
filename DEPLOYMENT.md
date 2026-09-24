# Demo deployment and handoff

This repository is a portfolio MVP. These steps are for a local or controlled demonstration handoff, not a production ERP deployment.

## Runtime prerequisites

- Java 25
- the checked-in Maven Wrapper
- Node.js/npm only when running browser E2E validation

## Build and run

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

The default database is a local H2 file under `./data`. It is demonstration storage, not a production persistence design.

## Configuration

The checked-in defaults are in `src/main/resources/application.properties`.

Spring Boot environment variables can override those properties when a different local/demo configuration is needed. Common examples include:

- `SERVER_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Do not commit real credentials. The default local demo intentionally uses a credential-free local H2 setup.

## Browser validation

```bash
npm ci
npx playwright install chromium
npm run test:e2e
```

`E2E_HOST` and `E2E_PORT` are test-only overrides.

## Handoff checklist

Before handing the demo to another developer:

1. run `./mvnw -B verify` or the Windows equivalent
2. run the application and verify the dashboard
3. run browser E2E when required
4. explain that the local H2 file contains demo data only
5. review the workflow, architecture, and security/limitations documents
6. do not represent the project as production-ready ERP software

## Production boundary

Production use would require authentication, authorization, tenancy, backup/restore, migration governance, rate limiting, observability, concurrency hardening, secure secrets handling, and operational monitoring.

See `docs/SECURITY_AND_LIMITATIONS.md`.
