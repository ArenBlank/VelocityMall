# VelocityMall 开发规范与架构蓝图（实战务实版）

> 本文档是 VelocityMall（极速商城）的全局架构契约、编码规范与后续 AI 协作准则。
> 后续任何需求分析、代码生成、重构、排错与架构调整，都必须优先阅读并遵守本文档。

## 0. AI 协作核心守则

### 0.1 开发前置动作

从本文档创建后，AI 在修改或生成任何项目代码前，必须优先回溯并阅读 `DEVELOPMENT_SPEC.md`，再结合当前代码状态判断是否可以执行。

每次用户给出新方案命令时得先思考一下用户给的方案依照现有项目会不会存在问题，存在的问题的话就得思考出新的方案和解决方案或替代方案给用户，重点是不要附和无脑执行，要先会审视用户的需求是否合理。

如果用户提出的新需求与本文档冲突，AI 不能静默执行，必须先输出：

- 具体冲突点。
- 按用户方案执行的收益与风险。
- 按本文档方案执行的收益与风险。
- 推荐方案与理由。

只有在用户明确决策后，AI 才能继续编写或修改代码。

### 0.2 务实优先原则

VelocityMall 是高质量求职实战项目，不是炫技型架构堆砌项目。

所有设计必须遵循：

- 能在单机或小规模集群上优雅运行。
- 能体现国内一线互联网工程规范。
- 能解释清楚为什么这样设计、解决什么问题、代价是什么。
- 不盲目假设百万级并发，不为了中间件而中间件。
- 不提前引入会阻碍本阶段启动与测试的复杂依赖。

### 0.3 拦截与预警机制

以下情况必须主动预警：

- 用户要求的技术方案明显过度设计。
- 用户要求绕过事务、幂等、限流、缓存一致性等关键约束。
- 用户要求破坏包结构、命名、数据库字段、接口风格等强制规范。
- 用户要求把秒杀链路的极端方案套用到普通业务链路。
- 用户要求在当前 Phase 提前接入尚未准备好的中间件，导致启动、测试或教学节奏受阻。

AI 应先给出架构判断，再等待用户确认。

## 1. 项目定位与核心愿景

VelocityMall 是一个模拟真实中型垂直电商场景的分布式微服务系统。

项目重点不是通过堆砌物理机器应对虚构的极端流量，而是展现扎实的企业级代码治理素养。系统目标是解决中等并发场景下的典型业务痛点，常规业务链路按千级 QPS 进行设计，核心读链路与秒杀入口可按数千级 QPS 进行压测演示，交易写链路以稳定性、一致性和可补偿为优先目标。

核心技术价值包括：

- 高并发读场景下的多级缓存治理。
- 瞬时并发写场景下的防超卖、限流与削峰。
- 分布式环境下订单状态流转、消息可靠投递与兜底补偿。
- 清晰的领域分层、数据建模、异常治理、参数校验与测试策略。
- 面向面试讲解的可运行、可演示、可扩展工程。

## 2. 技术栈与版本基线

### 2.1 基础技术栈

- JDK：17。
- 构建工具：Maven。
- 核心框架：Spring Boot 3.2.x。
- 微服务体系：Spring Cloud 2023.0.x。
- Alibaba 微服务组件：Spring Cloud Alibaba 2023.0.x。
- ORM：MyBatis-Plus 3.5.x。
- 数据库：MySQL 8.x。
- 缓存：Redis，按阶段接入。
- 消息队列：RocketMQ，按阶段接入。
- 网关：Spring Cloud Gateway。
- 注册发现：Nacos，按阶段接入。
- 搜索：Elasticsearch，商品搜索与索引重建。
- 分布式协调：Redisson，用于缓存治理与分布式锁。
- 流量治理：Sentinel，用于秒杀入口限流与降级验证。
- 对象存储：MinIO，用于商品封面等媒体资源。
- 前端：Vue 3 + Vite + Pinia + Vue Router + Axios。
- 压测：k6，用于秒杀、熔断、混合集群和 CI smoke 验证。

### 2.2 版本选择原则

版本选择以兼容矩阵为准，不盲目追最新。

如果未来升级 Spring Boot、Spring Cloud 或 Spring Cloud Alibaba，必须同步检查三者版本兼容关系，不能只改一个版本号。

## 3. 全局里程碑与当前阶段

当前项目总阶段为 **Phase 23：3 节点高可用极限压榨与自动闭环**。旧版文档中的局部 Phase 编号只作为历史功能切片索引，不再代表项目当前完成度。

| 全局阶段 | 核心结论 | 当前状态 |
| --- | --- | --- |
| Phase 1 ~ 20：企业级微服务电商基座构建 | 完成 User、Product、Order、Seckill 等核心微服务链路；全面接入 Spring Cloud Alibaba 体系，整合 Nacos、RocketMQ、Redis、Sentinel；落地 JWT 鉴权传递、分布式事务或最终一致性基础方案。 | 已完成 |
| Phase 21：秒杀系统单点高并发防线与基准压测 | 引入 Redis Lua 脚本保障库存扣减绝对原子性；引入 RocketMQ 实现秒杀订单异步削峰落库；使用 k6 完成单机基准测试，确立单体承载基线。 | 已完成 |
| Phase 22：混合部署集群方案确立 | 基于本地物理机资源限制，确立“中间件容器化 + 业务进程宿主机直跑”的混合部署方案；Docker 管理 MySQL、Redis、Nacos、RocketMQ、Nginx、Elasticsearch；Windows 宿主机通过 `--server.port` 启动多 Java 实例，Docker Nginx 通过 `host.docker.internal` 转发流量。 | 已完成 |
| Phase 23：3 节点高可用极限压榨与自动闭环 | 成功拉起 Gateway x3 + Seckill x3 高可用集群；1500 VUs 下达到 **2142 QPS**，p50 延迟低至 **7.59ms**；1000 件库存绝对零超卖；`cleanup_server.py` 已实现 setup/teardown 数据闭环；Windows TCP Ephemeral Port 耗尽被识别为本地 OS 层瓶颈。 | **当前最新** |
| Phase 24：文档对齐、后台闭环与 CI 稳定化 | 规划把已实现的 Admin/Admin Web、Full Chain E2E、CI k6 smoke、接口边界和数据初始化固化为稳定基座；第一轮优先同步文档，不新增业务 API。 | 下一阶段规划 |

