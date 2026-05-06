# VelocityMall 当前实现状态与接口功能文档

> 用途：本文档面向后续 AI 协作或外部规划工具（例如 Gemini），用于快速理解 VelocityMall 当前已经实现到什么阶段、有哪些可用接口、哪些能力已经具备、哪些仍属于规划。  
> 基准文档：`DEVELOPMENT_SPEC.md`。  
> 当前代码结构：Spring Boot 3.2.x + Spring Cloud 2023.0.x + Spring Cloud Alibaba 2023.0.x + MyBatis-Plus + MySQL + Redis + RocketMQ + Nacos + Elasticsearch + Spring Cloud Gateway。

## 1. 项目定位

VelocityMall 是一个模拟真实中型垂直电商场景的分布式微服务商城系统。当前定位是：

- 常规业务链路按千级 QPS 进行设计。
- 核心读链路与秒杀入口可按数千级 QPS 进行压测演示。
- 交易写链路优先保证稳定性、一致性、幂等和可补偿。
- 项目目标不是堆砌中间件，而是展示企业级代码治理、分层设计、交易一致性、缓存治理、消息可靠性和高并发秒杀链路。

## 2. 当前模块划分

| 模块 | 服务名 | 职责 | 默认端口 |
| --- | --- | --- | --- |
| `velocity-mall-common` | 无独立服务 | 公共响应、异常、实体基类、Redis/MyBatis 配置、跨服务 DTO/VO、用户上下文 | 无 |
| `velocity-mall-gateway` | `velocity-mall-gateway` | 统一入口、JWT 鉴权、内部接口拦截、Sentinel 秒杀限流、路由转发 | 8080 |
| `velocity-mall-product` | `velocity-mall-product` | 商品分类、SPU/SKU 查询、商品缓存、库存锁定/释放/扣减、商品同步消息 | 8081 |
| `velocity-mall-order` | `velocity-mall-order` | 购物车、普通订单、订单查询、支付 mock、退款 mock、延时关单、秒杀订单落库 | 8082 |
| `velocity-mall-seckill` | `velocity-mall-seckill` | 秒杀库存预热、Redis Lua 抢购、防重复抢购、MQ 异步下单、Redis 占位回滚 | 8083 |
| `velocity-mall-search` | `velocity-mall-search` | Elasticsearch 商品搜索、索引重建、商品同步消费 | 8085 |
| `velocity-mall-coupon` | `velocity-mall-coupon` | 优惠券领取、领取记录、限领、防重复领取 | 8086 |
| `velocity-mall-review` | `velocity-mall-review` | 商品评价发布/分页/统计/删除、点赞/点踩互动体系、购买资格校验 | 8087 |

## 3. Roadmap 完成度与当前阶段判断

重要说明：

- 当前代码主干能力已经覆盖 `Phase 1` 到 `Phase 7` 的核心内容，并且已经补齐 `Phase 10` 到 `Phase 16`。
- `Phase 8/9` 当前未在代码主干中体现为独立落地模块或增量 SQL，可视为历史预留或已被后续规划覆盖。
- 旧文档曾将”用户评价与互动体系”写作 `Phase 8`，但该功能已在 `Phase 16` 以独立 `velocity-mall-review` 服务完整实现。
- 后续规划不应再判断”项目只到 Phase 8”，而应以当前已实现的 Phase 10-16 为基线。

