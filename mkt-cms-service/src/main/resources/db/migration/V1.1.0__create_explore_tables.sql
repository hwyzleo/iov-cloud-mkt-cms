# ============================================================
# MKT-CMS 探索页内容运营子域建表（MKT-CMS-DSN-CR-003 §4.2 / D-016）
# Flyway 版本：V1.1.0
# 说明：
#   1) 类型化内容库（频道 / 文章 / 专题 / Banner）与频道页面草稿、不可变发布版本、当前指针、操作留痕；
#   2) 草稿可改；发布版本及被引用修订不可改删；当前版本由显式指针决定；
#   3) 时间列统一 DATETIME(3)，按 UTC 墙钟存储（会话 UTC，见 FW-MYSQL）；
#   4) 具体字段长度以本脚本为准，不修改历史迁移脚本。
# ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 频道（编码稳定；名称、排序、启停可维护）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `channel_code` VARCHAR(32) NOT NULL COMMENT '频道编码：RECOMMEND / ACTIVITY / LOCAL',
    `name` VARCHAR(64) NOT NULL COMMENT '频道显示名称',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '显式排序',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '启停状态：ENABLED / DISABLED',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_code` (`channel_code`),
    KEY `idx_channel_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页频道表';

-- ------------------------------------------------------------
-- 文章根实体（来源类型 + 状态 + 当前修订）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id` VARCHAR(64) NOT NULL COMMENT '文章业务标识',
    `source_type` VARCHAR(16) NOT NULL COMMENT '来源类型：OFFICIAL / UGC_REFERENCE',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT '根实体状态：PUBLISHED / OFFLINE',
    `current_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '当前已发布修订标识；NULL 表示尚未发布',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_id` (`article_id`),
    KEY `idx_article_status_type` (`status`, `source_type`),
    KEY `idx_article_current_revision` (`current_revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页文章根实体表';