Phase 23 之后，当前代码继续补齐了 C 端买家前台、管理后台前台、Admin 全量管理接口、MinIO 商品封面、数据库驱动的秒杀活动配置与未开始活动演示商品；这些属于 Phase 23 之后的工程对齐补强，不改变当前总阶段编号。

### 3.1 历史功能切片索引（非当前总阶段）

以下细分条目保留为早期开发验收索引，用于说明能力如何逐步落地。它们不是当前项目总阶段，后续判断项目状态必须以 Phase 23 为准。

### Phase 1：工程骨架

目标：

- 建立 Maven 父子工程。
- 建立公共模块、网关模块、商品模块、订单模块。
- 保证无中间件启动条件下可以编译通过。

验收标准：

- `mvn -q -DskipTests package` 成功。
- Gateway 不引入 Spring MVC starter。
- 公共响应体与全局异常处理基础结构可编译。

### Phase 2：数据库建模与领域层

目标：

- 建立核心业务 DDL。
- 建立实体类、Mapper 与 MyBatis-Plus 基础配置。
- 抽取公共实体模型。

验收标准：

- 商品、订单核心表结构具备主键、审计字段、逻辑删除字段。
- 需要并发控制的核心状态或库存表具备乐观锁字段。
- 实体类继承公共实体，Mapper 继承 `BaseMapper<T>`。
- `mvn -q -DskipTests package` 成功。

### Phase 3：基础业务 API

目标：

- 建立商品查询、订单创建、订单查询等基础 API。
- 引入 DTO、VO、参数校验与统一异常处理。
- 完成基础业务单元测试或轻量集成测试。

验收标准：

- Controller 不包含核心业务逻辑。
- Service 负责业务流程编排。
- 涉及多表写操作的方法必须使用 `@Transactional`。
- API 返回统一 `Result<T>`。

### Phase 4：缓存治理

目标：

- 商品详情接入 Redis 缓存。
- 建立缓存 Key 规范、序列化规范、TTL 随机过期策略。
- 处理缓存穿透、击穿与热点数据回源保护。

验收标准：

- RedisTemplate 使用统一序列化器。
- 商品详情查无数据时写入短 TTL 空对象。
- 热点回源使用 Redisson 分布式锁或等价保护机制。

### Phase 5：订单异步化与消息可靠性

目标：

- 接入 RocketMQ。
- 下单、延时关单、库存释放、支付回调建立事件驱动链路。
- 完成消费者幂等与异常重试策略。

验收标准：

- 消息携带全局唯一 BusinessId 与 TraceId。
- 消费者侧具备幂等防重。
- 延时关单不影响已支付订单。
- 支付回调重复通知不会导致状态错乱。

### Phase 6: API 网关与统一身份鉴权（已完成）

目标：

- 接入 Spring Cloud Gateway 作为统一入口。
- 建立全局 JWT 黑白名单过滤机制。
- 对商品库存锁定、释放等内部接口建立网关黑名单，禁止外部请求直接访问。
- 通过网关注入可信 `X-User-Id` 请求头，并在订单服务中使用 `ThreadLocal` 用户上下文透传。

验收标准：

- Gateway 使用 WebFlux，不引入 Spring MVC。
- 商品公开查询接口可匿名 `GET` 访问，内部库存接口外部访问返回 `403`。
- 订单创建接口必须携带合法 JWT，订单用户 ID 来自网关注入的可信上下文。
- 订单服务在请求完成后清理 `ThreadLocal`，避免用户上下文泄漏。

### Phase 7: 高并发秒杀核心链路

目标：

- 建立秒杀库存预热。
- 使用 Redis Lua 完成资格校验、防重复抢购、库存原子扣减。
- 使用 RocketMQ 削峰异步落库。

验收标准：

- 秒杀入口不先查 MySQL 判断库存。
- Redis Lua 保证库存扣减与用户占位原子性。
- 超卖为 0。
- 重复抢购被拦截。

### Phase 8/9：历史预留说明

说明：

- 当前代码主干未发现独立 Phase 8、Phase 9 的落地模块或增量 SQL。
- 以下编号仅作为旧版功能切片索引，不再作为项目总阶段判断依据；项目当前总阶段为 Phase 23。
- 用户评价与互动体系已在 Phase 16 落地，不再作为未实现候选功能描述。

### Phase 10：模拟支付回调与物理库存扣减闭环（已完成）

目标：

- 在订单服务提供兼容前端的模拟支付成功接口，并补齐模拟第三方回调入口。
- 通过支付流水、HMAC 回调验签、金额校验、订单状态条件更新、事务提交后发送 MQ、商品服务本地事务扣减真实库存，形成支付闭环。
- 使用 MySQL 本地消费防重表保证 Product 消费幂等。

验收标准：

- `POST /api/v1/orders/pay/mock` 可将待支付订单流转为已支付，内部必须创建/复用 `oms_payment_transaction` 支付流水。
- `POST /api/v1/orders/pay/mock/callback` 不要求用户 JWT，但必须验签；错误签名返回业务码 `40300`。
- `oms_order.pay_time` 正确写入。
- `payment-success-topic` 只在订单事务提交后发送。
- Product 消费后扣减 `pms_sku.stock`、增加 `sale_count`。
- `mq_consume_log` 唯一索引防止重复扣减。

### Phase 11：购物车与普通订单物理锁库流转（已完成）

目标：

- 在订单服务实现 Redis Hash 购物车。
- 普通订单提交时调用 Product 内部批量锁库接口。
- 通过 `pms_stock_lock_log` 记录普通订单库存锁定、释放、真实扣减生命周期。

验收标准：

- 支持购物车加入、查询、删除。
- 普通订单从购物车读取 SKU 数量并创建订单。
- Product 批量锁库按库存条件更新并写入锁定流水。
- 延时关单或取消订单可幂等释放锁定库存。
- 普通订单支付后锁流水 `0 -> 2`，真实库存与锁定库存同步扣减。

### Phase 12：C 端订单管理与逆向退款链路（已完成）

目标：

- 补齐用户订单分页、详情、手动取消、模拟退款接口。
- 普通订单取消释放 Product 锁库，秒杀订单取消回补 Redis 秒杀库存。
- 退款通过 MQ 通知 Product 幂等恢复真实库存与销量。

验收标准：

