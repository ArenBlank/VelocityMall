# VelocityMall 极速商城

VelocityMall 是基于 Spring Cloud Alibaba 的高并发电商微服务项目，当前代码以 Java 17、Spring Boot 3.2.4、Spring Cloud 2023.0.1、Spring Cloud Alibaba 2023.0.1.0 为版本基线。

当前仓库是 10 模块 Maven 工程，核心链路已覆盖商品、购物车、订单、秒杀、搜索、优惠券、评价、用户、收货地址、后台发货和商品上下架管理。若旧文档描述与当前代码不一致，以当前代码、配置和 E2E 脚本为准。

## 模块

| 模块 | 端口 | 职责 |
| --- | ---: | --- |
| `velocity-mall-common` | -- | 公共响应、异常、实体基类、MyBatis/Redis/Feign/Trace 配置、用户上下文、跨服务 DTO/VO |
| `velocity-mall-gateway` | 8080 | 统一入口、JWT 鉴权、用户/管理员 Header 注入、内部接口拦截、Sentinel 秒杀限流、路由转发 |
| `velocity-mall-product` | 8081 | 商品分类、SPU/SKU 查询、缓存治理、库存锁定/释放/扣减、商品搜索同步消息 |
| `velocity-mall-order` | 8082 | 购物车、普通订单、支付 mock、退款 mock、延时关单、秒杀订单落库、发货/确认收货 |
| `velocity-mall-seckill` | 8083 | Redis Lua 秒杀、防重复抢购、MQ 异步下单、库存占位回滚 |
| `velocity-mall-search` | 8085 | Elasticsearch 商品搜索、索引重建、商品同步消费 |
| `velocity-mall-coupon` | 8086 | 优惠券领取、限领、防重复领取、库存扣减 |
| `velocity-mall-review` | 8087 | 商品评价发布/分页/统计/删除、点赞/点踩互动、购买资格校验 |
| `velocity-mall-user` | 8088 | 用户注册、登录、JWT、当前用户信息、收货地址 |
| `velocity-mall-admin` | 8089 | 管理员登录、订单发货、商品上架/下架 |

## 快速验证

```bash
# 构建所有模块
mvn clean package -DskipTests

# 启动完整 E2E：中间件 + 9 个可运行服务
bash scripts/ci/e2e.sh
```

`scripts/ci/e2e.sh` 当前会启动 product、order、seckill、search、coupon、gateway、user、admin、review，并验证注册登录、地址、领券、购物车、下单、支付、后台发货、确认收货、评价互动、退款、秒杀、搜索索引重建和内部接口拦截。

## 压测结果

| 场景 | VU | 吞吐/QPS | 结果 |
| --- | ---: | ---: | --- |
| Sentinel 限流 | 1,000 | 1,992 req/s | 99.75% 请求被 429 拦截 |
| 防超卖 | 200 | 1,561 req/s | 50 库存精确售出，零超卖，P95 约 4ms |
| 熔断降级 | 500 | 1,146 req/s | 1s 内熔断，失败率 0.01% |
| 集群极限 300 VU | 300 | 1,897 req/s | 秒杀成功 1,000，超卖 0 |
| 集群极限 1500 VU | 1,500 | 2,142 req/s | 秒杀成功 1,000，超卖 0 |

压测脚本位于 `scripts/performance/`，报告位于 `doc/performance/`。

## 文档入口

- `DEVELOPMENT_SPEC.md`：架构规范、编码约束、阶段路线和后续开发红线。
- `CURRENT_IMPLEMENTATION_STATUS.md`：按当前代码同步后的模块、接口、E2E 和待规划能力清单。
- `CLAUDE.md`：给 AI 协作工具使用的项目工作指南。
