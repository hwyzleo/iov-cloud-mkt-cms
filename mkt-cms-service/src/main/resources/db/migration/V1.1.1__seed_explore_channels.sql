# ============================================================
# MKT-CMS 探索页预置频道（MKT-CMS-DSN-CR-003 §4.1 / AC-029）
# Flyway 版本：V1.1.1
# 说明：预置 RECOMMEND / ACTIVITY / LOCAL 三个稳定频道编码；
#       重放时不覆盖运营已调整的名称、排序与启停状态。
# ============================================================

SET NAMES utf8mb4;

INSERT INTO `cms_explore_channel` (`channel_code`, `name`, `sort_no`, `status`, `lock_version`,
                                   `create_by`, `create_time`, `modify_by`, `modify_time`)
VALUES ('RECOMMEND', '推荐', 10, 'ENABLED', 0, 'system', UTC_TIMESTAMP(3), 'system', UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE `channel_code` = `channel_code`;

INSERT INTO `cms_explore_channel` (`channel_code`, `name`, `sort_no`, `status`, `lock_version`,
                                   `create_by`, `create_time`, `modify_by`, `modify_time`)
VALUES ('ACTIVITY', '活动', 20, 'ENABLED', 0, 'system', UTC_TIMESTAMP(3), 'system', UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE `channel_code` = `channel_code`;

INSERT INTO `cms_explore_channel` (`channel_code`, `name`, `sort_no`, `status`, `lock_version`,
                                   `create_by`, `create_time`, `modify_by`, `modify_time`)
VALUES ('LOCAL', '此地', 30, 'ENABLED', 0, 'system', UTC_TIMESTAMP(3), 'system', UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE `channel_code` = `channel_code`;