- 订单列表和详情只能查看当前用户订单。
- 待支付订单可取消，状态流转为已取消。
- 已支付订单可 mock 退款，状态流转为已退款，内部必须创建/复用 `oms_payment_transaction` 退款流水。
- `POST /api/v1/orders/refund/mock/callback` 不要求用户 JWT，但必须验签。
- `order-refund-topic` 消费具备本地事务和幂等防重。
- 退款只恢复 `stock` 与 `sale_count`，不触碰普通订单 `lock_stock`。

### Phase 13：高性能商品搜索与异构数据同步（已完成）

目标：

- 新增 Search 服务，使用 Elasticsearch 承载商品搜索。
- Product 商品变更通过 RocketMQ 顺序消息同步 ES。
- 提供索引重建能力，支持历史商品冷启动。

验收标准：

- `velocity-mall-search` 服务可独立启动并注册到 Nacos。
- `GET /api/v1/search/skus` 支持关键词、分页、排序查询。
- `POST /api/v1/search/inner/skus/rebuild-index` 支持内部索引重建。
- Product 上下架或 SKU 更新后发送 `product-sync-topic`。
- Gateway 放行公开搜索接口，拦截 `/api/v1/search/inner/**`。

### Phase 14：首页多级分类树与高并发缓存防线（已完成）

目标：

- 在 Product 服务提供三级商品分类树查询。
- 使用 Redis JSON 缓存分类树。
- 通过空数组短 TTL、Redisson 分布式锁、Double Check、随机 TTL 抖动解决穿透、击穿和雪崩风险。

验收标准：

- `GET /api/v1/categories/tree` 可匿名访问。
- `pms_category.status` 支持启用/禁用过滤。
- 分类树一次性查库后在内存组装，避免递归 N+1 查询。
- 缓存命中直接返回，缓存未命中时加锁回源并二次检查。
- Gateway 新增 `/api/v1/categories/**` 路由和分类树白名单。

### Phase 15：营销域优惠券系统与高并发抢券（已完成）

目标：

- 新增 Coupon 服务，提供优惠券领取接口。
- 使用 Redis `INCR` 做用户限领前置拦截。
- 使用 MySQL `stock > 0 + version` 乐观锁扣减兜底，避免超发。

验收标准：

- `POST /api/v1/coupons/{coupon-id}/claim` 必须登录。
- `sms_coupon` 与 `sms_coupon_history` 支持优惠券基础数据和领取流水。
- Redis 限领 Key TTL 覆盖到券结束后。
- MySQL 乐观锁有限重试，失败时补偿 Redis 计数。
- 同一用户超过限领次数不重复扣减库存。

### Phase 16：用户评价与互动体系（已完成）

目标：

- 建立商品评价发布、删除与分页查询能力。
- 支持用户查看其他用户评价，并对评价进行点赞或点踩。
- 建立评价互动幂等、防重复操作与计数一致性策略。
- 为商品详情页提供评价数量、好评率或互动统计等可展示指标。

验收标准（已实现）：

- 只有已完成订单的购买用户可以发布对应商品评价（通过 Feign 调用订单服务内 `/api/v1/orders/inner/check-purchase` 校验；当前代码要求订单状态为 `3`）。
- 同一用户对同一订单同一 SKU 只能发布一条有效评价（数据库唯一约束 `uk_user_order_sku` + `DuplicateKeyException` 幂等防重）。
- 评价删除必须校验评价归属，禁止用户操作他人评价。
- 评价列表支持按 SPU 分页查询，返回的 `ReviewVO` 不暴露 `userId`，但会携带 `mine` 标记用于前端只给本人评价展示删除入口。
- 点赞和点踩互斥并具备幂等防重，同一用户对同一评价只能保持一种互动状态（唯一约束 `uk_review_user` + 物理删除/类型切换）。
- 点赞数、点踩数等计数字段使用原子 `UPDATE` 且减操作带 `> 0` 条件防止负数。
- 评价统计（总数、好评数、好评率）使用 Redis + Redisson 分布式锁 + 双重检查缓存，TTL 带随机抖动。

### Phase 17：历史预留说明

说明：

- 当前代码主干未发现独立 Phase 17 的落地模块或增量 SQL。
- Phase 17 仅作为历史编号缺口保留，不作为当前项目完成度或下一步规划依据。

### Phase 18：用户注册登录与 JWT 体系（已完成）

目标：

- 新增 User 服务，提供用户注册、登录和当前用户信息查询。
- 使用 BCrypt 存储密码密文。
- 登录成功后签发包含 `userId` 的 JWT，由 Gateway 解析后注入 `X-User-Id`。

验收标准（已实现）：

- `POST /api/v1/users/register` 可创建用户，用户名唯一。
- `POST /api/v1/users/login` 校验 BCrypt 密码并返回 JWT 与用户基本信息。
- `GET /api/v1/users/me` 通过 `UserContext` 查询当前用户。
- Gateway 对注册和登录接口放行，对其他用户接口要求合法用户 JWT。

### Phase 19：收货地址与订单地址快照（已完成）

目标：

- 建立用户收货地址表和地址 CRUD。
- 普通订单创建时必须携带 `addressId`，订单服务通过 User Feign 拉取地址并保存订单地址快照。

验收标准（已实现）：

- `POST/PUT/DELETE/GET /api/v1/users/addresses` 支持当前用户地址维护。
- 默认地址设置会清理同一用户已有默认地址。
- `GET /api/v1/users/inner/addresses/{id}` 按 `userId` 校验地址归属，仅供服务间调用。
- `oms_order` 保存收货人、手机号、省市区和详细地址快照。

### Phase 20：订单履约基础链路（已完成）

目标：

- 支持管理员发货与 C 端用户确认收货。
- 订单状态从已付款流转到已发货，再由当前用户确认到已完成。

验收标准（已实现）：

- `POST /api/v1/orders/inner/{order-sn}/deliver` 条件更新已付款订单为已发货，写入物流公司、物流单号和发货时间。
- `PUT /api/v1/orders/{order-sn}/confirm-receipt` 仅允许订单归属用户将已发货订单确认收货。
- `oms_order` 保存 `delivery_company`、`delivery_sn`、`delivery_time`、`receive_time`。

### 旧版功能切片 21：管理后台基础能力（已完成，非全局 Phase 21）

目标：

