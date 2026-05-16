# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Source Of Truth

When Markdown docs conflict with implementation, trust the current code first:

- Root `pom.xml` for module list and dependency versions.
- Each module's `application.yml` for service names, ports, middleware addresses, and gateway routes.
- Controllers and Feign clients for API surface.
- `scripts/ci/e2e.sh` for the currently verified full-chain behavior.
- `doc/sql/` for schema phases and seeded tables.

## Build And Run

```bash
# Build all modules
mvn clean package -DskipTests

# Build one module and its dependencies
mvn clean package -pl velocity-mall-order -am -DskipTests

# Run one service from the repository root
mvn spring-boot:run -pl velocity-mall-gateway

# Run a packaged service
java -jar velocity-mall-gateway/target/velocity-mall-gateway-1.0.0-SNAPSHOT.jar

# Full-chain E2E
bash scripts/ci/e2e.sh
```

All bootable modules except `velocity-mall-common` use `spring-boot-maven-plugin`.

## Architecture Overview

VelocityMall is a Spring Cloud Alibaba ecommerce microservice project using one logical MySQL database, `velocity_mall`, separated by table prefixes:

- `pms_`: product and inventory.
- `oms_`: orders, order items, reviews, review interactions.
- `ums_`: users, addresses, administrators.
- `sms_`: coupons and coupon histories.

### Modules And Ports

| Module | Port | Responsibility |
| --- | ---: | --- |
| `velocity-mall-common` | -- | Shared base entities, DTO/VO, result model, exceptions, Redis/MyBatis/Feign/trace config, user context |
| `velocity-mall-gateway` | 8080 | Spring Cloud Gateway, JWT auth, user/admin header injection, internal route blocking, Sentinel gateway rules |
| `velocity-mall-product` | 8081 | Category tree, SPU/SKU query, cache protection, stock lock/release/deduct/refund, product sync MQ |
| `velocity-mall-order` | 8082 | Cart, normal order, payment/refund mock, delayed close, seckill order persistence, delivery, receipt confirmation |
| `velocity-mall-seckill` | 8083 | Redis Lua seckill, duplicate blocking, MQ async order, Redis occupation rollback |
| `velocity-mall-search` | 8085 | Elasticsearch SKU search, index rebuild, product sync consumer |
| `velocity-mall-coupon` | 8086 | Coupon claim, per-user limit, duplicate protection, stock deduction |
| `velocity-mall-review` | 8087 | Product review CRUD, stats cache, like/dislike switching, purchase eligibility check |
| `velocity-mall-user` | 8088 | User registration/login/JWT, current user info, shipping addresses |
| `velocity-mall-admin` | 8089 | Admin login, order delivery, SPU publish/unpublish |

External dependencies: Nacos `127.0.0.1:8848`, MySQL 8, Redis 7.2, RocketMQ 4.9.4, Elasticsearch 8.10.4, Sentinel Dashboard `127.0.0.1:8858`, optional MinIO `127.0.0.1:9000`.

## Current Capability Baseline

The implemented baseline covers Phase 1-16 and Phase 18-21. Phase 8/9 and Phase 17 are historical numbering gaps, not current completion boundaries.

- Phase 10: mock payment callback and physical stock deduction.
- Phase 11: Redis cart and normal-order physical stock lock flow.
- Phase 12: C-end order list/detail/cancel/refund.
- Phase 13: Elasticsearch search and product sync.
- Phase 14: category tree with Redis/Redisson cache protection.
- Phase 15: coupon claim with Redis pre-limit and MySQL optimistic stock deduction.
- Phase 16: reviews, stats cache, like/dislike interaction.
- Phase 18: user registration/login/JWT.
- Phase 19: shipping address snapshot for order creation.
- Phase 20: order delivery and receipt confirmation.
- Phase 21: admin login plus delivery and SPU status operations.

## Auth And Gateway Rules

Gateway `AuthGlobalFilter` is the auth boundary.

- Anonymous POST whitelist: `/api/v1/users/register`, `/api/v1/users/login`, `/api/v1/admin/login`.
- Anonymous GET whitelist: `/api/v1/products/spus/**`, numeric `/api/v1/products/skus/{sku-id}`, `/api/v1/search/**`, `/api/v1/categories/tree`, `/api/v1/reviews/products/**`.
- Normal user JWT must contain `userId`; Gateway injects `X-User-Id`.
- Admin JWT must contain `adminId`; Gateway injects `X-Admin-Id` for `/api/v1/admin/**`.
- Gateway blocks external access to `/api/v1/products/inner/**`, `/api/v1/search/inner/**`, `/api/v1/orders/inner/**`, `/api/v1/users/inner/**`, and legacy product stock lock/unlock endpoints.

## Core Patterns

### Inventory

- Normal orders use Product service MySQL conditional updates to lock stock: `stock - lock_stock >= quantity`.
- `pms_stock_lock_log` records normal-order stock lifecycle.
- Payment success deducts physical stock and locked stock for normal orders.
- Refund restores physical stock and decreases sale count.
- Seckill uses Redis Lua to atomically check duplicate purchase, deduct Redis stock, and record user occupation before sending `seckill-order-topic`.

### RocketMQ

| Topic | Producer | Consumer | Purpose |
| --- | --- | --- | --- |
| `normal-order-delay-topic` | order | order | Normal order timeout close |
| `velocity-mall-order-delay-topic` | order | order | Legacy order delayed close |
| `payment-success-topic` | order | product | Payment success physical stock deduction |
| `order-refund-topic` | order | product | Refund stock and sale-count rollback |
| `seckill-order-topic` | seckill | order | Async seckill order creation |
| `seckill-delay-topic` | order | order | Seckill order timeout close |
| `seckill-rollback-topic` | order | seckill | Redis seckill stock rollback |
| `product-sync-topic` | product | search | SKU/SPU updates to Elasticsearch |

Product-side MQ idempotency uses `mq_consume_log` with unique `topic + consumer_group + order_sn`.

### Trace

`X-Trace-Id` is generated by Gateway, loaded into MDC by servlet services, propagated through OpenFeign, and copied into RocketMQ messages through `MqTraceContext`.

## E2E Coverage

`scripts/ci/e2e.sh` currently starts the full application chain and verifies:

- User register/login and JWT acquisition.
- User address creation and order address snapshot.
- Admin login.
- Category, SPU, SKU public reads.
- Coupon auth, claim, duplicate claim behavior.
- Product/search/order/user internal API blocking through Gateway.
- Cart add/list.
- Normal order create, payment, stock deduction, MQ idempotency.
- Admin delivery and C-end receipt confirmation.
- Review create/list/like/dislike toggle/stats/delete.
- Refund flow and product stock rollback.
- Seckill Redis Lua path and async order persistence.
- Search index rebuild and public search.

## Coding Conventions

- Controller returns `Result<T>` and delegates business logic to Service.
- External API responses must not expose Entity directly.
- Entities inherit `BaseEntity`; concurrent state/inventory entities inherit `VersionedEntity`.
- Business errors use `BusinessException` and `ResultCode`.
- Internal service APIs live under `/inner/**` and must stay blocked by Gateway.
- Redis keys should stay centralized in constants/helpers.
- Prefer conditional SQL updates and explicit idempotency for state transitions.
- Run at least `mvn clean package -DskipTests` after structural code changes.
