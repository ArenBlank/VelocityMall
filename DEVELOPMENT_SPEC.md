# VelocityMall 开发规范与架构蓝图（实战务实版）

> 本文档是 VelocityMall（极速商城）的全局架构契约、编码规范与后续 AI 协作准则。
> 后续任何需求分析、代码生成、重构、排错与架构调整，都必须优先阅读并遵守本文档。

## 0. AI 协作核心守则

### 0.1 开发前置动作

从本文档创建后，AI 在修改或生成任何项目代码前，必须优先回溯并阅读 `DEVELOPMENT_SPEC.md`，再结合当前代码状态判断是否可以执行。

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

项目重点不是通过堆砌物理机器应对虚构的极端流量，而是展现扎实的企业级代码治理素养。系统目标是解决中等并发场景下的典型业务痛点，峰值可按数百至数千 QPS 进行设计与压测演示。

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

### 2.2 版本选择原则

版本选择以兼容矩阵为准，不盲目追最新。

如果未来升级 Spring Boot、Spring Cloud 或 Spring Cloud Alibaba，必须同步检查三者版本兼容关系，不能只改一个版本号。

## 3. Phase Roadmap

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

### Phase 6：秒杀链路

目标：

- 建立秒杀库存预热。
- 使用 Redis Lua 完成资格校验、防重复抢购、库存原子扣减。
- 使用 RocketMQ 削峰异步落库。

验收标准：

- 秒杀入口不先查 MySQL 判断库存。
- Redis Lua 保证库存扣减与用户占位原子性。
- 超卖为 0。
- 重复抢购被拦截。

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
velocitymall:seckill:stock:{activityId}:{skuId}
velocitymall:seckill:user:{activityId}:{skuId}
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
seckill-success-topic
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

秒杀入口禁止先查 MySQL 判断库存。

标准流程：

1. 后台任务提前将秒杀库存预热到 Redis。
2. 用户发起抢购请求。
3. 后端执行 Redis Lua 脚本。
4. Lua 内原子完成资格校验与库存扣减。
5. Lua 成功后发送 RocketMQ 消息。
6. 前端立即返回 `HTTP 202 Accepted`，表示排队中。
7. 消费者按数据库可承受速率异步创建订单。

Lua 脚本必须原子完成：

1. 验证用户是否已抢购。
2. 验证库存是否大于 0。
3. 执行 `DECR` 扣减库存。
4. 执行 `SADD` 记录当前用户 ID。

消息 Topic：

```text
seckill-success-topic
```

消息体必须包含：

- `activityId`。
- `skuId`。
- `userId`。
- `quantity`。
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
```

允许流转：

```text
待付款 -> 已付款
待付款 -> 已关闭/超时取消
已付款 -> 已发货
已发货 -> 已完成
```

禁止流转：

```text
已付款 -> 待付款
已关闭 -> 已付款
已完成 -> 已关闭
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
3. 查询订单当前状态。
4. 若订单已支付、已发货或已完成，直接返回成功，避免支付平台继续重试。
5. 若订单待付款，执行状态更新：`待付款 -> 已付款`。
6. 若订单已关闭，需要进入人工核查或退款流程，不能静默改为已付款。

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
  "code": 200,
  "message": "success",
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

必须：

- 命名清晰。
- 事务边界明确。
- 异常语义明确。
- 数据库字段有注释。
- 核心状态更新有条件。
- 并发敏感表使用乐观锁或条件更新。
- 每次代码变更后进行可执行验证。

## 13. 当前代码与后续重构提示

当前项目已完成：

- Maven 多模块结构。
- 公共响应体。
- 全局异常处理基础结构。
- MyBatis-Plus 分页与乐观锁插件。
- 商品与订单基础 DDL。
- 商品与订单实体、Mapper。

后续建议优先补齐：

- `BaseEntity` 与 `VersionedEntity` 公共实体抽取。
- 实体类继承公共实体后的 Lombok 注解调整。
- `BusinessException` 与 `ResultCode`。
- DTO、VO、Controller、Service 基础业务闭环。
- RedisTemplate 序列化配置。
- RocketMQ 消息模型与消费者幂等。

这些重构应按 Phase 推进，不需要一次性完成所有中间件接入。
