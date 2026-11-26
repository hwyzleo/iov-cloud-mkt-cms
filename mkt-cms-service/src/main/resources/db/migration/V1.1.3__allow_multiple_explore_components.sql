# ============================================================
# MKT-CMS 探索页同类组件可多次添加（MKT-CMS-DSN-CR-003 v0.4 修订）
# Flyway 版本：V1.1.3
# 说明：
#   1) 移除 page_id + component_type 唯一约束，允许同一页面添加多个同类区块；
#   2) 组件顺序由 sort_no 显式控制，每页最多 20 个组件由应用层校验；
#   3) 组件内条目仍按 component_id + sort_no 稳定排序。
# ============================================================

SET NAMES utf8mb4;

ALTER TABLE `cms_explore_page_component`
    DROP INDEX `uk_component_page_type`;