- 新增 Admin 服务，提供管理员登录、订单发货和商品上下架入口。
- 当前代码已继续扩展为后台商品 SPU/SKU 管理、SKU 封面上传、秒杀活动管理/预热、优惠券管理、评论管理与搜索索引重建。
- 管理员 JWT 与普通用户 JWT 隔离，Gateway 解析 `adminId` 并注入 `X-Admin-Id`。

验收标准（已实现）：

- `POST /api/v1/admin/login` 校验管理员账号密码并返回 admin JWT。
- `POST /api/v1/admin/orders/{order-sn}/deliver` 通过 Feign 调用订单内部发货接口。
- `PUT /api/v1/admin/products/spus/{spu-id}/status?action=publish|unpublish` 通过 Feign 调用 Product 内部上下架接口。
- Admin 已提供 SPU/SKU 新增编辑、SKU 封面上传、订单查询、秒杀活动 CRUD/启停/预热、优惠券 CRUD/启停、评价删除和搜索索引重建入口。
- `velocity-mall-admin-web` 已提供登录、商品、订单、秒杀活动、优惠券、评论、媒体和搜索索引管理页面，并已纳入 CI 的 `Admin Web Build` smoke。
- Admin 服务拦截 `/api/v1/admin/**`，排除登录接口。

## 4. 工程目录、包结构与接口规约

每个微服务模块 `velocity-mall-xxx` 的基础包名固定为 `com.velocitymall.xxx`。

### 4.1 标准目录分层

每个业务服务模块应遵循以下结构：

```text
com.velocitymall.xxx
├── XxxApplication.java
├── controller
├── service
│   └── impl
├── mapper
├── entity
├── model
│   ├── dto
│   └── vo
├── config
├── enums
└── constant
```

职责约束：

- `controller`：仅负责 HTTP 路由映射、入参初步校验、调用 Service、返回结果，禁止编写核心业务逻辑。
- `service`：定义业务能力接口。
- `service.impl`：实现核心业务流程，涉及多表更新、库存锁定、订单状态变更时必须考虑事务边界。
- `mapper`：MyBatis-Plus 数据访问接口，默认不写 XML。
- `entity`：数据库映射实体，必须继承公共实体基类。
- `model.dto`：前端或外部系统传入的数据传输对象，配合 `@Valid`、`@NotNull`、`@NotBlank` 等校验。
- `model.vo`：返回给前端的视图对象，禁止直接把 Entity 暴露给外部 API。
- `config`：模块级配置。
- `enums`：状态枚举与业务枚举。
- `constant`：业务常量，避免魔法值散落。

### 4.2 RESTful API 命名规约

路径统一使用小写字母与中划线，禁止驼峰命名路径。

示例：

```text
GET    /api/v1/products/{sku-id}
POST   /api/v1/orders
GET    /api/v1/orders/{order-sn}
PUT    /api/v1/orders/{order-sn}/cancel
DELETE /api/v1/carts/items/{item-id}
```

HTTP 动词约束：

- `GET`：查询。
- `POST`：新增、提交、触发非幂等动作。
- `PUT`：整体更新、状态变更。
- `PATCH`：局部更新，谨慎使用。
- `DELETE`：逻辑删除。

### 4.3 Controller 规范

Controller 必须：

- 使用 `@Validated` 或在方法参数上使用 `@Valid`。
- 返回 `Result<T>`。
- 不直接操作 Mapper。
- 不直接写 Redis、RocketMQ、事务逻辑。
- 不吞异常，统一交给全局异常处理器。

Controller 禁止：

- 编写库存扣减逻辑。
- 编写订单状态机逻辑。
- 编写缓存回源逻辑。
- 拼接 SQL 或构造复杂查询条件。

### 4.4 Service 规范

Service 是业务流程编排核心。

必须使用 `@Transactional` 的场景：

- 一个业务方法内更新两张及以上业务表。
- 订单创建同时写订单主表与订单明细。
- 订单取消同时更新订单状态并释放库存。
- 支付成功同时更新订单状态、支付流水、库存最终确认。

不建议使用长事务的场景：

- 调用远程服务。
- 等待 MQ 发送结果。
- 执行耗时批处理。
- 执行缓存锁等待。

长流程应通过本地事务 + 消息事件 + 补偿任务拆解。

## 5. 数据库设计与 MyBatis-Plus 规约

### 5.1 基础数据库规范

MySQL 表设计必须遵守：

- 存储引擎使用 `InnoDB`。
- 字符集使用 `utf8mb4`。
- 表与字段必须添加中文 `COMMENT`。
- 主键统一为 `BIGINT`。
- 金额字段使用 `DECIMAL(18, 2)`，禁止使用 `FLOAT` 或 `DOUBLE`。
- 状态字段使用 `TINYINT` 或 `INT`，并在注释中写清枚举含义。
- 核心查询字段必须建立索引。
- 唯一业务号必须建立唯一索引。

### 5.2 公共实体抽取

公共实体必须放在 `velocity-mall-common` 模块：

```text
com.velocitymall.common.entity.BaseEntity
com.velocitymall.common.entity.VersionedEntity
```

`BaseEntity` 包含所有表通用字段：

- `id`：`BIGINT`，使用 `@TableId(type = IdType.ASSIGN_ID)`。
- `create_time`：使用 `@TableField(fill = FieldFill.INSERT)`。
- `update_time`：使用 `@TableField(fill = FieldFill.INSERT_UPDATE)`。
- `is_deleted`：使用 `@TableLogic`，0 为未删除，1 为已删除。

`VersionedEntity` 继承 `BaseEntity`，仅用于核心状态或库存敏感表：

- `version`：使用 `@Version`。

必须继承 `VersionedEntity` 的典型表：

- `pms_sku`：库存、锁定库存会发生并发修改。
- `pms_spu`：上架状态属于核心状态。
- `oms_order`：订单状态流转需要防止并发覆盖。

不强制继承 `VersionedEntity` 的典型表：

- `pms_category`：分类表不属于高频并发状态表。
- `oms_order_item`：订单明细一般为创建后只读快照。

### 5.3 Lombok 与实体继承规范

