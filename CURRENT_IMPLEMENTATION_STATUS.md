# VelocityMall 当前实现状态与接口功能文档

> 同步基准：当前代码、配置、SQL、`velocity-mall-web`、`velocity-mall-admin-web`、`scripts/ci/e2e.sh` 与 GitHub Actions CI workflow。旧版 Markdown 如与本文件或代码冲突，以当前代码为准。

## 0. 当前阶段快照

当前项目总阶段已推进到 **Phase 23：3 节点高可用极限压榨与自动闭环**。旧版文档中关于 Phase 3 或 Phase 1-21 的表述均已滞后；后续判断项目状态时，以本节和当前代码、配置、压测脚本为准。

| 项目 | 当前结论 |
| --- | --- |
| 当前阶段 | **Phase 23：3 节点高可用极限压榨与自动闭环** |
| 极限拓扑 | Docker Nginx -> Gateway x3 -> Nacos 负载均衡 -> Seckill x3 -> RocketMQ -> Order 异步落库 |
| 部署形态 | 混合部署：MySQL、Redis、Nacos、RocketMQ、Nginx、Elasticsearch 使用 Docker；Java 微服务使用宿主机 JDK 17 多进程运行 |
| 压测成绩 | 1500 VUs 下经 Nginx 负载均衡达到 **2142 QPS**，p50 延迟低至 **7.59ms** |
| 数据一致性 | Redis Lua 原子扣减在高并发下 100% 准确，1000 库存精确售出，绝对零超卖 |
| 工程闭环 | `scripts/performance/cleanup_server.py` 提供 `/setup` 与 `/cleanup`，保证压测数据自动初始化和销毁 |
| C 端前台 | 已新增 `velocity-mall-web`，技术栈为 Vue 3 + Vite + Pinia + Vue Router + Axios；用户端请求统一经 Nginx/Gateway，不直连 Java 服务端口 |
| 管理后台前台 | 已新增 `velocity-mall-admin-web`，覆盖登录、商品、订单、秒杀活动、优惠券、评论、媒体和搜索索引管理页面 |
| 秒杀活动数据 | 秒杀价、原价、开始/结束时间、活动状态已由后端 `sms_seckill_activity` 表和 Seckill API 提供，不再由前端静态配置 |
| 媒体资源 | 商品封面使用 MinIO，经 `/minio/velocity-mall-product/...` 访问；当前已有默认封面与 SKU 2002 未开始活动演示封面 |
| 已知瓶颈 | Windows OS 层面的 TCP 临时端口（Ephemeral Port）/连接资源耗尽，表现为 `Connection Refused`；该瓶颈来自本地压测环境，不是 Redis Lua 或业务代码问题 |

## 1. 项目定位

VelocityMall 是一个模拟真实中型垂直电商场景的分布式微服务商城系统。当前重点是展示企业级代码治理、清晰分层、交易一致性、缓存治理、消息可靠性和秒杀高并发入口，而不是堆砌不可运行的中间件。

当前主干已经覆盖商品、购物车、订单、支付 mock、退款 mock、秒杀、数据库驱动秒杀活动、搜索、优惠券、评价、用户、地址、C 端买家前台、管理后台前台、后台发货、商品 SPU/SKU 管理、优惠券管理、秒杀活动管理、评论管理、搜索索引重建、MinIO 商品封面和基础 Trace 透传；性能侧已经完成 Phase 23 的 Gateway x3 与 Seckill x3 本地混合集群极限压测。

## 2. 当前模块划分

