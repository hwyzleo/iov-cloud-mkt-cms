# ============================================================
# MKT-CMS 探索页文章多图（MKT-CMS-DSN-CR-003 v0.4 修订）
# Flyway 版本：V1.1.2
# 说明：
#   1) 文章修订支持最多 9 张图片，独立子表保证类型化校验与稳定排序；
#   2) 旧单封面 cover_* 保留用于兼容，并回填为 images[0]；
#   3) 图片属于不可变修订内容，发布后随修订一起不可改删。
# ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cms_explore_article_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_revision_id` VARCHAR(64) NOT NULL COMMENT '文章修订标识',
    `sort_no` INT NOT NULL COMMENT '图片顺序（从 10 递增）',
    `url` VARCHAR(1024) NOT NULL COMMENT 'HTTPS 素材 URL',
    `width` INT NOT NULL COMMENT '图片宽（像素）',
    `height` INT NOT NULL COMMENT '图片高（像素）',
    `mime` VARCHAR(64) NOT NULL COMMENT 'MIME：image/jpeg / image/png / image/webp',
    `size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_image_sort` (`article_revision_id`, `sort_no`),
    KEY `idx_article_image_revision` (`article_revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页文章修订图片表（最多 9 张）';

-- 旧单封面回填为第一张图片，保证历史文章多图契约可用
INSERT INTO `cms_explore_article_image`
    (`article_revision_id`, `sort_no`, `url`, `width`, `height`, `mime`, `size`)
SELECT `revision_id`, 10, `cover_url`, `cover_width`, `cover_height`, `cover_mime`, `cover_size`
FROM `cms_explore_article_revision`
WHERE `cover_url` IS NOT NULL
  AND `cover_url` <> ''
  AND `cover_width` IS NOT NULL
  AND `cover_height` IS NOT NULL
  AND `cover_mime` IS NOT NULL
  AND `cover_size` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `cms_explore_article_image` i
      WHERE i.`article_revision_id` = `cms_explore_article_revision`.`revision_id`
  );