| Phase | 目标 | 当前状态 |
| --- | --- | --- |
| Phase 1 工程骨架 | Maven 父子工程、公共模块、商品、订单、网关 | 已实现 |
| Phase 2 数据库建模与领域层 | 核心表、实体、Mapper、MyBatis-Plus | 已实现基础表与核心实体 |
| Phase 3 基础业务 API | 商品查询、订单创建/查询、DTO/VO、统一异常 | 已实现 |
| Phase 4 缓存治理 | Redis 缓存、TTL 随机、空对象、锁保护 | 已部分实现，商品/分类/购物车/秒杀/优惠券使用 Redis |
| Phase 5 订单异步化与消息可靠性 | RocketMQ、延时关单、支付成功、退款、幂等 | 已实现核心链路 |
| Phase 6 网关与鉴权 | Gateway、JWT、白名单、黑名单、用户上下文 | 已实现 |
| Phase 7 秒杀链路 | Redis Lua、库存预热、防重、MQ 削峰 | 已实现核心链路 |
| Phase 8/9 历史预留 | 当前主干未发现独立 Phase 8/9 落地模块 | 不作为当前项目完成度终点 |
| Phase 10 模拟支付闭环 | Mock 支付、支付成功 MQ、Product 真实库存扣减、消费幂等 | 已实现 |
| Phase 11 购物车与普通订单锁库 | Redis 购物车、普通订单创建、批量锁库、锁流水、延时释放 | 已实现 |
| Phase 12 C 端订单管理与退款 | 订单列表/详情/取消、Mock 退款、退款 MQ、库存销量回滚 | 已实现 |
| Phase 13 商品搜索与异构同步 | Search 服务、ES 索引、Product MQ 同步、索引重建 | 已实现 |
| Phase 14 分类树与缓存防线 | 分类树接口、Redis JSON 缓存、Redisson 锁、Double Check、TTL 抖动 | 已实现 |
| Phase 15 优惠券与高并发抢券 | Coupon 服务、领券流水、Redis 限领、MySQL 乐观锁扣库存 | 已实现 |
| Phase 16 用户评价与互动 | 发评/删评、商品评价分页、评价统计、点赞/点踩/切换/取消、购买资格校验、统计缓存 | 已实现 |

## 4. 通用接口约定

### 4.1 响应体

所有业务接口统一返回：

```json
{
  "code": 20000,
  "message": "操作成功",
  "data": {}
}
```

常用响应码：

| code | 含义 |
| --- | --- |
| `20000` | 操作成功 |
| `40000` | 请求参数错误 |
| `40100` | 未认证或登录失效 |
| `40300` | 无访问权限 |
| `50000` | 系统异常 |
| `50001` | 业务处理失败或业务警告 |

### 4.2 鉴权规则

统一入口为 Gateway：`http://127.0.0.1:8080`。

需要登录的接口必须携带：

```http
Authorization: Bearer <jwt>
```

Gateway 会解析 JWT 中的 `userId`，并向后端服务注入可信请求头：

```http
X-User-Id: <userId>
```

白名单匿名 GET 接口：

- `GET /api/v1/products/spus/**`
- `GET /api/v1/products/skus/{sku-id}`，仅数字 SKU ID
- `GET /api/v1/search/**`
- `GET /api/v1/categories/tree`
- `GET /api/v1/reviews/products/**`

外部禁止访问的内部接口：

- `/api/v1/products/inner/**`
- `/api/v1/search/inner/**`
- `/api/v1/products/skus/lock-stock`
- `/api/v1/products/skus/unlock-stock`

注意：部分内部接口可被服务间 Feign 调用或 E2E 直接打服务端口验证，但不应通过 Gateway 暴露给外部用户。

## 5. Gateway 路由与限流

Gateway 路由：

| 路径 | 目标服务 |
| --- | --- |
| `/api/v1/products/**` | `velocity-mall-product` |
| `/api/v1/categories/**` | `velocity-mall-product` |
| `/api/v1/orders/**` | `velocity-mall-order` |
| `/api/v1/carts/**` | `velocity-mall-order` |
| `/api/v1/seckill/**` | `velocity-mall-seckill` |
| `/api/v1/search/**` | `velocity-mall-search` |
| `/api/v1/coupons/**` | `velocity-mall-coupon` |
| `/api/v1/reviews/**` | `velocity-mall-review` |

Sentinel 当前对秒杀接口做网关限流：

- 资源：`/api/v1/seckill/execute/.*`
- 自定义 API 名：`seckill_api`
- 当前配置 QPS：`5`
- 限流返回：HTTP `429`，业务码 `42900`

## 6. 已实现接口清单

### 6.1 商品分类接口

#### `GET /api/v1/categories/tree`

说明：查询商品分类树。

鉴权：匿名 GET 可访问。

返回：`Result<List<CategoryTreeVO>>`。

当前能力：

- 分类树构建。
- Redis 缓存分类树。
- 缓存 TTL 带随机扰动。
- Redisson 锁保护回源。

### 6.2 商品接口

#### `GET /api/v1/products/spus/{spu-id}`

说明：查询 SPU 详情。

鉴权：匿名 GET 可访问。

返回：`Result<SpuDetailVO>`。

当前能力：

- 查询 SPU 基础信息及关联 SKU。
- Redis 缓存商品详情。
- 空对象防穿透。
- 随机 TTL 防雪崩。
- Redisson 锁防击穿。