| 模块 | 服务名 | 职责 | 默认端口 |
| --- | --- | --- | ---: |
| `velocity-mall-common` | 无独立服务 | 公共响应、异常、实体基类、Redis/MyBatis/Feign/Trace 配置、跨服务 DTO/VO、用户上下文 | -- |
| `velocity-mall-gateway` | `velocity-mall-gateway` | 统一入口、JWT 鉴权、用户/管理员 Header 注入、内部接口拦截、Sentinel 秒杀限流、路由转发 | 8080 |
| `velocity-mall-product` | `velocity-mall-product` | 商品分类、SPU/SKU 查询、商品缓存、库存锁定/释放/扣减/退款、商品同步消息 | 8081 |
| `velocity-mall-order` | `velocity-mall-order` | 购物车、普通订单、订单查询、支付 mock、退款 mock、延时关单、秒杀订单落库、发货、确认收货 | 8082 |
| `velocity-mall-seckill` | `velocity-mall-seckill` | 秒杀库存预热、Redis Lua 抢购、防重复抢购、MQ 异步下单、Redis 占位回滚 | 8083 |
| `velocity-mall-search` | `velocity-mall-search` | Elasticsearch 商品搜索、索引重建、商品同步消费 | 8085 |
| `velocity-mall-coupon` | `velocity-mall-coupon` | 优惠券领取、可用券/我的券查询、内部核销/释放、领取记录、限领、防重复领取 | 8086 |
| `velocity-mall-review` | `velocity-mall-review` | 商品评价发布/分页/统计/删除、点赞/点踩互动、购买资格校验 | 8087 |
| `velocity-mall-user` | `velocity-mall-user` | 用户注册、登录、JWT、当前用户信息、收货地址 | 8088 |
| `velocity-mall-admin` | `velocity-mall-admin` | 管理员登录、商品 SPU/SKU 管理、SKU 封面上传、订单发货、秒杀活动管理/预热、优惠券管理、评论管理、搜索索引重建 | 8089 |
| `velocity-mall-web` | 前端应用 | C 端买家前台：登录/注册、首页搜索、商品详情、秒杀抢购、排队结果、购物车、地址、优惠券、订单、mock 支付、评价 | 5173 |
| `velocity-mall-admin-web` | 前端应用 | 管理后台前台：登录、商品、订单、秒杀活动、优惠券、评论、媒体资源、搜索索引运维 | 5174 |

## 3. 全局里程碑与当前阶段

当前项目已完成 Phase 1-23。旧版功能 Roadmap 中的局部编号仅可作为历史实现线索，不再代表项目总阶段。

| Phase | 目标 | 当前状态 |
| --- | --- | --- |
| Phase 1-20 企业级微服务电商基座构建 | 完成 User、Product、Order、Seckill 等核心微服务链路；全面接入 Spring Cloud Alibaba、Nacos、RocketMQ、Redis、Sentinel；落地 JWT 鉴权传递、交易状态流转、消息幂等与最终一致性基础方案 | 已完成 |
| Phase 21 秒杀系统单点高并发防线与基准压测 | 引入 Redis Lua 保障库存扣减绝对原子性；引入 RocketMQ 实现秒杀订单异步削峰落库；使用 k6 完成单机基准压测，确立单体承载基线 | 已完成 |
| Phase 22 混合部署集群方案确立 | 确立“中间件容器化 + 业务进程宿主机直跑”的本地部署基调；Docker 管理 MySQL、Redis、Nacos、RocketMQ、Nginx、Elasticsearch；Windows 宿主机通过 `--server.port` 启动多 Java 实例；Docker Nginx 通过 `host.docker.internal` 引流到宿主机进程 | 已完成 |
| Phase 23 3 节点高可用极限压榨与自动闭环 | 成功拉起 Gateway x3 + Seckill x3 高可用集群；1500 VUs 达到 2142 QPS、p50 7.59ms；1000 库存零超卖；`cleanup_server.py` 完成 setup/teardown 数据闭环；识别 Windows TCP Ephemeral Port 耗尽为本地 OS 瓶颈 | **当前最新** |
| Phase 24 文档对齐、后台闭环与 CI 稳定化 | 计划将已实现的 Admin/Admin Web、Full Chain E2E、CI k6 smoke、接口边界和数据初始化固化为稳定基座；第一轮优先同步文档，不新增业务 API | 下一阶段规划 |

Phase 23 之后，项目在不改变总阶段编号的前提下补齐了 C 端买家前台、管理后台前台、Admin 全量管理接口、MinIO 商品封面兜底、数据库驱动的秒杀活动配置，以及“活动未开始”长倒计时演示商品。

## 4. 通用接口约定

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