普通实体可使用：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
```

如果实体继承 `BaseEntity` 或 `VersionedEntity`，并且确实需要 Builder，必须使用 Lombok 的 `@SuperBuilder`，禁止使用普通 `@Builder`，避免父类字段无法参与构造。

建议：

- Entity 层不强制使用 Builder。
- DTO、VO 可按需使用 Builder。
- 领域对象字段较多且测试构造频繁时，才考虑 `@SuperBuilder`。

### 5.4 MyBatis-Plus 插件规范

公共配置类放在：

```text
com.velocitymall.common.config.MyBatisPlusConfig
```

必须注册：

- `PaginationInnerInterceptor(DbType.MYSQL)`：分页插件。
- `OptimisticLockerInnerInterceptor`：乐观锁插件。

应用服务需要扫描公共配置。若启动类不在 `com.velocitymall` 根包下，必须设置：

```java
@SpringBootApplication(scanBasePackages = "com.velocitymall")
```

### 5.5 Mapper 规范

Mapper 接口必须：

- 放在 `com.velocitymall.xxx.mapper` 包下。
- 继承 `BaseMapper<T>`。
- 添加 `@Mapper` 注解。

默认不编写 XML。只有以下情况才允许新增 XML：

- 多表复杂聚合查询。
- 动态 SQL 过于复杂，Wrapper 可读性明显下降。
- 需要精细控制查询字段、索引 hint 或执行计划。

## 6. 中间件命名与底层配置规范

### 6.1 Redis Key 命名

Key 格式：

```text
项目名:模块名:业务线:唯一标识
```

示例：

```text
velocitymall:product:spu:{spuId}
velocitymall:product:sku:{skuId}
velocitymall:product:lock:{skuId}
velocitymall:order:submit-token:{userId}:{token}
velocitymall:seckill:stock:{skuId}
velocitymall:seckill:bought:{skuId}
```

禁止：

- 使用无项目名前缀的 Key。
- 使用中文 Key。
- 使用含义不明的缩写。
- 在业务代码中散落字符串 Key，必须集中到常量类或 Key 构造器。

### 6.2 Redis TTL 防雪崩策略

写入普通业务缓存时，必须使用：

```text
基础过期时间 + 随机波动时间
```

示例：

```text
30 分钟 + 1 到 5 分钟随机值
```

目的：

- 避免大量热点 Key 同一时刻过期。
- 降低缓存雪崩风险。

### 6.3 Redis 序列化规范

`RedisTemplate` 必须配置统一序列化器：

- Key：`StringRedisSerializer`。
- Hash Key：`StringRedisSerializer`。
- Value：`GenericJackson2JsonRedisSerializer`。
- Hash Value：`GenericJackson2JsonRedisSerializer`。

禁止使用 JDK 默认序列化。

### 6.4 RocketMQ 命名规范

Topic 格式：

```text
业务模块-动作-topic
```

示例：

```text
order-create-topic
order-close-delay-topic
seckill-order-topic
stock-release-topic
payment-success-topic
```

Consumer Group 格式：

```text
业务模块-动作-consumer-group
```

示例：

```text
order-close-consumer-group
seckill-order-create-consumer-group
stock-release-consumer-group
```

Producer Group 格式：

```text
业务模块-producer-group
```

示例：

```text
velocity-mall-order-producer-group
```

### 6.5 RocketMQ 消费幂等

所有消息必须携带：

- `businessId`：业务唯一标识。
- `traceId`：链路追踪标识。
- `eventType`：事件类型。
- `eventTime`：事件发生时间。

消费者必须实现幂等，不得只依赖 RocketMQ 的重试机制。

可选实现：

- 数据库唯一索引防重。
- Redis `SET NX` 防重。
- 独立消息消费记录表。

核心业务建议使用数据库唯一索引或消费记录表兜底，Redis 防重只能作为性能优化，不应作为唯一可靠依据。

## 7. 核心工程场景落地方案

### 7.1 场景一：商品详情页多级缓存

#### 技术卡点

商品详情属于典型高频读场景。如果每次查询直接访问 MySQL，热点商品会迅速耗尽数据库连接池。

#### 落地规范

缓存读取流程：

1. 查询 Redis。
2. Redis 命中且不是空对象，直接返回。
3. Redis 命中空对象，返回商品不存在。
4. Redis Miss，尝试获取分布式锁。
5. 获取锁成功后二次查询 Redis。
6. 二次确认仍 Miss，回源 MySQL。
7. MySQL 查到数据，写入 Redis，并设置随机 TTL。
8. MySQL 查不到数据，写入短 TTL 空对象。

#### 防穿透

查无此商品时，在 Redis 存入空 JSON：

```json
{}
```

TTL 建议 60 秒以内。

#### 防击穿

热点 Key 失效回源时，使用 Redisson 分布式锁：

```java
redissonClient.getLock("velocitymall:product:lock:" + skuId)
```

要求：

- 使用 `tryLock`。
- 设置合理的 `waitTime`，例如 1 秒。
- 设置合理的 `leaseTime`，避免死锁。
- 只有获取锁的线程回源 MySQL。
- 未获取锁的线程可短暂重试、返回降级数据或提示稍后重试。

#### 防雪崩

所有商品缓存 TTL 都必须加随机波动。

### 7.2 场景二：普通下单库存锁定

普通下单不等于秒杀，不应强行套用 Redis Lua。

#### 推荐方案

普通订单锁库存可以使用 MySQL 条件更新：

```sql
UPDATE pms_sku
SET lock_stock = lock_stock + #{quantity},
    version = version + 1
WHERE id = #{skuId}
  AND is_deleted = 0
  AND stock - lock_stock >= #{quantity};