#### `GET /api/v1/products/skus/{sku-id}`

说明：查询 SKU 详情。

鉴权：匿名 GET 可访问，Gateway 只允许数字 SKU ID。

返回：`Result<SkuVO>`。

#### `GET /api/v1/products/inner/skus/{sku-id}`

说明：内部接口，查询 SKU 搜索索引源数据。

鉴权：外部 Gateway 黑名单拦截；服务间调用使用。

返回：`Result<ProductSkuSearchDTO>`。

#### `GET /api/v1/products/inner/skus/search-source?page=1&size=500`

说明：内部接口，分页查询已发布 SKU 搜索源数据，用于重建 Elasticsearch 索引。

鉴权：外部 Gateway 黑名单拦截；搜索服务调用。

返回：`Result<PageVO<ProductSkuSearchDTO>>`。

#### `PUT /api/v1/products/inner/skus/{sku-id}`

说明：内部接口，更新 SKU 基础信息。

请求体：

```json
{
  "skuName": "string",
  "price": 1999.00,
  "coverImg": "string"
}
```

鉴权：外部 Gateway 黑名单拦截。

当前能力：

- 更新 SKU 基础信息。
- 触发商品同步消息，用于搜索索引更新。

#### `PUT /api/v1/products/inner/spus/{spu-id}/publish`

说明：内部接口，上架 SPU。

鉴权：外部 Gateway 黑名单拦截。

当前能力：

- 更新上架状态。
- 清理/刷新缓存。
- 触发商品同步消息。

#### `PUT /api/v1/products/inner/spus/{spu-id}/unpublish`

说明：内部接口，下架 SPU。

鉴权：外部 Gateway 黑名单拦截。

当前能力：

- 更新下架状态。
- 清理/刷新缓存。
- 触发商品同步消息。

#### `PUT /api/v1/products/skus/lock-stock`

说明：锁定单个 SKU 库存的旧接口。

请求体：

```json
{
  "skuId": 2001,
  "quantity": 1
}
```

鉴权：Gateway 黑名单拦截，不应外部访问。

#### `PUT /api/v1/products/skus/unlock-stock`

说明：释放单个 SKU 锁定库存的旧接口。

请求体：

```json
{
  "orderSn": "VM202605...",
  "skuId": 2001,
  "quantity": 1
}
```

鉴权：Gateway 黑名单拦截，不应外部访问。

#### `PUT /api/v1/products/inner/skus/lock-batch`

说明：内部接口，批量锁定普通订单物理库存。

请求体：`StockLockDTO`，包含 `orderSn` 和订单商品项列表。

鉴权：外部 Gateway 黑名单拦截；订单服务 Feign 调用。

当前能力：

- MySQL 条件更新锁库存。
- 写入 `pms_stock_lock_log`。
- 通过影响行数判断库存不足或并发冲突。

#### `PUT /api/v1/products/inner/skus/unlock-batch`

说明：内部接口，批量释放普通订单锁定库存。

请求体：`StockLockDTO`。

鉴权：外部 Gateway 黑名单拦截；订单服务 Feign 调用。

当前能力：

- 释放 `lock_stock`。
- 更新库存锁定日志状态。
- 支持订单取消、延时关单、异常补偿。

### 6.3 购物车接口

#### `POST /api/v1/carts/items`

说明：加入购物车。

鉴权：需要 JWT。

请求体：

```json
{
  "skuId": 2001,
  "quantity": 2
}
```

返回：`Result<Void>`。

当前能力：

- 根据当前用户写 Redis Hash。
- 同一 SKU 再次加入会累加数量。
- 会通过商品服务校验 SKU。

#### `GET /api/v1/carts/items`

说明：查询当前用户购物车。

鉴权：需要 JWT。

返回：`Result<List<CartItemVO>>`。

当前能力：

- 从 Redis Hash 读取购物车。
- 查询 SKU 信息组装购物车展示数据。

#### `DELETE /api/v1/carts/items/{sku-id}`

说明：删除当前用户购物车中的某个 SKU。

鉴权：需要 JWT。

返回：`Result<Void>`。

### 6.4 订单接口

#### `POST /api/v1/orders`

说明：提交普通订单。

鉴权：需要 JWT。

请求体：

```json
{
  "skuIds": [2001]
}
```

返回：`Result<OrderVO>`，包含订单号等信息。

当前能力：