## 5. 鉴权与 Gateway 路由

统一入口为 `http://127.0.0.1:8080`。

公开 POST：

- `POST /api/v1/users/register`
- `POST /api/v1/users/login`
- `POST /api/v1/admin/login`

公开 GET：

- `GET /api/v1/products/spus/**`
- `GET /api/v1/products/skus/{sku-id}`，仅数字 SKU ID
- `GET /api/v1/search/**`
- `GET /api/v1/categories/tree`
- `GET /api/v1/reviews/products/**`

需要用户登录的接口携带：

```http
Authorization: Bearer <user-jwt>
```

Gateway 解析 `userId` 并注入：

```http
X-User-Id: <userId>
```

管理后台接口携带管理员 JWT。Gateway 解析 `adminId` 并注入：

```http
X-Admin-Id: <adminId>
```

Gateway 当前路由：

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
| `/api/v1/users/**` | `velocity-mall-user` |
| `/api/v1/admin/**` | `velocity-mall-admin` |

外部禁止访问的内部接口：

- `/api/v1/products/inner/**`
- `/api/v1/search/inner/**`
- `/api/v1/orders/inner/**`
- `/api/v1/users/inner/**`
- `/api/v1/products/skus/lock-stock`
- `/api/v1/products/skus/unlock-stock`

当前接口分层约定：

- Public：C 端用户可经 Gateway 访问的商品、分类、搜索、评论、用户、地址、购物车、订单、优惠券和秒杀接口。
- Admin：`/api/v1/admin/**`，必须使用管理员 JWT；Gateway 注入 `X-Admin-Id`。
- Inner：`/inner/**` 仅允许服务间调用，Gateway 外部访问必须拦截。`/api/v1/products/skus/lock-stock` 与 `/unlock-stock` 为当前兼容库存接口，外部同样禁止访问，后续边界清理时再评估是否统一迁移到 `/inner/**`。

Sentinel 当前对秒杀接口做网关限流，资源为 `/api/v1/seckill/execute/.*`，API 名为 `seckill_api`，默认 QPS 为 `5`，限流返回 HTTP `429`、业务码 `42900`。

## 6. 已实现接口清单

### 6.1 商品与分类

- `GET /api/v1/categories/tree`：查询分类树，匿名访问，Redis 缓存 + Redisson 回源保护。
- `GET /api/v1/products/spus/{spu-id}`：查询 SPU 详情，匿名访问，商品详情缓存、空对象、防击穿、防雪崩。
- `GET /api/v1/products/skus/{sku-id}`：查询 SKU 详情，匿名访问，Gateway 仅放行数字 SKU ID。
- `GET /api/v1/products/inner/skus/{sku-id}`：内部查询 SKU 搜索源数据。
- `GET /api/v1/products/inner/skus/search-source?page=1&size=500`：内部分页拉取搜索源数据。
- `PUT /api/v1/products/inner/skus/{sku-id}`：内部更新 SKU 基础信息并触发搜索同步。
- `PUT /api/v1/products/inner/spus/{spu-id}/publish`：内部上架 SPU。
- `PUT /api/v1/products/inner/spus/{spu-id}/unpublish`：内部下架 SPU。
- `PUT /api/v1/products/skus/lock-stock`：兼容库存锁定接口，外部经 Gateway 禁止访问。
- `PUT /api/v1/products/skus/unlock-stock`：兼容库存释放接口，外部经 Gateway 禁止访问。
- `PUT /api/v1/products/inner/skus/lock-batch`：内部批量锁定普通订单物理库存。
- `PUT /api/v1/products/inner/skus/unlock-batch`：内部批量释放普通订单锁定库存。

### 6.2 用户与地址

- `POST /api/v1/users/register`：用户注册，BCrypt 保存密码。
- `POST /api/v1/users/login`：用户登录，返回 JWT 和用户基本信息。
- `GET /api/v1/users/me`：查询当前用户。
- `POST /api/v1/users/addresses`：新增收货地址。
- `PUT /api/v1/users/addresses/{id}`：修改自己的收货地址。
- `DELETE /api/v1/users/addresses/{id}`：删除自己的收货地址。
- `GET /api/v1/users/addresses`：查询自己的收货地址列表。
- `GET /api/v1/users/inner/addresses/{id}?userId=...`：内部按用户校验并查询地址，供订单服务保存地址快照。

