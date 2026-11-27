# ============================================================
# MKT-CMS 探索页频道独立生命周期（MKT-CMS-DSN-CR-005 §3.3 / D-019）
# Flyway 版本：V1.1.5
# 说明：
#   1) 新增频道配置修订表（草稿 + 不可变已发布修订）；
#   2) 根表增加可编辑草稿指针与当前发布指针，旧 name/sort_no/status 仅保留用于迁移与回滚兼容；
#   3) 为现有频道生成 revision_no=1 的 PUBLISHED 修订并回填当前指针，迁移前后手机读取结果一致；
#   4) 使用确定性修订标识 + 存在性判断防重，重复执行不产生重复修订。
# ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 频道配置修订（DRAFT 可改；PUBLISHED 不可改删）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_channel_revision` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `revision_id` VARCHAR(64) NOT NULL COMMENT '修订业务标识（服务端生成）',
    `channel_code` VARCHAR(32) NOT NULL COMMENT '所属频道编码',
    `revision_no` INT NOT NULL COMMENT '单调递增修订序号',
    `name` VARCHAR(64) NOT NULL COMMENT '频道显示名称',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '显式排序',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '启停状态：ENABLED / DISABLED',
    `revision_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '修订状态：DRAFT / PUBLISHED',
    `content_hash` CHAR(64) NOT NULL COMMENT '规范化配置 SHA-256',
    `published_by` VARCHAR(64) DEFAULT NULL COMMENT '发布操作人（首次发布写入后不重写）',
    `published_at` DATETIME(3) DEFAULT NULL COMMENT '发布时间（UTC）',
    `request_id` VARCHAR(64) DEFAULT NULL COMMENT '发布幂等请求标识',
    `request_hash` CHAR(64) DEFAULT NULL COMMENT '规范发布命令 SHA-256',
    `expected_current_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '发布前提：期望的当前修订标识（首次发布为空）',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_revision_id` (`revision_id`),
    UNIQUE KEY `uk_channel_revision_no` (`channel_code`, `revision_no`),
    UNIQUE KEY `uk_channel_revision_request_id` (`request_id`),
    KEY `idx_channel_revision_status` (`channel_code`, `revision_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页频道配置修订表';

-- ------------------------------------------------------------
-- 根表增加双指针；旧 name/sort_no/status 保留用于迁移与回滚兼容
-- ------------------------------------------------------------
ALTER TABLE `cms_explore_channel`
    ADD COLUMN `draft_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '当前可编辑草稿修订标识' AFTER `status`,
    ADD COLUMN `current_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '当前已发布修订标识；手机只读此指针' AFTER `draft_revision_id`;

-- ------------------------------------------------------------
-- 现有频道回填首个已发布修订（手机可见结果与迁移前一致）
-- content_hash 与 ChannelRevision.contentHash 的规范化字符串完全一致
-- ------------------------------------------------------------
INSERT INTO `cms_explore_channel_revision` (
    `revision_id`, `channel_code`, `revision_no`, `name`, `sort_no`, `status`,
    `revision_status`, `content_hash`, `published_by`, `published_at`,
    `create_by`, `create_time`, `modify_by`, `modify_time`
)
SELECT
    CONCAT('ecr_', UPPER(SUBSTRING(SHA2(CONCAT('CHANNEL|', `c`.`channel_code`, '|1'), 256), 1, 20))),
    `c`.`channel_code`, 1, `c`.`name`, `c`.`sort_no`, `c`.`status`,
    'PUBLISHED',
    SHA2(CONCAT(`c`.`channel_code`, '|', `c`.`name`, '|', `c`.`sort_no`, '|', `c`.`status`), 256),
    'system', UTC_TIMESTAMP(3), 'system', UTC_TIMESTAMP(3), 'system', UTC_TIMESTAMP(3)
FROM `cms_explore_channel` `c`
WHERE NOT EXISTS (
    SELECT 1 FROM `cms_explore_channel_revision` `r`
    WHERE `r`.`channel_code` = `c`.`channel_code`
      AND `r`.`revision_no` = 1
);

-- ------------------------------------------------------------
-- 回填当前发布指针（防重：仅填补空指针）
-- ------------------------------------------------------------
UPDATE `cms_explore_channel` `c`
JOIN `cms_explore_channel_revision` `r`
    ON `r`.`channel_code` = `c`.`channel_code` AND `r`.`revision_no` = 1
SET `c`.`current_revision_id` = `r`.`revision_id`
WHERE `c`.`current_revision_id` IS NULL;
