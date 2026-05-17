# VelocityMall Flyway 数据库迁移说明

本项目使用 FlywayDB 管理 MySQL 表结构版本。当前项目是多个微服务共用一个业务库 `velocity_mall`，所以只维护一套迁移脚本。

## 当前目录分工

Flyway 默认只扫描这个目录：

```text
velocity-mall-common/src/main/resources/db/migration/
```

当前默认迁移文件：

```text
V1__init_schema.sql
```

本地演示数据单独放在：

```text
velocity-mall-common/src/main/resources/db/devdata/
```

当前可选演示数据文件：

```text
R__demo_seed_data.sql
```

`doc/sql/` 是历史 SQL、人工参考和旧阶段脚本归档，不是 Flyway 的默认迁移源。不要把新表结构变更继续追加到 `doc/sql/` 后再靠人工执行。

## 启动行为

各个连接 MySQL 的服务都配置了：

```yaml
spring:
  flyway:
    enabled: ${FLYWAY_ENABLED:true}
    locations: ${FLYWAY_LOCATIONS:classpath:db/migration}
    baseline-on-migrate: true
    baseline-version: 1
```

干净数据库首次启动任意一个业务服务时，JDBC URL 里的 `createDatabaseIfNotExist=true` 会先创建 `velocity_mall` 数据库，Flyway 再自动执行 `V1__init_schema.sql`，创建业务表和 `flyway_schema_history` 记录表。

`V1__init_schema.sql` 开头会把 `velocity_mall` 的默认字符集固定为 `utf8mb4`，每张业务表也显式声明了 `DEFAULT CHARSET = utf8mb4`，避免接手者本机 MySQL 默认字符集不是 `utf8mb4` 时插入中文出现乱码。

已有表但没有 Flyway 历史表的老数据库首次启动时，会 baseline 到版本 `1`，不会重建或清空现有业务表。之后新增的 `V2`、`V3` 等迁移会继续自动执行。

多个微服务同时启动时，Flyway 会通过数据库锁避免重复迁移。

## V 文件怎么用

`V` 表示 versioned migration，也就是一次性版本迁移。

命名规则：

```text
V版本号__英文描述.sql
```

注意中间是两个下划线。

示例：

```text
V2__add_order_cancel_reason.sql
V3__create_after_sale_table.sql
V4__add_order_delivery_index.sql
```

如果要给订单表新增取消原因字段，创建：

```text
velocity-mall-common/src/main/resources/db/migration/V2__add_order_cancel_reason.sql
```

内容：

```sql
ALTER TABLE `oms_order`
    ADD COLUMN `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因';
```

启动服务后，Flyway 会检查 `flyway_schema_history`。如果数据库没有执行过版本 `2`，就执行这个文件里的 SQL；执行成功后记录版本 `2`，下次启动不再重复执行。

约束：

- 同一个版本号只能有一个文件，不能同时存在 `V2__a.sql` 和 `V2__b.sql`。
- 已经提交并被环境执行过的 `V*.sql` 不要再改内容，否则 Flyway 校验会失败。
- 表结构变化优先追加新的 `V*.sql`，不要直接修改 `V1__init_schema.sql`。

## R 文件怎么用

`R` 表示 repeatable migration，也就是可重复迁移。

示例：

```text
R__demo_seed_data.sql
```

它不靠版本号排序，而是靠文件内容 checksum 判断是否需要重新执行：

- 第一次被扫描到：执行。
- 文件内容没变：不重复执行。
- 文件内容变了：下次 migrate 时重新执行。

本项目把演示商品和秒杀活动数据放在 `db/devdata`，默认不启用。需要本地演示数据时，启动服务前设置：

```powershell
$env:FLYWAY_LOCATIONS = "classpath:db/migration,classpath:db/devdata"
```

然后启动任意一个连接 MySQL 的服务即可。

表结构变更不要用 `R`，优先使用 `V`。例如 `ALTER TABLE ADD COLUMN` 重复执行容易报重复字段错误。

## 常用操作

临时关闭 Flyway：

```powershell
$env:FLYWAY_ENABLED = "false"
```

查看迁移状态：

```sql
SELECT installed_rank, version, description, type, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

本地从零验证：

1. 停掉业务服务。
2. 清空或重建 `velocity_mall` 数据库。
3. 启动任意一个业务服务。
4. 检查 `flyway_schema_history` 和核心业务表是否生成。

## 接手约定

- 自动迁移入口只有 `velocity-mall-common/src/main/resources/db/migration/`。
- 本地演示数据入口只有 `velocity-mall-common/src/main/resources/db/devdata/`，并且默认不启用。
- `doc/sql/` 只作为历史参考和手工排查资料。
- 新增表、字段、索引时，追加下一个版本号的 `V*.sql`。
- 多个服务共享同一个业务库时，只维护这一套迁移脚本，避免各模块各自建表导致版本漂移。