### 6.3 购物车与订单

- `POST /api/v1/carts/items`：加入购物车，Redis Hash 存储，同 SKU 累加数量。
- `GET /api/v1/carts/items`：查询当前用户购物车。
- `DELETE /api/v1/carts/items/{sku-id}`：删除购物车 SKU。
- `POST /api/v1/orders`：提交普通订单，请求体包含 `skuIds` 和 `addressId`。
- `GET /api/v1/orders?page=1&size=10&status=0`：分页查询当前用户订单。
- `GET /api/v1/orders/{order-sn}`：查询当前用户订单详情。
- `PUT /api/v1/orders/{order-sn}/cancel`：取消待支付订单。
- `POST /api/v1/orders/pay/mock?orderSn=...&payType=1`：模拟支付成功。
- `POST /api/v1/orders/{order-sn}/refund/mock`：模拟退款。
- `PUT /api/v1/orders/{order-sn}/confirm-receipt`：用户确认收货，要求订单已发货且属于当前用户。
- `GET /api/v1/orders/inner/check-purchase?userId=...&orderSn=...&skuId=...`：内部校验用户是否拥有已完成订单中的指定 SKU，供评价服务使用。
- `POST /api/v1/orders/inner/{order-sn}/deliver`：内部发货接口，供 admin 服务调用。

订单状态当前含义：

| status | 含义 |
| ---: | --- |
| 0 | 待付款 |
| 1 | 已付款 |
| 2 | 已发货 |
| 3 | 已完成 |
| 4 | 已关闭/取消 |
| 5 | 已退款 |

### 6.4 管理后台

- `POST /api/v1/admin/login`：管理员登录，返回 admin JWT。
- `GET /api/v1/admin/products/spus`：分页查询后台 SPU。
- `GET /api/v1/admin/products/spus/{spu-id}`：查询后台 SPU 详情。
- `POST /api/v1/admin/products/spus`：新增 SPU。
- `PUT /api/v1/admin/products/spus/{spu-id}`：编辑 SPU。
- `POST /api/v1/admin/orders/{order-sn}/deliver?deliveryCompany=...&deliverySn=...`：管理员发货。
- `PUT /api/v1/admin/products/spus/{spu-id}/status?action=publish|unpublish`：管理员上架或下架 SPU。
- `POST /api/v1/admin/products/skus`：新增 SKU。
- `PUT /api/v1/admin/products/skus/{sku-id}`：编辑 SKU。
- `POST /api/v1/admin/products/skus/{sku-id}/cover`：上传 SKU 封面。
- `GET /api/v1/admin/orders`：分页查询后台订单。
- `GET /api/v1/admin/orders/{order-sn}`：查询后台订单详情。
- `GET /api/v1/admin/seckill/activities`：分页查询秒杀活动。
- `POST /api/v1/admin/seckill/activities`：新增秒杀活动。
- `PUT /api/v1/admin/seckill/activities/{id}`：编辑秒杀活动。
- `PUT /api/v1/admin/seckill/activities/{id}/status`：修改秒杀活动状态。
- `POST /api/v1/admin/seckill/activities/{id}/preheat`：手动预热秒杀活动。
- `GET /api/v1/admin/coupons`：分页查询优惠券。
- `POST /api/v1/admin/coupons`：新增优惠券。
- `PUT /api/v1/admin/coupons/{id}`：编辑优惠券。
- `PUT /api/v1/admin/coupons/{id}/status`：修改优惠券状态。
- `GET /api/v1/admin/reviews`：分页查询后台评价。
- `DELETE /api/v1/admin/reviews/{id}`：后台删除评价。
- `POST /api/v1/admin/search/skus/rebuild-index`：后台触发搜索索引重建。

### 6.5 优惠券

