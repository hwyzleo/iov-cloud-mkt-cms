# MKT-CMS 数据库版本管理说明

## Flyway 版本化策略

本项目使用 Flyway 管理 `db_cms` 中的表结构与初始化数据，配置见 `bootstrap.yml`：

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations: db/migration
    validate-on-migrate: true
```

- 迁移脚本目录：`mkt-cms-service/src/main/resources/db/migration/`
- 版本记录表：`db_cms.flyway_schema_history`
- `validate-on-migrate: true`：已应用迁移的版本号与校验和必须与本地脚本一致，**不得修改或重命名已发布的迁移文件**

## 版本号规则

采用语义化版本号：`V{主版本}.{次版本}.{补丁版本}__{描述}.sql`

| 段位 | 递增时机 | 示例 |
| --- | --- | --- |
| 主版本 | 重大架构调整（如拆表重组、存储模型换代） | V1.0.0 → V2.0.0 |
| 次版本 | 新增功能模块 / 新 CR 带来的新表或结构性变更 | V1.0.0 → V1.1.0 |
| 补丁版本 | 小功能、索引与约束调整、初始化数据补充、缺陷修复 | V1.0.0 → V1.0.1 |

约定：

1. **一个变更一个文件**，描述使用小写下划线，能看出用途，例如 `V1.1.0__create_agreement_menu_permissions.sql`。
2. **不得重命名或修改已应用过的文件**；需要修正时新增更高版本号的文件。
3. **不得在同一项目内混用 `V1`、`V2` 与 `V1.0.0` 两种写法**：Flyway 视 `V1` 与 `V1.0.0` 为同一版本，混用会导致启动校验失败。本项目统一使用三段式。
4. 版本号由**数字逐段比较**（`1.0.10 > 1.0.9`，`1.1.0 > 1.0.1`），不按字符串排序，无需担心位数不足。
5. 数据类脚本必须**可重复执行**（`INSERT ... ON DUPLICATE KEY UPDATE` 或 `WHERE NOT EXISTS`），便于多环境与重放场景。
6. 涉及菜单与权限的迁移按 MKT-CMS-DSN-CR-002 §13.3 要求保持可重复执行。

## 现有迁移

| 版本 | 说明 |
| --- | --- |
| V1.0.0 | 协议内容子域建表：草稿、修订、发布包、当前指针、操作留痕（MKT-CMS-DSN-CR-001） |
| V1.0.1 | 车端激活协议初始基线内容：隐私政策 + 用户协议两份修订与成套发布包（MKT-CMS-DSN-CR-001） |

## 注意事项

- 时间列统一为 `DATETIME(3)`，按 UTC 墙钟写入，脚本内使用 `UTC_TIMESTAMP(3)`，不要使用 `NOW()`。
- 协议正文属于不可变快照：已发布修订不得通过迁移脚本改写正文；内容调整走「编辑草稿 → 定稿新修订 → 发布新包」。
- 脚本首行保留 `SET NAMES utf8mb4;`，确保中文正文与 `SHA2(body, 256)` 摘要按 UTF-8 字节计算。
