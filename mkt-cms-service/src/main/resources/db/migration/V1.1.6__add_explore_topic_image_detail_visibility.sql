# ============================================================
# MKT-CMS 探索页专题封面/详情图集解耦（MKT-CMS-DSN-CR-005 §5.1 / D-020）
# Flyway 版本：V1.1.6
# 说明：
#   1) 专题图片增加详情可见标志，默认 TRUE；
#   2) 存量行与历史 cover 回填行均为 TRUE，迁移前后详情展示不变；
#   3) 不修改历史图片其他元数据，文章图集不开放该运营配置。
# ============================================================

SET NAMES utf8mb4;

ALTER TABLE `cms_explore_topic_image`
    ADD COLUMN `show_in_detail` BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT '是否进入详情图集；FALSE 表示仅作卡片封面候选' AFTER `size`;

-- 兜底：历史行按 TRUE 回填（列默认值已覆盖，语句幂等）
UPDATE `cms_explore_topic_image` SET `show_in_detail` = TRUE WHERE `show_in_detail` IS NULL;