- `POST /api/v1/coupons/{coupon-id}/claim`：领取优惠券，需要 JWT。
- `GET /api/v1/coupons/available`：查询当前用户可领取/可用优惠券，需要 JWT。
- `GET /api/v1/coupons/my`：查询当前用户已领取优惠券，需要 JWT。
- `POST /api/v1/coupons/inner/use`：内部核销优惠券。
- `POST /api/v1/coupons/inner/release`：内部释放优惠券。

当前能力：

- 校验优惠券存在、有效期、可领取状态。
- Redis 记录用户领取次数，用于限领前置检查。
- MySQL 条件更新扣减优惠券库存。
- 写入 `sms_coupon_history`。
- 重复领取返回业务警告，不重复扣库存。
- 订单链路可通过内部接口完成优惠券核销与释放。

### 6.6 秒杀

- `GET /api/v1/seckill/activities`：查询启用且未结束的秒杀活动列表，需要 JWT；返回后端计算后的活动状态。
- `GET /api/v1/seckill/activities/skus/{skuId}`：按 SKU 查询秒杀活动，需要 JWT。
- `POST /api/v1/seckill/execute/{skuId}`：执行秒杀抢购，需要 JWT。
- `GET /api/v1/orders/seckill/result/{skuId}`：秒杀请求进入 MQ 后由前端轮询订单生成结果，需要 JWT。

当前能力：

- `sms_seckill_activity` 提供秒杀价、原价、活动库存、开始/结束时间和启用状态。
- Seckill 服务根据当前时间返回 `NOT_STARTED`、`ACTIVE`、`ENDED`、`DISABLED` 等前端状态。
- 定时任务预热启用且处于有效窗口内的活动库存到 Redis。
- Redis Lua 原子完成库存判断、库存扣减、用户占位。
- 防止同一用户重复抢购。
- 活动未开始、活动已结束、活动禁用会在进入 Redis Lua 前被后端拦截。
- 发送 `seckill-order-topic`。
- MQ 发送失败时回滚 Redis 库存和用户占位。
- 订单服务消费后异步创建秒杀订单并发送延时关单消息。
- 秒杀超时未支付时通过 `seckill-rollback-topic` 回滚 Redis 占位。

Redis Key：

- `velocitymall:seckill:stock:{skuId}`
- `velocitymall:seckill:bought:{skuId}`

### 6.7 搜索

- `GET /api/v1/search/skus?keyword=Phone&sort=sale_desc&page=1&size=10`：搜索 SKU，匿名访问。
- `POST /api/v1/search/inner/skus/rebuild-index`：内部重建 SKU 搜索索引，Gateway 外部访问会被拦截。

当前搜索行为：

- 数字关键词按 SKU ID 精确匹配，例如 `keyword=2002` 只返回 SKU 2002。
- 文本关键词按 SKU 名称短语匹配，避免单字误命中过多商品。
- 首页和秒杀专区均按搜索结果与后端秒杀活动数据合并展示；无命中时返回空列表，不回退展示全部商品。

### 6.8 商品评价

- `POST /api/v1/reviews`：发布商品评价，需要 JWT。当前代码通过订单服务校验用户拥有已完成订单中的对应 SKU。
- `GET /api/v1/reviews/products/{spu-id}?page=1&size=10`：分页查询商品评价，匿名可访问；携带 JWT 时会填充当前用户互动状态。
- `GET /api/v1/reviews/products/{spu-id}/stats`：查询商品评价统计，匿名可访问，Redis 缓存。
- `POST /api/v1/reviews/{review-id}/interaction`：点赞/点踩/切换/取消，需要 JWT。
- `DELETE /api/v1/reviews/{review-id}`：删除自己的评价，需要 JWT。

评价核心约束：

- 同一用户对同一订单同一 SKU 只能发布一条有效评价。
- 删除评价必须校验归属。
- 点赞和点踩互斥，使用唯一约束 `uk_review_user` 与物理删除/类型切换保证当前状态唯一。
- 计数更新使用原子 `UPDATE`，减操作带 `> 0` 条件。

### 6.9 C 端买家前台

`velocity-mall-web` 当前以真实后端接口为数据源，页面范围包括：

