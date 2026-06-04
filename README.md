# Simple Budget Management

Simple REST API for managing personal budget accounts and transactions.

## Requirements

- Java 17+
- Docker Desktop

## Run on Windows

```powershell
.\run.ps1 -Port 8080
```

## Run on Linux

```bash
chmod +x run.sh
./run.sh 8080
```

The application will be available at:

```text
http://localhost:{port}
```

## API documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```


## Main endpoints

### Accounts

```text
GET    /v1/accounts
POST   /v1/accounts/create
GET    /v1/accounts/{accountName}
DELETE /v1/accounts/{accountName}
```

### Transactions

```text
GET    /v1/transactions/{accountName}
POST   /v1/transactions/{accountName}
DELETE /v1/transactions/{accountName}/{uuid}
```

Transaction filters:

```text
GET /v1/transactions/{accountName}?from=2026-06-01&to=2026-06-30&category=Food
```

CSV export:

```text
GET /v1/transactions/{accountName}/export
GET /v1/transactions/{accountName}/export?from=2026-06-01&to=2026-06-30&category=Food
```

### Statistics

```text
GET /v1/stats/{accountName}
```
