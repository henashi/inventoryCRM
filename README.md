# Inventory CRM

[![CI](https://github.com/henashi/inventoryCRM/actions/workflows/ci.yml/badge.svg)](https://github.com/henashi/inventoryCRM/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Enterprise Inventory Management + Customer Relationship Management system** — a full-stack solution for small to medium businesses.

> 📖 [中文文档](README.zh-CN.md)

---

## Tech Stack

**Backend** | Spring Boot 4.0 / Java 17 / Spring Security + JWT / Spring Data JPA / Hibernate / MySQL / MapStruct / Druid
**Frontend** | Vue 3.5 / TypeScript / Pinia / Ant Design Vue 4 / ECharts
**Infra** | Docker / docker-compose / GitHub Actions CI/CD / Nacos / Swagger UI

## Features

### Inventory Management
- **Stock operations**: IN / OUT / ADJUST with full audit trail
- **Inventory logs**: every stock change is recorded with before/after snapshots
- **Low-stock alerts**: threshold-based warnings (DANGER / WARNING)
- **CSV import/export**: bulk product operations

### Customer Management
- **Full CRUD**: search, filter, pagination
- **Batch status update**: enable/disable customers in bulk
- **CSV import/export**: with row-level validation and duplicate detection
- **Referral tracking**: customer-to-customer referral tree

### Order Management
- **Order lifecycle**: create, delete, discount application
- **Multi-item orders**: per-item pricing and quantity
- **Order audit**: time-stamped with full history

### AI Features
- **Stock prediction**: OLS linear regression — predicts days until stockout by product
- **Customer scoring**: 6-dimension weighted scoring with radar charts
- **Gift recommendation**: score-based Top-3 matching + birthday auto-gifting
- **AI assistant**: LLM-powered natural language query (intent → query → answer) with SSE streaming

### Security
- **JWT authentication**: access token (24h) + refresh token (7d)
- **RBAC**: 3 roles (ADMIN / MANAGER / USER) at URL + method granularity
- **Soft delete**: all entities via `@SQLDelete` + `@SQLRestriction`
- **Security headers**: CSP, HSTS, X-Frame-Options, X-Content-Type-Options
- **Audit logging**: AOP-based operation log for every business action

### DevOps
- **Docker**: multi-stage backend build + nginx frontend serving
- **docker-compose**: MySQL + backend + frontend one-command start
- **CI/CD**: GitHub Actions — build → test → Docker → integration test → ghcr.io publish

## Quick Start

### Prerequisites

- Docker & docker-compose
- Java 17+ (for local development)
- Node.js 20+ (for frontend development)

### Docker (Recommended)

```bash
# Clone and start
git clone https://github.com/henashi/inventoryCRM.git
cd inventoryCRM

# Start all services
docker-compose up -d

# Access
# Frontend: http://localhost:8081
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

Default credentials: `admin` / `admin123`

### Local Development

**Backend:**

```bash
# Ensure MySQL is running (port 3306)
# Create database 'inventorycrm'
# Configure credentials in application-dev.yml

./mvnw spring-boot:run -Dspring.profiles.active=dev
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev  # starts on port 8081
```

## API Documentation

Swagger UI is available at `/swagger-ui.html` when the backend is running.

Key endpoint groups:

| Prefix | Module |
|--------|--------|
| `/api/auth/**` | Authentication |
| `/api/products/**` | Product management |
| `/api/customers/**` | Customer management |
| `/api/inventories/**` | Inventory operations |
| `/api/inventory-logs/**` | Inventory audit log |
| `/api/orders/**` | Order management |
| `/api/gifts/**` | Gift management |
| `/api/gift-logs/**` | Gift distribution log |
| `/api/data-dicts/**` | Data dictionary |
| `/api/operation-logs/**` | Operation audit log |
| `/api/ai/**` | AI features (predictions, scoring, recommendations, chat) |

## Testing

```bash
# Backend tests
./mvnw test

# Frontend unit tests
cd frontend && npm test

# E2E tests (requires backend + frontend running)
cd frontend && npm run e2e
```

Test coverage: **413 backend tests** (0 failures) + **181 frontend tests** (0 failures).

## Project Structure

```
├── src/main/java/com/henashi/inventorycrm/
│   ├── ai/              # AI: LLM service, NL query agent, scoring, prediction
│   ├── aspect/          # AOP: audit logging, inventory log
│   ├── config/          # Spring Security, CORS, Nacos
│   ├── controller/      # REST controllers
│   ├── dto/             # Request/response DTOs
│   ├── exception/       # Custom exceptions
│   ├── handler/         # Global exception handler
│   ├── mapper/          # MapStruct DTO-Entity mappers
│   ├── pojo/            # JPA entities
│   ├── repository/      # Spring Data repositories
│   └── service/         # Business logic
├── frontend/src/
│   ├── api/             # Axios API wrappers
│   ├── components/      # Reusable Vue components
│   ├── layouts/         # App layout
│   ├── router/          # Vue Router + access control
│   ├── stores/          # Pinia state management
│   ├── types/           # TypeScript type definitions
│   ├── utils/           # Utility functions
│   └── views/           # Page components
├── docker-compose.yml
├── Dockerfile
└── .github/workflows/ci.yml
```

## Screenshots

> *(Coming soon — view the [live demo](#) for a walkthrough)*

## License

[MIT](LICENSE) © 2026 henashi