- 从当前用户购物车读取指定 SKU 数量。
- 调用商品服务批量锁定库存。
- 写入 `oms_order` 和 `oms_order_item`。
- 生成订单商品快照。
- 发送普通订单延时关单消息。
- 下单成功后删除购物车中已下单 SKU。

#### `GET /api/v1/orders?page=1&size=10&status=0`

说明：分页查询当前用户订单列表。

鉴权：需要 JWT。

返回：`Result<PageVO<OrderDetailVO>>`。

参数：

- `page`：页码，默认 1。
- `size`：每页数量，默认 10。
- `status`：可选订单状态。

#### `GET /api/v1/orders/{order-sn}`

说明：查询当前用户订单详情。

鉴权：需要 JWT。

返回：`Result<OrderDetailVO>`。

#### `PUT /api/v1/orders/{order-sn}/cancel`

说明：取消待支付订单。

鉴权：需要 JWT。

返回：`Result<Void>`。

当前能力：

- 校验订单归属。
- 仅待支付订单可取消。
- 普通订单释放商品锁定库存。
- 秒杀订单发送 Redis 库存回滚消息。

#### `POST /api/v1/orders/pay/mock?orderSn=...&payType=1`

说明：模拟支付成功回调。

鉴权：需要 JWT。

返回：`Result<Void>`。

当前能力：

- 校验订单归属和订单状态。
- 标记订单已支付。
- 发送 `payment-success-topic` 消息。
- 商品服务消费后扣减物理库存、减少锁定库存、增加销量。
- 消费侧写 `mq_consume_log` 做幂等记录。

#### `POST /api/v1/orders/{order-sn}/refund/mock`

说明：模拟退款。

鉴权：需要 JWT。

返回：`Result<Void>`。

当前能力：

- 校验订单归属和订单状态。
- 标记订单退款状态。
- 发送 `order-refund-topic` 消息。
- 商品服务消费后回滚库存和销量。
- 消费侧写 `mq_consume_log` 做幂等记录。

### 6.5 优惠券接口

#### `POST /api/v1/coupons/{coupon-id}/claim`

说明：领取优惠券。

鉴权：需要 JWT。

返回：`Result<Void>`。

当前能力：

- 校验优惠券存在、有效期、可领取状态。
- 使用 Redis 记录用户领取次数，用于限领预检查。
- MySQL 条件更新扣减优惠券库存。
- 写入 `sms_coupon_history`。
- 重复领取返回业务警告，不重复扣库存。
- 事务隔离级别为 `READ_COMMITTED`。

### 6.6 秒杀接口

#### `POST /api/v1/seckill/execute/{skuId}`

说明：执行秒杀抢购。

鉴权：需要 JWT。

返回：`Result<String>`，成功时返回秒杀订单号。

当前能力：

- 定时任务预热测试 SKU 秒杀库存到 Redis。
- Redis Lua 原子完成库存判断、库存扣减、用户占位。
- 防止同一用户重复抢购。
- 发送 `seckill-order-topic` 消息。
- MQ 发送失败时回滚 Redis 库存和用户占位。
- 订单服务消费后异步创建秒杀订单。
- 发送秒杀订单延时关单消息。
- 秒杀超时未支付时可通过 `seckill-rollback-topic` 回滚 Redis 占位。

Redis Key：

- `velocitymall:seckill:stock:{skuId}`
- `velocitymall:seckill:bought:{skuId}`

### 6.7 搜索接口

#### `GET /api/v1/search/skus?keyword=Phone&sort=sale_desc&page=1&size=10`

说明：搜索 SKU。

鉴权：匿名 GET 可访问。

返回：`Result<PageVO<SearchSkuVO>>`。

参数：

- `keyword`：可选关键词。
- `sort`：默认 `sale_desc`。
- `page`：默认 1，范围 1 到 100。
- `size`：默认 10，范围 1 到 100。

当前能力：

- 查询 Elasticsearch 索引。
- 支持分页。
- 支持基础排序。

#### `POST /api/v1/search/inner/skus/rebuild-index`

说明：内部接口，重建 SKU 搜索索引。

鉴权：

- 通过 Gateway 访问会被黑名单拦截。
- E2E 中直接访问搜索服务 `8085` 验证。

返回：`Result<RebuildIndexVO>`。

当前能力：

- 从商品服务分页拉取已发布 SKU 搜索源数据。
- 重建 Elasticsearch 索引。
- 使用 Redisson 锁避免重复重建。