-- ------------------------------------------------------------
-- 文章修订（不可变快照；官方安全 HTML 或 UGC 展示快照）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_article_revision` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `revision_id` VARCHAR(64) NOT NULL COMMENT '修订标识',
    `article_id` VARCHAR(64) NOT NULL COMMENT '文章业务标识',
    `revision_no` INT NOT NULL COMMENT '修订序号（同文章内递增）',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '修订状态：DRAFT / PUBLISHED',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `summary` VARCHAR(1024) DEFAULT NULL COMMENT '摘要',
    `cover_url` VARCHAR(1024) DEFAULT NULL COMMENT '封面 HTTPS URL',
    `cover_width` INT DEFAULT NULL COMMENT '封面宽（像素）',
    `cover_height` INT DEFAULT NULL COMMENT '封面高（像素）',
    `cover_mime` VARCHAR(64) DEFAULT NULL COMMENT '封面 MIME：image/jpeg / image/png / image/webp',
    `cover_size` BIGINT DEFAULT NULL COMMENT '封面大小（字节）',
    `author_name` VARCHAR(128) DEFAULT NULL COMMENT '作者显示名',
    `author_avatar_url` VARCHAR(1024) DEFAULT NULL COMMENT '作者头像 HTTPS URL',
    `display_at` DATETIME(3) DEFAULT NULL COMMENT '展示时间（UTC）',
    `content_type` VARCHAR(64) NOT NULL DEFAULT 'text/html; charset=utf-8' COMMENT '正文格式',
    `body` MEDIUMTEXT DEFAULT NULL COMMENT '官方文章安全 HTML；UGC 为空',
    `content_hash` CHAR(64) NOT NULL COMMENT '安全正文 SHA-256；UGC 为展示快照摘要',
    `sanitizer_version` VARCHAR(32) DEFAULT NULL COMMENT '清洗策略版本（官方文章）',
    `source_content_id` VARCHAR(128) DEFAULT NULL COMMENT 'UGC 来源内容标识',
    `native_route_code` VARCHAR(128) DEFAULT NULL COMMENT 'UGC 原生路由编码',
    `published_at` DATETIME(3) DEFAULT NULL COMMENT '发布时间（UTC）',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_revision_id` (`revision_id`),
    UNIQUE KEY `uk_article_revision_no` (`article_id`, `revision_no`),
    KEY `idx_article_revision_published` (`article_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页文章修订表（发布后不可改删）';

-- ------------------------------------------------------------
-- 专题根实体
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_topic` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic_id` VARCHAR(64) NOT NULL COMMENT '专题业务标识',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT '根实体状态：PUBLISHED / OFFLINE',
    `current_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '当前已发布修订标识',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_id` (`topic_id`),
    KEY `idx_topic_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页专题根实体表';

-- ------------------------------------------------------------
-- 专题修订（不可变快照）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_topic_revision` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `revision_id` VARCHAR(64) NOT NULL COMMENT '修订标识',
    `topic_id` VARCHAR(64) NOT NULL COMMENT '专题业务标识',
    `revision_no` INT NOT NULL COMMENT '修订序号',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '修订状态：DRAFT / PUBLISHED',
    `name` VARCHAR(128) NOT NULL COMMENT '专题名称',
    `summary` VARCHAR(1024) DEFAULT NULL COMMENT '专题摘要',
    `cover_url` VARCHAR(1024) DEFAULT NULL COMMENT '封面 HTTPS URL',
    `cover_width` INT DEFAULT NULL COMMENT '封面宽',
    `cover_height` INT DEFAULT NULL COMMENT '封面高',
    `cover_mime` VARCHAR(64) DEFAULT NULL COMMENT '封面 MIME',
    `cover_size` BIGINT DEFAULT NULL COMMENT '封面大小（字节）',
    `target_type` VARCHAR(16) DEFAULT NULL COMMENT '专题跳转类型：ARTICLE / TOPIC / H5 / NATIVE',
    `target_ref_id` VARCHAR(64) DEFAULT NULL COMMENT '目标业务标识（ARTICLE/TOPIC）',
    `target_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '目标修订标识（ARTICLE/TOPIC）',
    `target_url` VARCHAR(1024) DEFAULT NULL COMMENT 'H5 目标 HTTPS URL',
    `target_route_code` VARCHAR(128) DEFAULT NULL COMMENT 'NATIVE 路由编码',
    `target_params` VARCHAR(2048) DEFAULT NULL COMMENT 'NATIVE 受控参数 JSON',
    `content_hash` CHAR(64) NOT NULL COMMENT '修订内容 SHA-256',
    `published_at` DATETIME(3) DEFAULT NULL COMMENT '发布时间（UTC）',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_revision_id` (`revision_id`),
    UNIQUE KEY `uk_topic_revision_no` (`topic_id`, `revision_no`),
    KEY `idx_topic_revision_published` (`topic_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页专题修订表（发布后不可改删）';

-- ------------------------------------------------------------
-- 专题文章条目（人工顺序 + 展示快照）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_topic_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic_revision_id` VARCHAR(64) NOT NULL COMMENT '专题修订标识',
    `article_revision_id` VARCHAR(64) NOT NULL COMMENT '引用文章修订标识（必须已发布）',
    `sort_no` INT NOT NULL COMMENT '人工顺序',
    `display_snapshot` JSON DEFAULT NULL COMMENT '卡片展示快照（固定 Schema）',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_item_sort` (`topic_revision_id`, `sort_no`),
    KEY `idx_topic_item_article_revision` (`article_revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页专题文章条目表';

-- ------------------------------------------------------------
-- Banner 根实体
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_banner` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `banner_id` VARCHAR(64) NOT NULL COMMENT 'Banner 业务标识',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT '根实体状态：PUBLISHED / OFFLINE',
    `current_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '当前已发布修订标识',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_banner_id` (`banner_id`),
    KEY `idx_banner_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页 Banner 根实体表';

-- ------------------------------------------------------------
-- Banner 修订（不可变快照）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_banner_revision` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `revision_id` VARCHAR(64) NOT NULL COMMENT '修订标识',
    `banner_id` VARCHAR(64) NOT NULL COMMENT 'Banner 业务标识',
    `revision_no` INT NOT NULL COMMENT '修订序号',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '修订状态：DRAFT / PUBLISHED',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `image_url` VARCHAR(1024) NOT NULL COMMENT '主图 HTTPS URL',
    `image_width` INT NOT NULL COMMENT '主图宽（像素）',
    `image_height` INT NOT NULL COMMENT '主图高（像素）',
    `image_mime` VARCHAR(64) NOT NULL COMMENT '主图 MIME',
    `image_size` BIGINT NOT NULL COMMENT '主图大小（字节）',
    `target_type` VARCHAR(16) NOT NULL COMMENT '跳转类型：ARTICLE / TOPIC / H5 / NATIVE',
    `target_ref_id` VARCHAR(64) DEFAULT NULL COMMENT '目标业务标识（ARTICLE/TOPIC）',
    `target_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '目标修订标识（ARTICLE/TOPIC）',
    `target_url` VARCHAR(1024) DEFAULT NULL COMMENT 'H5 目标 HTTPS URL',
    `target_route_code` VARCHAR(128) DEFAULT NULL COMMENT 'NATIVE 路由编码',
    `target_params` VARCHAR(2048) DEFAULT NULL COMMENT 'NATIVE 受控参数 JSON',
    `default_sort_no` INT NOT NULL DEFAULT 0 COMMENT '默认排序（页面编排可覆盖）',
    `content_hash` CHAR(64) NOT NULL COMMENT '修订内容 SHA-256',
    `published_at` DATETIME(3) DEFAULT NULL COMMENT '发布时间（UTC）',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_banner_revision_id` (`revision_id`),
    UNIQUE KEY `uk_banner_revision_no` (`banner_id`, `revision_no`),
    KEY `idx_banner_revision_published` (`banner_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页 Banner 修订表（发布后不可改删）';

-- ------------------------------------------------------------
-- 频道页面版本（版本 0 唯一草稿；>0 不可变发布版本）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_page` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `page_id` VARCHAR(64) NOT NULL COMMENT '页面业务标识',
    `channel_code` VARCHAR(32) NOT NULL COMMENT '频道编码',
    `page_version` INT NOT NULL COMMENT '版本：0 草稿；>0 发布版本',
    `status` VARCHAR(16) NOT NULL COMMENT '状态：DRAFT / SCHEDULED / PUBLISHED / EXPIRED / OFFLINE / CANCELED',
    `effective_at` DATETIME(3) DEFAULT NULL COMMENT '生效时间（UTC）；立即发布为空',
    `expire_at` DATETIME(3) DEFAULT NULL COMMENT '到期时间（UTC）；空表示不到期',
    `request_id` VARCHAR(64) DEFAULT NULL COMMENT '幂等请求标识（草稿为空）',
    `request_hash` CHAR(64) DEFAULT NULL COMMENT '规范化发布命令 SHA-256',
    `expected_release_id` VARCHAR(64) DEFAULT NULL COMMENT '发布前提：期望的当前版本标识',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT '草稿乐观锁版本；发布版本固定 0',
    `published_by` VARCHAR(64) DEFAULT NULL COMMENT '发布/调度操作人',
    `published_at` DATETIME(3) DEFAULT NULL COMMENT '发布时间（UTC）',
    `offline_at` DATETIME(3) DEFAULT NULL COMMENT '下线时间（UTC）',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_page_id` (`page_id`),
    UNIQUE KEY `uk_page_channel_version` (`channel_code`, `page_version`),
    UNIQUE KEY `uk_page_request_id` (`request_id`),
    KEY `idx_page_channel_status` (`channel_code`, `status`),
    KEY `idx_page_schedule` (`status`, `effective_at`),
    KEY `idx_page_expire` (`status`, `expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页频道页面版本表';

-- ------------------------------------------------------------
-- 页面组件（每页每类组件最多一个）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_page_component` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `component_id` VARCHAR(64) NOT NULL COMMENT '组件业务标识',
    `page_id` VARCHAR(64) NOT NULL COMMENT '页面业务标识',
    `component_type` VARCHAR(32) NOT NULL COMMENT '组件类型：BANNER_CAROUSEL / TOPIC_GRID / ARTICLE_FEED',
    `sort_no` INT NOT NULL COMMENT '组件显式排序',
    `config_json` JSON DEFAULT NULL COMMENT '组件受控配置',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_component_id` (`component_id`),
    UNIQUE KEY `uk_component_page_type` (`page_id`, `component_type`),
    KEY `idx_component_page_sort` (`page_id`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页页面组件表';

-- ------------------------------------------------------------
-- 页面条目（发布版本条目不可改删；展示快照固定 Schema）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_page_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id` VARCHAR(64) NOT NULL COMMENT '条目业务标识',
    `component_id` VARCHAR(64) NOT NULL COMMENT '组件业务标识',
    `item_type` VARCHAR(16) NOT NULL COMMENT '条目类型：BANNER / TOPIC / ARTICLE',
    `ref_id` VARCHAR(64) NOT NULL COMMENT '引用内容业务标识',
    `ref_revision_id` VARCHAR(64) NOT NULL COMMENT '引用内容修订标识',
    `sort_no` INT NOT NULL COMMENT '条目人工顺序',
    `start_at` DATETIME(3) DEFAULT NULL COMMENT '条目生效时间（UTC）',
    `end_at` DATETIME(3) DEFAULT NULL COMMENT '条目失效时间（UTC，不含）',
    `target_type` VARCHAR(16) DEFAULT NULL COMMENT '跳转类型',
    `target_ref_id` VARCHAR(64) DEFAULT NULL COMMENT '跳转目标业务标识',
    `target_revision_id` VARCHAR(64) DEFAULT NULL COMMENT '跳转目标修订标识',
    `target_url` VARCHAR(1024) DEFAULT NULL COMMENT 'H5 跳转 URL',
    `target_route_code` VARCHAR(128) DEFAULT NULL COMMENT 'NATIVE 路由编码',
    `target_params` VARCHAR(2048) DEFAULT NULL COMMENT 'NATIVE 受控参数 JSON',
    `display_snapshot` JSON DEFAULT NULL COMMENT '卡片展示快照（固定 Schema，非任意业务 JSON）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item_id` (`item_id`),
    UNIQUE KEY `uk_item_component_sort` (`component_id`, `sort_no`),
    KEY `idx_item_cursor` (`component_id`, `sort_no`, `item_id`),
    KEY `idx_item_ref` (`item_type`, `ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页页面条目表';

-- ------------------------------------------------------------
-- 频道当前页面指针（每频道最多一个当前版本）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_page_current` (
    `channel_code` VARCHAR(32) NOT NULL COMMENT '频道编码',
    `release_id` VARCHAR(64) DEFAULT NULL COMMENT '当前发布版本标识；NULL 表示无当前页面',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT '指针版本',
    `updated_at` DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (`channel_code`),
    KEY `idx_current_release` (`release_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页频道当前版本指针表';

-- ------------------------------------------------------------
-- 操作留痕（成功操作与业务同事务；失败尝试走独立安全日志）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_explore_operation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operation_id` VARCHAR(64) NOT NULL COMMENT '操作标识',
    `object_type` VARCHAR(32) NOT NULL COMMENT '对象类型：EXPLORE_ARTICLE / EXPLORE_TOPIC / EXPLORE_BANNER / EXPLORE_PAGE / EXPLORE_CHANNEL',
    `object_id` VARCHAR(64) NOT NULL COMMENT '对象业务标识',
    `action` VARCHAR(32) NOT NULL COMMENT '动作',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人（安全上下文或固定调度身份）',
    `operated_at` DATETIME(3) NOT NULL COMMENT '操作时间（UTC）',
    `result` VARCHAR(16) NOT NULL COMMENT '结果：SUCCESS',
    `request_id` VARCHAR(64) DEFAULT NULL COMMENT '关联幂等请求标识',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪 ID',
    `detail` VARCHAR(1024) DEFAULT NULL COMMENT '受控摘要（不得包含正文与敏感个人信息）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_explore_operation_id` (`operation_id`),
    KEY `idx_explore_operation_object` (`object_type`, `object_id`),
    KEY `idx_explore_operation_time` (`operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='探索页操作留痕表';
