# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# 构建所有模块
mvn clean install -DskipTests

# 构建单个模块
mvn clean install -pl velocity-mall-common -DskipTests

# 运行单个服务（在对应模块目录下）
mvn spring-boot:run -pl velocity-mall-gateway

# 或直接运行 JAR
java -jar velocity-mall-gateway/target/velocity-mall-gateway-1.0.0-SNAPSHOT.jar
```

所有 bootable 模块（除 `velocity-mall-common` 外）都包含 `spring-boot-maven-plugin`，可独立运行。

## 架构概览

VelocityMall 是一个基于 Spring Cloud Alibaba 的秒杀电商微服务项目，单数据库 `velocity_mall`，按表前缀逻辑隔离（`pms_` 商品、`oms_` 订单、`ums_` 用户、`sms_` 营销）。

### 模块与端口

| 模块 | 端口 | 职责 |
|------|------|------|
| `velocity-mall-common` | -- | 共享基座（实体、VO/DTO、异常、拦截器、配置） |
| `velocity-mall-gateway` | 8080 | Spring Cloud Gateway 入口 + JWT 鉴权 + Sentinel 限流 |
| `velocity-mall-product` | 8081 | 商品/分类/库存管理 |
| `velocity-mall-order` | 8082 | 订单/购物车 |
| `velocity-mall-seckill` | 8083 | 秒杀（无数据库，纯 Redis Lua + MQ） |
| `velocity-mall-search` | 8085 | Elasticsearch 商品搜索 |
| `velocity-mall-coupon` | 8086 | 优惠券 |
| `velocity-mall-review` | 8087 | 商品评价 |
| `velocity-mall-user` | 8088 | 用户注册/登录/JWT + 收货地址 |
| `velocity-mall-admin` | 8089 | 后台管理 |

外部依赖：Nacos (`127.0.0.1:8848`)、MySQL 8、Redis (Redisson)、RocketMQ、Elasticsearch、Sentinel Dashboard (`127.0.0.1:8858`)、MinIO (`127.0.0.1:9000`)。

### 关键中间件版本

Java 17, Spring Boot 3.2.4, Spring Cloud 2023.0.1, Spring Cloud Alibaba 2023.0.1.0, MyBatis-Plus 3.5.7, RocketMQ Starter 2.3.0, Redisson 3.27.2, JJWT 0.11.5。

## 核心架构模式

### 鉴权链路

1. Gateway `AuthGlobalFilter` 校验 JWT Token，提取 `userId`（普通用户）或 `adminId`（管理员）→ 写入 `X-User-Id` / `X-Admin-Id` Header 传递给下游
2. 各服务的 `UserInterceptor` 或 `AdminInterceptor` 读取 Header → 写入 `UserContext` / `AdminContext` ThreadLocal
3. 白名单：所有 `GET` 请求 + `/api/v1/users/register` 和 `/api/v1/users/login` 无需 Token
4. 黑名单：`/inner/**` 路径在网关层禁止外部请求，仅服务间 Feign / 内部 HTTP 调用

### 库存管理

- **普通订单**：两阶段锁定。OrderService 通过 Feign 调用 ProductService 锁库存 → 写入 `pms_stock_lock_log` 记录 → 支付成功后 deductPhysicalStock → 超时/退款时 unlockPhysicalStock
- **秒杀订单**：Redis Lua 脚本原子预扣（`velocitymall:seckill:stock:{skuId}`）→ 异步 MQ 创建订单 → 5 分钟超时 MQ 自动回滚（Lua COMPENSATE/ROLLBACK）
- 库存 SQL 使用 `UPDATE ... SET stock = stock - ? WHERE stock >= ?` 做悲观锁
- 实体层通过 `version` 字段 + MyBatis-Plus 乐观锁插件保证并发安全
- 解锁操作通过 Redis key 做幂等（same `orderSn` won't unlock twice）

### RocketMQ 消息流

| Topic | 生产者 | 消费者 | 用途 |
|-------|--------|--------|------|
| `seckill-order-topic` | seckill | order | 秒杀后异步创建订单 |
| `seckill-delay-topic` | order | order（延时5分钟） | 秒杀订单超时关单 |
| `seckill-rollback-topic` | order | seckill | 秒杀库存回滚 |
| `normal-order-delay-topic` | order | order（延时30分钟） | 普通订单超时关单 |
| `payment-success-topic` | order | product | 支付完成 → 扣减物理库存 |
| `order-refund-topic` | order | product | 退款 → 退还库存 |
| `product-sync-topic` | product | search（顺序消费） | SKU 变更 → 同步 ES 索引 |
| `velocity-mall-order-delay-topic` | order | order | 旧版订单延时关闭 |

MQ 消费幂等通过 `mq_consume_log` 表（唯一键 `topic + consumer_group + order_sn`）保证。

### 分布式追踪

`X-Trace-Id` Header 在 Gateway 生成 → Feign RequestInterceptor 透传 → RocketMQ `MqTraceContext` 透传 → MDC 记录日志。

## 秒杀链路

```
用户请求 → Gateway (Sentinel 5 QPS/SKU 限流)
  → seckill-service (Redis Lua: 检查重复 + 扣库存)
    → RocketMQ (seckill-order-topic)
      → order-service (异步创建订单 + 发送延时关闭消息)
        → [超时触发] order-service (关闭订单 + 发送回滚消息)
          → seckill-service (Redis Lua: 回滚库存)
```

`SeckillSkuScheduledTask` 每分钟从 DB 预热秒杀库存到 Redis。

## 通用编码约定

- 实体继承 `BaseEntity`，提供 `id`(雪花ID)、`createTime`、`updateTime`、`isDeleted`（逻辑删除）
- 并发冲突实体继承 `VersionedEntity`，额外提供 `version` 字段
- Controller 返回统一使用 `Result<T>` 包装，成功用 `Result.success(data)`，失败由 `BusinessException` + `GlobalExceptionHandler` 统一处理
- 服务间内部 API 放在 `/inner/**` 路径下（如 `/api/v1/products/inner/sku/{id}`），这些路径在网关层被阻止外部访问
- 数据库 DDL 在 `doc/sql/` 目录下按阶段编号
