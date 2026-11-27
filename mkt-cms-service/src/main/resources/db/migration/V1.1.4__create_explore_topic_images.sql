# ============================================================
# MKT-CMS 探索页专题图集（MKT-CMS-DSN-CR-004 v0.1 修订）
# Flyway 版本：V1.1.4
# 说明：
#   1) 专题修订支持最多 9 张图片，独立子表保证类型化校验与稳定排序；
#   2) 旧单封面 cover_* 保留用于兼容，并回填为 images[0]；
#   3) 图片属于不可变修订内容，发布后随修订一起不可改删；
#   4) 清空指向其他专题的历史 TOPIC 目标，统一回到专题详情模式。
# ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cms_explore_topic_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic_revision_id` VARCHAR(64) NOT NULL COMMENT '专题修订标识',
    `sort_no` INT NOT NULL COMMENT '图片顺序（从 10 递增）',
    `url` VARCHAR(1024) NOT NULL COMMENT 'HTTPS 素材 URL',
    `width` INT NOT NULL COMMENT '图片宽（像素）',
    `height` INT NOT NULL COMMENT '图片高（像素）',
    `mime` VARCHAR(64) NOT NULL COMMENT 'MIME：image/jpeg / image/png / image/webp',
    `size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_image_sort` (`topic_revision_id`, `sort_no`),
    KEY `idx_topic_image_revision` (`topic_revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页专题修订图片表（最多 9 张）';

-- 旧单封面回填为第一张图片，保证历史专题图集契约可用（重复执行不产生重复行）
INSERT INTO `cms_explore_topic_image`
    (`topic_revision_id`, `sort_no`, `url`, `width`, `height`, `mime`, `size`)
SELECT `revision_id`, 10, `cover_url`, `cover_width`, `cover_height`, `cover_mime`, `cover_size`
FROM `cms_explore_topic_revision`
WHERE `cover_url` IS NOT NULL
  AND `cover_url` <> ''
  AND `cover_width` IS NOT NULL
  AND `cover_height` IS NOT NULL
  AND `cover_mime` IS NOT NULL
  AND `cover_size` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `cms_explore_topic_image` i
      WHERE i.`topic_revision_id` = `cms_explore_topic_revision`.`revision_id`
  );

-- 指向其他专题的历史 TOPIC 目标不再受支持（CR-004 §3.1），统一回到专题详情模式；
-- 已核实当前环境不存在该类数据，语句为幂等兜底，预期影响 0 行。
UPDATE `cms_explore_topic_revision`
SET `target_type` = NULL,
    `target_ref_id` = NULL,
    `target_revision_id` = NULL
WHERE `target_type` = 'TOPIC'
  AND `target_ref_id` IS NOT NULL
  AND `target_ref_id` <> `topic_id`;