- 登录/注册：`POST /api/v1/users/login`、`POST /api/v1/users/register`，Token 持久化到 `localStorage`。
- 首页与秒杀专区：`GET /api/v1/search/skus` + `GET /api/v1/seckill/activities`，按真实搜索结果与活动数据合并展示。
- 商品详情：`GET /api/v1/products/skus/{skuId}`、`GET /api/v1/products/spus/{spuId}`、`GET /api/v1/reviews/products/{spuId}`、`GET /api/v1/reviews/products/{spuId}/stats`。
- 秒杀抢购：`POST /api/v1/seckill/execute/{skuId}`，成功后轮询 `GET /api/v1/orders/seckill/result/{skuId}`。
- 购物车与普通订单：`POST/GET/DELETE /api/v1/carts/items`、`POST /api/v1/orders`。
- 地址、优惠券、我的订单、mock 支付、mock 退款、确认收货、评价发布与互动均已接入对应后端接口。

前台开发约束：

- 不展示没有真实功能承载的伪导航、伪定位、下载 App、伪规格或伪 SKU 选项。
- 秒杀价、活动时间、活动状态必须来自后端活动接口；前端只负责倒计时和状态渲染。
- 商品封面必须来自后端 SKU `coverImg/cover_img` 或 MinIO 兜底资源，不使用本机绝对路径。

### 6.10 管理后台前台

`velocity-mall-admin-web` 当前以 Admin API 为数据源，页面范围包括：

- 登录：`POST /api/v1/admin/login`，管理员 Token 持久化到 `localStorage`。
- 商品管理：SPU/SKU 列表、详情、新增、编辑、上下架与 SKU 封面上传。
- 订单管理：订单列表、订单详情、发货。
- 秒杀活动：活动列表、新增、编辑、启停与手动预热。
- 优惠券：优惠券列表、新增、编辑与启停。
- 评论管理：评价列表与后台删除。
- 运维入口：媒体资源查看、搜索索引重建。

后台前端开发约束：

- 所有请求走 `/api/v1/admin/**` 或已有 Admin 聚合接口，不直接调用后端内部服务端口。
- 失败路径必须展示后端业务错误信息，不吞异常。
- CI 已新增 `Admin Web Build`，固定执行 `npm ci` 与 `npm run build`，防止后台前端路由、类型和打包配置回归。
- 当前不引入 RBAC，仍使用单一管理员 JWT 模型。

## 7. 已实现消息链路

| Topic | Producer | Consumer | 用途 |
| --- | --- | --- | --- |
| `normal-order-delay-topic` | order | order `NormalOrderDelayConsumer` | 普通订单超时关闭与库存释放 |
| `velocity-mall-order-delay-topic` | order | order `OrderDelayConsumer` | 旧版订单延时关单/库存释放链路 |
| `payment-success-topic` | order | product `PaymentSuccessConsumer` | 支付成功后扣减物理库存、销量增加 |
| `order-refund-topic` | order | product `OrderRefundConsumer` | 退款后回滚库存和销量 |
| `seckill-order-topic` | seckill | order `SeckillOrderConsumer` | 秒杀成功后异步创建订单 |
| `seckill-delay-topic` | order | order `SeckillDelayConsumer` | 秒杀订单超时关闭 |
| `seckill-rollback-topic` | order | seckill `SeckillStockRollbackConsumer` | 秒杀订单关闭后回滚 Redis 秒杀库存 |
| `product-sync-topic` | product | search `ProductSyncConsumer` | 商品上架、下架、SKU 更新后同步搜索索引 |

## 8. 数据与中间件现状

Full Chain E2E 使用 `docker/docker-compose.e2e.yml` 启动：

- MySQL 8.0，端口 `3306`。
- Redis 7.2，端口 `6379`。
- Nacos 2.2.3，端口 `8848` 和 `9848`。
- RocketMQ 4.9.4，NameServer `9876`，Broker `10911`。
- Elasticsearch 8.10.4，HTTP 端口映射 `9201`。