### 6.8 商品评价接口

#### `POST /api/v1/reviews`

说明：发布商品评价。

鉴权：需要 JWT。

请求体：

```json
{
  "orderSn": "VM202605...",
  "skuId": 2001,
  "spuId": 1001,
  "rating": 5,
  "content": "产品质量很好"
}
```

返回：`Result<Void>`。

当前能力：

- 校验评价归属（`UserContext.getUserId()`）。
- 通过 Feign 调用订单服务校验用户已购买且已支付对应订单的对应 SKU。
- 通过数据库唯一约束 `uk_user_order_sku` 保证同一用户对同一订单同一 SKU 只可评价一次。
- 捕获 `DuplicateKeyException` 做幂等防重。
- 写入评价后清除该 SPU 的评价统计缓存。

#### `GET /api/v1/reviews/products/{spu-id}?page=1&size=10`

说明：分页查询商品评价列表。

鉴权：匿名 GET 可访问，网关白名单放行。若用户已登录（携 JWT），会填充当前用户对每条评价的互动状态。

返回：`Result<PageVO<ReviewVO>>`，每条评价包含 `currentInteractionType`（0=无互动，1=已赞，2=已踩）。

参数：

- `page`：页码，默认 1，最小 1。
- `size`：每页数量，默认 10，范围 1-100。

当前能力：

- MyBatis-Plus 分页按 `create_time DESC` 排序。
- 可选地批量查询当前用户互动记录并填充到 VO。

#### `GET /api/v1/reviews/products/{spu-id}/stats`

说明：查询商品评价统计（总评价数、好评数、好评率）。

鉴权：匿名 GET 可访问，网关白名单放行。

返回：`Result<ReviewStatsVO>`，包含 `totalCount`、`goodCount`、`goodRate`（百分比，两位小数，评分 >= 4 计为好评）。

当前能力：

- Redis 缓存统计结果，TTL 30 分钟 + 0-10 分钟随机抖动。
- 空结果缓存 1 分钟。
- Redisson 分布式锁 + 双重检查防击穿。
- 锁超时返回友好提示。

#### `POST /api/v1/reviews/{review-id}/interaction`

说明：对评价进行互动（点赞/点踩/切换/取消）。

鉴权：需要 JWT。

请求体：

```json
{
  "interactionType": 1
}
```

`interactionType`：1=点赞，2=点踩。

返回：`Result<Void>`。

当前能力：

- **无记录 + 点赞**：插入互动记录 + 增加点赞计数（原子 `UPDATE`，并发兜底 `DuplicateKeyException`）。
- **无记录 + 点踩**：插入互动记录 + 增加点踩计数。
- **同类型重复**：物理删除互动记录 + 减少对应计数（取消操作）。
- **从点赞切换到点踩**（反之亦然）：更新互动类型 + 原子切换计数（`like_count - 1, dislike_count + 1`）。
- 所有计数减操作均有 `> 0` 条件，防止负值。
- 互动表使用物理删除（非逻辑删除），配合唯一约束 `uk_review_user` 保证一个用户对一条评价只有一条有效记录。

#### `DELETE /api/v1/reviews/{review-id}`

说明：删除自己的评价。

鉴权：需要 JWT。

返回：`Result<Void>`。

当前能力：

- 校验评价归属（仅可删除自己的评价）。
- MyBatis-Plus 逻辑删除（`is_deleted = 1`）。
- 删除后清除该 SPU 的评价统计缓存。

## 7. 已实现消息链路

| Topic | Producer | Consumer | 用途 |
| --- | --- | --- | --- |
| `normal-order-delay-topic` | 订单服务 | 订单服务 `NormalOrderDelayConsumer` | 普通订单超时关闭与库存释放 |
| `velocity-mall-order-delay-topic` | 订单服务 | 订单服务 `OrderDelayConsumer` | 旧版订单延时关单/库存释放链路 |
| `payment-success-topic` | 订单服务 | 商品服务 `PaymentSuccessConsumer` | 支付成功后扣减物理库存、销量增加 |
| `order-refund-topic` | 订单服务 | 商品服务 `OrderRefundConsumer` | 退款后回滚库存和销量 |
| `seckill-order-topic` | 秒杀服务 | 订单服务 `SeckillOrderConsumer` | 秒杀成功后异步创建订单 |
| `seckill-delay-topic` | 订单服务 | 订单服务 `SeckillDelayConsumer` | 秒杀订单超时关闭 |
| `seckill-rollback-topic` | 订单服务 | 秒杀服务 `SeckillStockRollbackConsumer` | 秒杀订单关闭后回滚 Redis 秒杀库存 |
| `product-sync-topic` | 商品服务 | 搜索服务 `ProductSyncConsumer` | 商品上架、下架、SKU 更新后同步搜索索引 |