```

判断影响行数：

- 影响行数为 1：锁库存成功。
- 影响行数为 0：库存不足或并发冲突。

#### 事务边界

创建订单时：

- 写订单主表。
- 写订单明细表。
- 锁定库存。

必须在本地事务内完成。

若未来商品服务与订单服务拆成物理独立库，可通过库存服务接口、消息补偿或 TCC/Saga 思路演进，当前阶段不提前引入复杂分布式事务框架。

### 7.3 场景三：秒杀防超卖

#### 技术卡点

秒杀模拟单品少量库存、短时间大量用户抢购。瓶颈不在订单表写入，而在入口瞬间并发与库存判断。

#### 落地规范

秒杀入口禁止先查 MySQL 判断库存。允许读取 `sms_seckill_activity` 校验活动是否启用、是否到达开始时间、是否已结束，但库存判断和扣减必须始终由 Redis Lua 原子完成。

标准流程：

1. 后台任务读取启用且进入有效窗口的秒杀活动，将活动库存预热到 Redis。
2. 用户发起抢购请求。
3. 后端先校验活动状态；未开始、已结束、禁用直接返回业务提示。
4. 活动可抢购时执行 Redis Lua 脚本。
5. Lua 内原子完成资格校验与库存扣减。
6. Lua 成功后发送 RocketMQ 消息。
7. 接口返回统一 `Result<String>`，成功消息表示“秒杀成功，正在排队中...”。
8. 消费者按数据库可承受速率异步创建订单。

Lua 脚本必须原子完成：

1. 验证用户是否已抢购。
2. 验证库存是否大于 0。
3. 执行 `DECR` 扣减库存。
4. 执行 `SADD` 记录当前用户 ID。

消息 Topic：

```text
seckill-order-topic
```

消息体必须包含：

- `skuId`。
- `userId`。
- `quantity`。
- `orderSn`。
- `businessId`。
- `traceId`。

#### 失败处理

如果 MQ 发送失败：

- 必须记录失败日志。
- 必须考虑恢复 Redis 库存与用户占位。
- 更稳妥方案是使用本地消息表或事务消息，在后续阶段引入。

当前求职项目可先实现可靠日志 + 补偿任务，不提前上复杂事务消息方案。

### 7.4 场景四：可靠订单流转与分布式事务兜底

#### 订单状态机

订单状态必须单向、可解释、可幂等。

建议状态：

```text
0 待付款
1 已付款
2 已发货
3 已完成
4 已关闭/超时取消
5 已退款
```

允许流转：

```text
待付款 -> 已付款
待付款 -> 已关闭/超时取消
已付款 -> 已退款
已付款 -> 已发货
已发货 -> 已完成
```

禁止流转：

```text
已付款 -> 待付款
已关闭 -> 已付款
已完成 -> 已关闭
已关闭 -> 已退款
已退款 -> 已付款
```

#### 短幂等键防重提交

提交订单前，前端或后端生成 UUID token。

后端在提交订单时使用 Redis：

```text
SET velocitymall:order:submit-token:{userId}:{token} value EX 30 NX
```

结果：

- 成功：允许继续创建订单。
- 失败：判定为重复提交，直接返回友好提示。

该机制用于短时间防重复点击，不替代数据库唯一索引与业务幂等。

#### 唯一订单号

订单号 `order_sn` 必须全局唯一，并建立唯一索引。

重复创建订单时，应通过业务幂等键或唯一索引避免重复数据。

#### 延时关单

订单创建成功后发送延时消息：

```text
order-close-delay-topic
```

消费者收到延时消息后：

1. 根据 `orderSn` 查询订单。
2. 如果订单不存在，记录日志并结束。
3. 如果订单仍为待付款，执行关闭订单。
4. 如果订单已支付，直接忽略。
5. 如果订单已关闭，幂等结束。

关闭订单时必须释放锁定库存。

#### 支付回调幂等

支付平台可能重复通知，支付成功回调必须幂等。

处理规则：

1. 根据支付流水号或支付平台交易号建立唯一索引。
2. 收到回调后先校验签名。
3. 校验回调金额必须等于订单应付金额和流水金额。
4. 查询订单当前状态。
5. 若订单已支付、已发货、已完成或已退款，直接返回成功，避免支付平台继续重试；不得重复发送库存扣减 MQ。
6. 若订单待付款，执行状态更新：`待付款 -> 已付款`。
7. 若订单已关闭，需要进入人工核查或退款流程，不能静默改为已付款。

状态更新必须带条件：

```sql
UPDATE oms_order
SET status = 1,
    version = version + 1
WHERE order_sn = #{orderSn}
  AND status = 0
  AND is_deleted = 0;
```

通过影响行数判断是否更新成功。

#### 库存释放幂等

库存释放可能来自：

- 用户主动取消订单。
- 延时关单。
- 支付失败补偿。
- 消息重试。

必须保证重复释放不会导致库存异常。

推荐方案：

- 订单状态先从待付款改为已关闭。
- 只有状态更新成功的线程才释放库存。
- 库存释放记录建立唯一键，避免重复释放。

## 8. 统一异常、返回体与校验规范

### 8.1 返回体

所有 HTTP API 返回统一结构：

```json
{
  "code": 20000,
  "message": "操作成功",
  "data": {}
}
```

禁止 Controller 直接返回 Map、String 或 Entity。

### 8.2 异常处理

公共异常处理放在：

```text
com.velocitymall.common.exception.GlobalExceptionHandler
```

必须覆盖：

- 参数校验异常。
- 参数绑定异常。
- 业务异常。
- 未知系统异常。

未知异常不能把堆栈返回给前端。

### 8.3 业务异常

建议后续新增：

```text
com.velocitymall.common.exception.BusinessException
com.velocitymall.common.result.ResultCode
```

业务异常必须包含：

- 错误码。
- 错误消息。
- 必要上下文日志。

## 9. 日志与链路追踪规范

### 9.1 日志原则

日志必须帮助定位问题，而不是制造噪音。

必须记录：

- 订单创建失败。
- 库存扣减失败。
- MQ 发送失败。
- MQ 消费失败。
- 支付回调验签失败。
- 分布式锁获取超时。

禁止记录：

- 明文密码。
- 完整支付敏感信息。
- 大体积请求体。
- 无意义循环日志。

### 9.2 TraceId

核心链路必须携带 TraceId：

- HTTP 请求入口生成或透传 TraceId。
- MQ 消息体携带 TraceId。
- 消费者日志打印 TraceId。

后续可通过网关过滤器或拦截器统一处理。

## 10. 配置与环境规范

### 10.1 application.yml

配置文件中允许保留本地默认值，但不能写入真实生产密码。

本地默认值示例：

```yaml
spring:
  datasource:
    username: root
    password: root