MinIO 与 Nginx 属于本地开发、演示和集群压测形态；CI Full Chain E2E 当前不通过 `docker-compose.e2e.yml` 启动 MinIO/Nginx。CI 中 Java 微服务仍由宿主机 JDK 17 直接启动，不放入 Compose。

SQL 文件位于 `doc/sql/`，当前包含 `schema.sql`、测试数据、Phase 10/11/12/14/15/16/18/19/20/21 增量脚本，以及 `phase23_seckill_activity.sql`。

当前演示商品与秒杀活动：

| SKU | SPU | 商品 | 活动状态 | 秒杀价 | 原价 | 活动时间 | 封面 |
| ---: | ---: | --- | --- | ---: | ---: | --- | --- |
| 2001 | 1001 | Velocity Phone Pro 16GB+512GB 曜石黑 | 进行中 | 4999.00 | 7999.00 | 2026-01-01 10:00:00 至 2027-12-31 23:59:59 | `/minio/velocity-mall-product/products/default-covers/phone-1.png` |
| 2002 | 1002 | Velocity Phone Air 12GB+256GB 松柏绿 | 未开始演示 | 3999.00 | 5999.00 | 2027-06-01 10:00:00 至 2027-06-30 23:59:59 | `/minio/velocity-mall-product/products/seckill-demo/velocity-phone-future-2002.png` |

新增秒杀商品的最小闭环是：写入 SPU/SKU、上传封面到 MinIO、写入 `sms_seckill_activity`、重建搜索索引、等待 Seckill 定时预热或手动触发数据准备。

## 9. E2E 当前覆盖链路

`scripts/ci/e2e.sh` 当前覆盖：

1. 启动 MySQL、Redis、Nacos、RocketMQ、Elasticsearch。
2. 初始化商品、订单、优惠券、评价、用户、地址、管理员等核心表和测试数据。
3. 启动 product、order、seckill、search、coupon、gateway、user、admin、review 九个应用。
4. 等待 Nacos 注册传播。
5. 注册 C 端用户、登录并获取用户 JWT。
6. 新增用户收货地址。
7. 管理员登录并获取 admin JWT。
8. 验证分类树、SPU、SKU 公开查询。
9. 验证未登录领券返回 401。
10. 验证 product/search 内部接口经 Gateway 访问返回 403。
11. 验证优惠券领取、库存扣减、领取记录、重复领取不重复扣库存。
12. 验证购物车加入和查询。
13. 验证普通订单创建、地址快照、库存锁定、订单明细快照。
14. 验证 mock 支付、订单状态、库存扣减、销量增加、消费幂等记录。
15. 验证管理员发货、物流公司/单号落库、用户确认收货。
16. 验证评价发布、评价列表、点赞、点赞切换点踩、匿名统计、删除评价。
17. 验证 mock 退款、库存回滚、销量回滚、消费幂等记录。
18. 验证秒杀 Redis Lua 扣库存、用户占位和秒杀订单异步落库。
19. 验证搜索索引重建、搜索查询。
20. 验证搜索内部接口经 Gateway 访问返回 403。

GitHub Actions 当前包含 Maven Build、Admin Web Build、Full Chain E2E 与 `k6 Load & Chaos Tests (CI Mode)`。Admin Web Build 以 Node.js 24 执行 `npm ci` 和 `npm run build`；Full Chain E2E 使用一次性 schema/seed；CI k6 smoke 使用一次性测试用户生成与 Redis 秒杀库存预热，目标是防回归，不追求本地 Phase 23 极限 QPS。

## 10. 压测脚本、集群架构与报告

压测脚本已存在于 `scripts/performance/`：

- `seckill-load-test.js`
- `multi-user-seckill-test.js`
- `chaos-spike-test.js`
- `extreme-300vu-test.js`
- `extreme-cluster-test.js`
- `generate_users.py`
- `cleanup_server.py`

压测报告位于 `doc/performance/`。当前报告主要覆盖秒杀入口、限流、防超卖、熔断降级和 Gateway/Seckill 多节点集群极限压测。

### 10.1 Phase 23 集群极限压测结论

