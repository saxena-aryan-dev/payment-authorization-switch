# Payment Authorization Switch

[![CI](https://github.com/code2946/Payment-Authorization-Switch-/actions/workflows/ci.yml/badge.svg)](https://github.com/code2946/Payment-Authorization-Switch-/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)
![Build](https://img.shields.io/badge/build-Maven-blue)
![Coverage Gate](https://img.shields.io/badge/coverage%20gate-60%25-yellow)

A real-time card **authorization switch** built with **Java 17 + Spring Boot 3**. It accepts
[ISO 8583](https://en.wikipedia.org/wiki/ISO_8583) financial messages, parses and validates them
with a hand-written codec, applies a deterministic set of authorization rules, persists every
transaction to a relational database, and returns an ISO 8583 response — all in a single synchronous
call.

This is the component that sits at the heart of a payment network: an acquirer sends a `0100`
authorization request, the switch decides, and a `0110` response goes back.

```
acquirer ──0100──▶  switch  ──0110──▶  acquirer
                       │
                       ▼
                  PostgreSQL
```

---

## Why this project

Most demo projects are CRUD apps. This one models a real slice of how card payments work end to end:

- **A real ISO 8583 codec** — Message Type Indicator, a 64-bit primary bitmap, and
  `FIXED` / `LLVAR` / `LLLVAR` data elements, encoded and decoded by hand (no black-box library) so
  the wire format is fully transparent and unit-tested.
- **Authorization decisioning** — Luhn (mod-10) PAN validation, expiry checks, per-transaction
  limits, a block-list, and a simple balance ledger, each mapped to a standard ISO **field-39**
  response code.
- **Production hygiene** — PANs are masked before they touch storage, every transaction is persisted
  to a relational schema, and the service ships with a JaCoCo coverage gate, Spotless formatting,
  a multi-stage Docker build, and a GitHub Actions pipeline.

---

## Architecture

```
                     POST /api/v1/authorizations  (raw ISO 8583)
                                   │
                                   ▼
                       ┌────────────────────────┐
   0100 request ─────▶ │  AuthorizationController│
                       └───────────┬────────────┘
                                   ▼
                            Iso8583Codec.decode()
                                   │
                                   ▼
                       ┌────────────────────────┐
                       │  AuthorizationService   │   decisioning
                       └───────────┬────────────┘
              ┌────────────────────┼─────────────────────┐
              ▼                    ▼                      ▼
        LuhnValidator        AccountService       TransactionRepository
        (mod-10 check)     (limits / balance /        (JPA → PostgreSQL,
                              block-list)              PAN masked at rest)
                                   │
                                   ▼
                        field-39 response code
                                   │
                                   ▼
                            Iso8583Codec.encode()
                                   │
   0110 response ◀──────────────── ┘
```

| Layer       | Key types                                                | Responsibility                              |
|-------------|----------------------------------------------------------|---------------------------------------------|
| `web`       | `AuthorizationController`, `GlobalExceptionHandler`, DTOs | REST endpoints, serialization, error mapping |
| `iso8583`   | `Iso8583Codec`, `Iso8583Message`, `Fields`, `FieldSpec`  | Parse / build raw ISO 8583 messages          |
| `service`   | `AuthorizationService`, `LuhnValidator`, `AccountService`, `PanMasking`, `ResponseCode` | Authorization rules and response-code mapping |
| `domain`    | `Transaction`, `TransactionStatus`                       | JPA entity persisted to the RDBMS            |
| `repository`| `TransactionRepository`                                  | Spring Data access layer                     |

---

## Response codes (ISO 8583 field 39)

| Code | Meaning             | Rule                                          |
|------|---------------------|-----------------------------------------------|
| `00` | Approved            | Passes all checks; balance debited            |
| `05` | Do not honor        | PAN is block-listed                           |
| `13` | Invalid amount      | Amount is zero or negative                    |
| `14` | Invalid card        | PAN fails the Luhn checksum                   |
| `51` | Insufficient funds  | Balance below the requested amount            |
| `54` | Expired card        | Field 14 expiry date is in the past           |
| `61` | Exceeds limit       | Amount above the per-transaction limit        |

---

## Quick start

### Run with Docker Compose (app + PostgreSQL)

```bash
docker compose up --build
```

The API is then available on `http://localhost:8080`.

### Send an authorization

The request body is a raw ISO 8583 string (MTI + bitmap + data elements). Here is an **approved**
sample — PAN `4111111111111111`, amount `100.00`, STAN `000777`:

```bash
curl -X POST http://localhost:8080/api/v1/authorizations \
  -H "Content-Type: text/plain" \
  --data '0100724000000000000016411111111111111100000000001000000007770356'
```

Response:

```json
{
  "responseMessage": "0110...",
  "mti": "0110",
  "stan": "000777",
  "responseCode": "00",
  "status": "APPROVED",
  "approvalCode": "8F2K1Q",
  "maskedPan": "************1111",
  "amountMinorUnits": 100000,
  "currency": "356"
}
```

### Endpoints

| Method | Path                          | Description                                  |
|--------|-------------------------------|----------------------------------------------|
| `POST` | `/api/v1/authorizations`      | Authorize a raw ISO 8583 `0100` message      |
| `GET`  | `/api/v1/transactions`        | 50 most recent transactions                  |
| `GET`  | `/api/v1/transactions/{stan}` | Look up by System Trace Audit Number (STAN)  |
| `GET`  | `/actuator/health`            | Liveness probe                               |

---

## Build & test

```bash
mvn clean verify      # compile, run the JUnit suite, enforce the 60% line-coverage gate
mvn spotless:apply    # auto-format sources
```

No local JDK or Maven? Build and test entirely inside a container:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-17 mvn -B clean verify
```

The test suite covers the ISO 8583 codec (round-trip encode/decode), the Luhn validator, the
authorization decisioning rules, and the REST layer via `MockMvc`.

---

## Project layout

```
src/
├── main/java/com/aryansaxena/paymentswitch/
│   ├── PaymentSwitchApplication.java
│   ├── iso8583/        # hand-written ISO 8583 codec + message model
│   ├── service/        # authorization rules, Luhn, PAN masking, response codes
│   ├── domain/         # JPA entities
│   ├── repository/     # Spring Data repositories
│   └── web/            # REST controller, DTOs, exception handling
└── test/java/...       # JUnit 5 + Mockito + AssertJ
```

---

## Design notes

- **Money is stored in minor units** (`long`), never floating point — a non-negotiable in payments.
- **PANs are masked at the boundary** (`************1111`) so full card numbers are never persisted
  or logged.
- **The codec is hand-rolled on purpose.** Reaching for a library would hide the exact part worth
  demonstrating: how an MTI, a bitmap, and variable-length data elements turn into bytes on the wire.
- **Decisioning is deterministic and pure** — the same request always yields the same field-39 code,
  which keeps the rules trivial to unit-test.

---

## Tech stack

Java 17 · Spring Boot 3 (Web, Data JPA, Validation, Actuator) · PostgreSQL · H2 (tests) ·
JUnit 5 · Mockito · AssertJ · JaCoCo · Spotless · Docker · GitHub Actions

---

## License

Released under the [MIT License](LICENSE).