```

生产环境必须通过环境变量、配置中心或启动参数注入。

### 10.2 Profile

后续应逐步拆分：

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

当前阶段可先保留单一配置文件，避免过早复杂化。

### 10.3 本地开发红线：混合部署

本项目在本地开发、E2E 和性能压测中必须采用“混合部署”模式，尤其是集群压测场景：

- **中间件使用 Docker**：MySQL、Redis、Nacos、RocketMQ、Nginx、Elasticsearch 等基础设施由 Docker Compose 启动。
- **Java 微服务使用宿主机 JDK 17**：Gateway、Seckill、Order、Product 等所有业务代码必须直接在宿主机启动，使用本机 JDK 17 运行。
- **禁止容器化 Java 服务**：严禁在用于本地开发或压测的 `docker-compose` 文件中为 Java 微服务新增 `image` 或 `build` 指令，严禁尝试把 Java 服务塞进 Compose 统一容器化启动，避免镜像拉取错误、JDK 镜像版本和网络映射问题干扰开发与压测。
- **本地集群扩容方式**：通过启动多个 Java 进程并指定不同端口实现，例如 Gateway 使用 `8080/8090/8091`，Seckill 使用 `8083/8093/8094`。
- **流量转发方式**：Docker Nginx 只负责入口负载均衡，通过 `host.docker.internal` 转发到宿主机 Gateway 多实例；Gateway 到 Seckill 的多实例分发交给 Nacos 服务发现与负载均衡。
- **标准集群 Compose**：本地集群压测优先使用 `docker/docker-compose-cluster.yml`，它只包含中间件与 Nginx，不应加入 Java 应用服务。
- **CI E2E Compose**：`docker/docker-compose.e2e.yml` 只包含 Full Chain E2E 必需的 MySQL、Redis、Nacos、RocketMQ、Elasticsearch；MinIO/Nginx 属于本地开发、演示和集群压测形态，不是 CI E2E 的必需容器。

示例启动思路：

```bash
docker compose -f docker/docker-compose-cluster.yml up -d

java -jar velocity-mall-gateway/target/velocity-mall-gateway-1.0.0-SNAPSHOT.jar --server.port=8080
java -jar velocity-mall-gateway/target/velocity-mall-gateway-1.0.0-SNAPSHOT.jar --server.port=8090
java -jar velocity-mall-gateway/target/velocity-mall-gateway-1.0.0-SNAPSHOT.jar --server.port=8091