当前已完成的 Phase 23 集群极限压测使用 **Gateway x3 + Seckill x3 + 异步落库**：

```text
k6 -> Docker Nginx -> Gateway x3(8080/8090/8091)
                    -> Nacos 负载均衡 -> Seckill x3(8083/8093/8094)
                    -> RocketMQ -> Order 异步创建秒杀订单
```

核心结果：

- 300 VU：吞吐约 **1897 req/s**，1000 库存精确售出，超卖 0。
- 1500 VUs：吞吐峰值 **2142 QPS**，p50 延迟低至 **7.59ms**，1000 库存精确售出，超卖 0。
- Redis Lua 原子扣减在高并发下保持 100% 准确。
- Sentinel 在该轮极限测试中全解封，性能瓶颈不在网关限流。
- 1500 VUs 轮次出现较多 `Connection Refused`，根因是 Windows OS 层面的 TCP 临时端口（Ephemeral Port）/连接资源耗尽；迁移 Linux 并调优 TCP 参数后仍有继续上探空间。

### 10.2 压测数据闭环

性能测试必须配合 `scripts/performance/cleanup_server.py`，形成自动化 setup/teardown 数据闭环：

- 测试前调用 `POST http://127.0.0.1:8099/setup`，重置 SKU 2001 库存为 1000、清理历史订单、预热 Redis 秒杀库存。
- 测试后调用 `POST http://127.0.0.1:8099/cleanup`，清理压测用户、订单、库存流水、消费日志和 Redis 秒杀 Key。
- 不允许在未清理历史数据的情况下对比 QPS、成功数或超卖结论，否则测试结果不可采信。

### 10.3 CI k6 smoke

CI 中的 k6 作业固定为小规模稳定性验证：

- 只启动 k6 smoke 必需的 Java 服务和中间件。
- 使用 `LOAD_TEST_USERS=200` 生成测试用户。
- 通过 CI 脚本直接预热 Redis 秒杀库存。
- 运行 `multi-user-seckill-test.js` 与 `chaos-spike-test.js` 的 `CI=true` 模式。
- 该模式不替代本地 Gateway x3 + Seckill x3 + Nginx 极限压测。

## 11. 当前仍需后续规划的能力

- 用户体系深化：刷新 token、用户资料修改、密码修改、手机号绑定等尚未完整实现。
- 管理后台闭环：商品、订单、秒杀活动、优惠券、评论、媒体和搜索索引能力已具备，后续重点是端到端验收、错误提示一致性和必要时引入 RBAC。
- 分类管理后台：当前前台已有分类树查询，后台分类新增/编辑/排序能力仍可单独规划。
- 支付系统：当前为 mock 支付，没有对接真实支付渠道。
- 售后体系：仅有 mock 退款，未形成售后工单、退货、审核等链路。
- 物流履约深化：当前已支持发货和确认收货，尚未对接真实物流轨迹。
- 观测体系：已有 `X-Trace-Id` 生成、Feign 透传、MQ Trace 上下文和 MDC，尚未形成完整链路追踪、指标监控与告警面板。
- 压测范围：已有 k6 秒杀相关脚本，普通交易全链路压测仍可继续补充。
- 数据迁移治理：CI 和本地 seed 已经很关键，后续需要明确迁移脚本规范；是否正式引入 Flyway/Liquibase 放到 Phase 24 后半段评估。

## 12. 后续开发建议

1. 优先保持 `scripts/ci/e2e.sh` 已跑通的全链路稳定。
2. Phase 24 第一优先级是后台闭环、CI 稳定化、接口边界和数据初始化治理，不急于新增散点业务能力。
3. 新增能力时按当前模块边界扩展，不要按旧文档把项目误判为停留在 Phase 3、Phase 8、Phase 16 或 Phase 21；当前总阶段仍是 Phase 23。
4. 涉及订单状态、库存、优惠券、评价互动、MQ 消费的改动，必须优先考虑条件更新、唯一约束和幂等。
5. 普通交易链路不要为了“万级 QPS”过度套用秒杀方案；秒杀链路继续保留 Redis Lua + MQ 削峰的专用设计。