## 8. 数据与中间件现状

E2E 使用 Docker Compose 启动：

- MySQL 8.0，端口 `3306`。
- Redis 7.2，端口 `6379`。
- Nacos 2.2.3，端口 `8848` 和 `9848`。
- RocketMQ 4.9.4，NameServer `9876`，Broker `10911`。
- Elasticsearch 8.10.4，HTTP 端口映射 `9201`。

E2E 初始化核心表：

- 商品：`pms_category`、`pms_spu`、`pms_sku`、`pms_stock_lock_log`。
- 订单：`oms_order`、`oms_order_item`。
- 优惠券：`sms_coupon`、`sms_coupon_history`。
- 评价：`oms_product_review`、`oms_review_interaction`。
- 消息幂等：`mq_consume_log`。

## 9. E2E 当前覆盖链路

`scripts/ci/e2e.sh` 当前覆盖：

1. 启动 MySQL、Redis、Nacos、RocketMQ、Elasticsearch。
2. 启动 product、order、seckill、search、coupon、gateway 六个应用（review 服务代码完整但 E2E 尚未纳入）。
3. 等待 Nacos 注册传播。
4. 验证分类树查询。
5. 验证 SPU 详情查询。
6. 验证 SKU 详情查询。
7. 验证优惠券领取、库存扣减、领取记录、重复领取不重复扣库存。
8. 验证购物车加入和查询。
9. 验证普通订单创建、库存锁定、订单明细快照。
10. 验证 mock 支付、订单状态、库存扣减、销量增加、消费幂等记录。
11. 验证 mock 退款、库存回滚、销量回滚、消费幂等记录。
12. 验证秒杀 Redis Lua 扣库存和用户占位。
13. 验证秒杀订单异步落库。
14. 验证搜索索引重建。
15. 验证搜索查询。
16. 验证搜索内部接口经 Gateway 访问返回 403。
17. 评价模块（review）尚未纳入 E2E，后续应补充：发评、查评、评价统计、点赞/点踩/切换/取消、删除评价等用例。

## 10. 当前未实现或需后续规划的能力

以下能力在文档或产品方向中已经出现，但当前代码尚未完整实现。注意：当前项目已经完成 Phase 10 到 Phase 16，后续规划应以这些能力为基线继续追加阶段，不要再按”当前停留在 Phase 8”理解：

- 用户模块：
  - 当前 JWT 使用固定测试 token，项目中没有完整用户注册、登录、刷新 token、用户资料模块。
- 管理后台：
  - 商品新增、类目维护、优惠券创建、活动配置等后台管理能力未形成完整外部 API。
- 支付系统：
  - 当前为 mock 支付接口，没有对接真实支付渠道。
- 订单履约：
  - 地址、物流、确认收货、售后工单等链路未实现。
- 压测脚本：
  - 当前有全链路 E2E，未看到独立的 JMeter/Gatling/k6 压测脚本。
- 观测体系：
  - 未形成完整 TraceId 透传、链路追踪、指标监控、告警面板。

## 11. 后续开发建议

建议在规划未来开发时遵循：

1. 优先不要破坏当前已跑通的 E2E 链路。
2. 不要把”Phase 8”理解为当前项目终点；当前代码已经实现到 Phase 16。
3. 评价模块已实现并独立为 `velocity-mall-review` 服务，包含完整的发评、分页查询、统计、点赞点踩互斥切换、购买资格校验、评价统计缓存等能力。
4. 评价模块目前缺少 E2E 测试覆盖（发评、改评、删评、查看评价、重复点赞、点赞切换点踩、非法操作他人评价等），后续应补充。
5. 用户模块已落地 `velocity-mall-user` 服务（注册/登录/JWT/地址），但整体用户体系（刷新 token、用户资料管理等）仍可深化。
6. 常规交易链路不要为了”万级 QPS”过度设计；项目当前定位是常规千级、热点读和秒杀入口数千级演示。