java -jar velocity-mall-seckill/target/velocity-mall-seckill-1.0.0-SNAPSHOT.jar --server.port=8083
java -jar velocity-mall-seckill/target/velocity-mall-seckill-1.0.0-SNAPSHOT.jar --server.port=8093
java -jar velocity-mall-seckill/target/velocity-mall-seckill-1.0.0-SNAPSHOT.jar --server.port=8094
```

## 11. 测试与验收规范

### 11.1 编译基线

每次结构性改动后必须至少执行：

```bash
mvn -q -DskipTests package
```

### 11.2 单元测试

Service 层核心逻辑应优先写单元测试。

优先覆盖：

- 库存不足。
- 重复提交。
- 订单状态非法流转。
- 支付回调重复通知。
- 秒杀重复抢购。

### 11.3 集成测试

涉及 MyBatis-Plus、Redis、RocketMQ 的功能，按阶段引入集成测试。

当前阶段不强制引入 Testcontainers，除非测试收益明显高于维护成本。

### 11.4 性能测试闭环

所有秒杀、集群和大规模性能测试必须使用 `scripts/performance/cleanup_server.py` 形成数据闭环，严禁污染业务数据库：

- 压测前必须启动清理服务：`python scripts/performance/cleanup_server.py`，默认监听 `8099`。
- 压测前必须调用 `POST /setup`：重置 SKU 2001 库存为 1000、清理历史订单和消费日志、预热 Redis 秒杀库存。
- 压测后必须调用 `POST /cleanup`：清理 `lt_u` 压测用户、订单、库存流水、消费日志、Redis 秒杀 Key 和提交幂等 Key。
- 压测报告必须记录本轮是否执行 `/setup` 和 `/cleanup`，否则库存成功数、超卖结论和 QPS 不可作为有效结论。
- 性能压测必须区分应用瓶颈与宿主机瓶颈。Windows 下若出现大量 `Connection Refused`，优先按 TCP 临时端口/连接资源耗尽分析，不得误判为 Redis Lua 超卖或业务代码异常。
- CI k6 smoke 是防回归验证，允许在一次性 CI 环境中通过 workflow seed 和 Redis 预热准备数据；该模式必须保持幂等，不替代本地 `cleanup_server.py` 闭环或 Gateway x3 + Seckill x3 + Nginx 极限压测。

## 12. 代码质量红线

禁止：

- Controller 直接调用 Mapper。
- 业务代码中硬编码 Redis Key。
- 金额使用浮点类型。
- Entity 直接作为外部接口响应。
- 消费者不做幂等。
- 订单状态无条件更新。
- 秒杀入口先查 MySQL 判断库存。
- 普通业务强行使用秒杀链路。
- 为展示技术而提前引入不可运行依赖。
- 在本地开发或压测 Compose 中为 Java 微服务新增 `image` 或 `build` 指令。
- 为了“统一部署”把 Java 微服务容器化，破坏当前宿主机 JDK 17 + Docker 中间件的混合部署基线。
- C 端前台展示没有真实后端字段或接口承载的伪导航、伪定位、下载 App、伪规格、伪 SKU 选项。
- 在前端硬编码秒杀价、活动开始时间、活动结束时间或活动状态。
- 商品封面使用本机绝对路径、聊天截图路径或无法随部署发布的临时路径。
- 外部用户绕过 Gateway 直接访问 `/inner/**`、Admin 或后端服务端口。
- 新增服务间接口时混用 Public 路径语义，导致 Gateway 无法统一拦截内部能力。

必须：

- 命名清晰。
- 事务边界明确。
- 异常语义明确。
- 数据库字段有注释。
- 核心状态更新有条件。
- 并发敏感表使用乐观锁或条件更新。
- 每次代码变更后进行可执行验证。
- 本地集群扩容通过多个宿主机 Java 进程和 `--server.port` 完成，由 Docker Nginx 经 `host.docker.internal` 转发入口流量。
- 性能测试前后必须执行 `cleanup_server.py` 的 `/setup` 与 `/cleanup` 数据闭环，压测数据不得污染业务数据库。
- C 端前台的页面元素必须能映射到真实用户功能或真实后端数据；后端缺字段时，应先补后端或隐藏该 UI。
- 秒杀活动展示必须来自 `sms_seckill_activity` 与 Seckill Activity API；前端只做倒计时、状态渲染和交互锁定。
- 商品图片必须使用 MinIO 可访问 URL、后端 SKU 封面字段或随前端构建发布的静态资源；当前商品封面统一走 `/minio/velocity-mall-product/...`。
- 首页、秒杀专区和商品详情搜索/展示必须使用 `GET /api/v1/search/skus`、商品详情 API 和秒杀活动 API 合并，不得无命中时偷偷回退展示全部商品。
- 外部入口统一走 Gateway；服务间接口使用 `/inner/**`，并确认 Gateway 外部访问返回 403。
- Admin 接口统一使用 `/api/v1/admin/**`、管理员 JWT 和 `X-Admin-Id`；接口级权限由 Admin 服务内的 RBAC V1 负责，Gateway 不查询业务权限表。

## 13. 当前代码与后续规划提示

当前项目已完成 **Phase 23：3 节点高可用极限压榨与自动闭环**。总体能力包括：

- Maven 多模块结构（10 个后端模块：common、gateway、product、order、seckill、search、coupon、review、user、admin）与两个前端应用：`velocity-mall-web` C 端买家前台、`velocity-mall-admin-web` 管理后台前台。
- 公共响应体（`Result<T>`）、全局异常处理（`GlobalExceptionHandler`）、业务异常（`BusinessException` + `ResultCode`）。
- 公共实体 `BaseEntity` 与 `VersionedEntity`（雪花 ID、审计字段、逻辑删除、乐观锁）。
- MyBatis-Plus 分页插件与乐观锁插件。
- 商品、订单、优惠券、评价、用户、管理员完整 DDL 与实体/Mapper。
- RedisTemplate 统一序列化（`GenericJackson2JsonRedisSerializer`）。
- Redisson 分布式锁，缓存穿透/击穿/雪崩三防。
- RocketMQ 8 条消息链路 + 消费幂等（`mq_consume_log` 表）。
- Spring Cloud Gateway 全局 JWT 鉴权 + 白名单/黑名单 + Sentinel 秒杀限流。
- 普通订单两阶段库存锁定 + 秒杀 Redis Lua 原子扣减 + 异步 MQ 落库。
- `sms_seckill_activity` 已承载秒杀价、原价、开始/结束时间、活动库存和启用状态，Seckill 服务提供活动列表与按 SKU 查询接口。
- Elasticsearch 商品搜索 + MQ 异构同步 + 索引重建。
- 评价发布、分页查询、统计缓存、点赞/点踩互斥切换、已完成订单购买资格 Feign 校验。
- 用户注册/登录/JWT、当前用户信息、收货地址维护。
- 订单地址快照、后台发货、用户确认收货。
- 管理员登录、RBAC 权限校验、管理员/角色/授权管理、商品 SPU/SKU 管理、SKU 封面上传、订单发货、秒杀活动管理/预热、优惠券管理、评论管理、搜索索引重建。
- MinIO 商品封面已接入；当前默认封面位于 `/minio/velocity-mall-product/products/default-covers/`，未开始活动演示封面位于 `/minio/velocity-mall-product/products/seckill-demo/velocity-phone-future-2002.png`。
- C 端买家前台已覆盖登录/注册、首页搜索、商品详情、秒杀抢购、排队结果、购物车、地址、优惠券、订单、mock 支付、退款/确认收货、评价展示与互动。
- 管理后台前台已覆盖登录、RBAC 管理、按权限菜单/按钮控制、商品、订单、秒杀活动、优惠券、评论、媒体资源和搜索索引运维。
- 当前演示数据包含进行中 SKU 2001 与未开始演示 SKU 2002；SKU 2002 的活动开始时间为 `2027-06-01 10:00:00`，用于验证“距离活动开始”长倒计时与到点自动切换。
- `scripts/ci/e2e.sh` 已覆盖 product、order、seckill、search、coupon、gateway、user、admin、review 九个应用的全链路验证。
- GitHub Actions 已包含 Maven Build、Admin Web Build、Full Chain E2E 与 `k6 Load & Chaos Tests (CI Mode)`；Admin Web Build 使用 Node.js 24 执行 `npm ci` 与 `npm run build`，CI k6 smoke 使用一次性 seed、测试用户生成和 Redis 秒杀库存预热来防回归。
- `scripts/performance/` 已包含 k6 秒杀、熔断、集群极限等压测脚本。
- Phase 21 已完成秒杀系统单点高并发防线与 k6 基准压测：Redis Lua 原子扣减、RocketMQ 异步削峰落库、单体承载基线确认。
- Phase 22 已确立混合部署集群方案：中间件 Docker 化，Java 微服务宿主机 JDK 17 多进程运行，Docker Nginx 通过 `host.docker.internal` 转发流量。
- Phase 23 已完成 Gateway x3 + Seckill x3 高可用集群极限压测，1500 VUs 下经 Nginx 负载均衡达到 **2142 QPS**、p50 **7.59ms**，1000 库存绝对零超卖。
- 当前已知极限瓶颈是 Windows OS 层面的 TCP 临时端口（Ephemeral Port）/连接资源耗尽导致的 `Connection Refused`，不是 Redis Lua 原子扣减或业务一致性问题。

后续可规划方向：

- **用户体系深化**：用户资料修改、刷新 token、密码修改、手机号绑定等。
- **Phase 24 后台闭环**：商品、订单、秒杀活动、优惠券、评论、媒体、搜索索引和 RBAC V1 能力已具备；CI 已覆盖 Admin API 全链路与 Admin Web 构建，后续重点是失败提示一致性和操作审计。
- **分类管理后台**：当前前台已有分类树查询，后台分类新增/编辑/排序能力仍可单独规划。
- **数据迁移治理**：已引入 FlywayDB 管理默认表结构迁移；后续新增表、字段和索引必须追加 `V*.sql`，本地演示数据保留为可选 repeatable migration。
- **履约物流深化**：真实物流轨迹、签收异常、售后工单、退货审核等。
- **真实支付渠道**：当前没有对接真实支付宝/微信；mock 支付已具备交易流水、回调验签、金额校验和幂等骨架，后续真实渠道接入重点是渠道参数、证书/密钥、异步通知和对账。
- **压测范围扩展**：已有 k6 秒杀相关脚本，普通交易全链路压测仍可补充。
- **观测体系完善**：当前已有 `X-Trace-Id` 生成、Feign 透传、MQ Trace 上下文和 MDC，后续可补齐链路追踪、指标监控与告警。
