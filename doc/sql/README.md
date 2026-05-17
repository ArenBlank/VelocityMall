# doc/sql 目录说明

这个目录保留历史建表脚本、阶段增量脚本和人工排查参考 SQL。

当前项目已经接入 FlywayDB，默认自动迁移入口是：

```text
velocity-mall-common/src/main/resources/db/migration/
```

本地可选演示数据入口是：

```text
velocity-mall-common/src/main/resources/db/devdata/
```

后续新增表、字段、索引时，请新增 `V*.sql` 到 Flyway 迁移目录，不要继续把 `doc/sql` 当作自动迁移来源。
