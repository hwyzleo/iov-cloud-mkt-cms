# ============================================================
# MKT-CMS 协议内容子域建表（MKT-CMS-DSN-CR-001 §3.2）
# Flyway 版本：V1.0.0
# 说明：草稿可改；修订/发布包不可改删；当前指针由发布包显式确定。
#       时间列为 DATETIME(3)，按 UTC 墙钟存储（会话 UTC，见 FW-MYSQL）。
# ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 协议草稿（可多次编辑，乐观锁防覆盖）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_agreement_draft` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `draft_id` VARCHAR(64) NOT NULL COMMENT '草稿业务标识',
    `agreement_type` VARCHAR(32) NOT NULL COMMENT '协议类型：PRIVACY_POLICY / USER_AGREEMENT',
    `scene` VARCHAR(32) NOT NULL COMMENT '场景：IVI_ACTIVATION',
    `market` VARCHAR(32) NOT NULL COMMENT '市场，未细分为 DEFAULT',
    `locale` VARCHAR(16) NOT NULL COMMENT '语言，首批 zh-CN',
    `brand_code` VARCHAR(64) NOT NULL COMMENT '品牌，未细分为 DEFAULT',
    `car_line_code` VARCHAR(64) NOT NULL COMMENT '车系，未细分为 DEFAULT',
    `title` VARCHAR(255) NOT NULL COMMENT '协议标题',
    `version` VARCHAR(64) NOT NULL COMMENT '业务展示版本，不作为排序键',
    `agreement_date` DATE NOT NULL COMMENT '内容提供方确认的协议日期',
    `content_type` VARCHAR(64) NOT NULL DEFAULT 'text/html; charset=utf-8' COMMENT '正文格式',
    `body` MEDIUMTEXT NOT NULL COMMENT '清洗后的安全 HTML 片段',
    `sanitizer_version` VARCHAR(32) NOT NULL COMMENT '清洗策略版本',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT '草稿乐观锁版本',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间（UTC）',
    `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    `modify_time` DATETIME(3) NOT NULL COMMENT '修改时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_draft_id` (`draft_id`),
    KEY `idx_draft_identity` (`agreement_type`, `scene`, `market`, `locale`, `brand_code`, `car_line_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议草稿表';

-- ------------------------------------------------------------
-- 协议修订（不可变快照：定稿后不可改删）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_agreement_revision` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `revision_id` VARCHAR(64) NOT NULL COMMENT '不可变修订标识',
    `agreement_code` VARCHAR(128) NOT NULL COMMENT '稳定协议身份标识（服务端确定性派生）',
    `agreement_type` VARCHAR(32) NOT NULL COMMENT '协议类型：PRIVACY_POLICY / USER_AGREEMENT',
    `scene` VARCHAR(32) NOT NULL COMMENT '场景',
    `market` VARCHAR(32) NOT NULL COMMENT '市场',
    `locale` VARCHAR(16) NOT NULL COMMENT '语言',
    `brand_code` VARCHAR(64) NOT NULL COMMENT '品牌',
    `car_line_code` VARCHAR(64) NOT NULL COMMENT '车系',
    `title` VARCHAR(255) NOT NULL COMMENT '协议标题',
    `version` VARCHAR(64) NOT NULL COMMENT '业务展示版本',
    `agreement_date` DATE NOT NULL COMMENT '协议日期',
    `content_type` VARCHAR(64) NOT NULL COMMENT '正文格式',
    `body` MEDIUMTEXT NOT NULL COMMENT '安全 HTML 快照（精确字节参与摘要）',
    `content_hash` CHAR(64) NOT NULL COMMENT '正文 SHA-256（64 位小写十六进制）',
    `sanitizer_version` VARCHAR(32) NOT NULL COMMENT '清洗策略版本',
    `source_draft_id` VARCHAR(64) NOT NULL COMMENT '来源草稿标识',
    `source_draft_version` INT NOT NULL COMMENT '来源草稿乐观锁版本',
    `frozen_by` VARCHAR(64) NOT NULL COMMENT '定稿操作人',
    `frozen_at` DATETIME(3) NOT NULL COMMENT '定稿时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_revision_id` (`revision_id`),
    UNIQUE KEY `uk_identity_version` (`agreement_type`, `scene`, `market`, `locale`, `brand_code`, `car_line_code`, `version`),
    UNIQUE KEY `uk_source_draft` (`source_draft_id`, `source_draft_version`),
    KEY `idx_revision_agreement_code` (`agreement_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议修订表（不可变）';

-- ------------------------------------------------------------
-- 协议发布包（不可变；成套两份修订）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_agreement_bundle` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bundle_id` VARCHAR(64) NOT NULL COMMENT '发布包标识',
    `scene` VARCHAR(32) NOT NULL COMMENT '场景',
    `market` VARCHAR(32) NOT NULL COMMENT '市场',
    `locale` VARCHAR(16) NOT NULL COMMENT '语言',
    `brand_code` VARCHAR(64) NOT NULL COMMENT '品牌',
    `car_line_code` VARCHAR(64) NOT NULL COMMENT '车系',
    `privacy_revision_id` VARCHAR(64) NOT NULL COMMENT '隐私政策修订标识',
    `user_revision_id` VARCHAR(64) NOT NULL COMMENT '用户协议修订标识',
    `published_by` VARCHAR(64) NOT NULL COMMENT '发布操作人',
    `published_at` DATETIME(3) NOT NULL COMMENT '发布时间（UTC）',
    `request_id` VARCHAR(64) NOT NULL COMMENT '幂等请求标识',
    `request_hash` CHAR(64) NOT NULL COMMENT '规范化业务命令 SHA-256',
    `expected_bundle_id` VARCHAR(64) DEFAULT NULL COMMENT '发布前提：期望的当前包标识（首次发布为空）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bundle_id` (`bundle_id`),
    UNIQUE KEY `uk_request_id` (`request_id`),
    KEY `idx_bundle_scope_published` (`scene`, `market`, `locale`, `brand_code`, `car_line_code`, `published_at`),
    KEY `idx_bundle_privacy_revision` (`privacy_revision_id`),
    KEY `idx_bundle_user_revision` (`user_revision_id`),
    CONSTRAINT `fk_bundle_privacy_revision` FOREIGN KEY (`privacy_revision_id`)
        REFERENCES `cms_agreement_revision` (`revision_id`),
    CONSTRAINT `fk_bundle_user_revision` FOREIGN KEY (`user_revision_id`)
        REFERENCES `cms_agreement_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议发布包表（不可变）';

-- ------------------------------------------------------------
-- 当前发布指针（五维 Scope 为主键，保证同一范围最多一个当前包）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_agreement_current` (
    `scene` VARCHAR(32) NOT NULL COMMENT '场景',
    `market` VARCHAR(32) NOT NULL COMMENT '市场',
    `locale` VARCHAR(16) NOT NULL COMMENT '语言',
    `brand_code` VARCHAR(64) NOT NULL COMMENT '品牌',
    `car_line_code` VARCHAR(64) NOT NULL COMMENT '车系',
    `bundle_id` VARCHAR(64) DEFAULT NULL COMMENT '当前发布包标识；NULL 表示该范围尚无当前包',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT '指针版本，切换时递增',
    `updated_at` DATETIME(3) NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (`scene`, `market`, `locale`, `brand_code`, `car_line_code`),
    KEY `idx_current_bundle` (`bundle_id`),
    CONSTRAINT `fk_current_bundle` FOREIGN KEY (`bundle_id`) REFERENCES `cms_agreement_bundle` (`bundle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议当前发布指针表';

-- ------------------------------------------------------------
-- 协议操作留痕（成功操作与业务同事务；失败尝试走独立安全日志）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cms_agreement_operation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operation_id` VARCHAR(64) NOT NULL COMMENT '操作标识',
    `action` VARCHAR(32) NOT NULL COMMENT '动作：CREATE_DRAFT / UPDATE_DRAFT / FREEZE / PUBLISH',
    `object_type` VARCHAR(32) NOT NULL COMMENT '对象类型：AGREEMENT_DRAFT / AGREEMENT_REVISION / AGREEMENT_BUNDLE',
    `object_id` VARCHAR(64) NOT NULL COMMENT '对象业务标识',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人（来自安全上下文）',
    `operated_at` DATETIME(3) NOT NULL COMMENT '操作时间（UTC）',
    `result` VARCHAR(16) NOT NULL COMMENT '结果：SUCCESS',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_operation_id` (`operation_id`),
    KEY `idx_operation_object` (`object_type`, `object_id`),
    KEY `idx_operation_operated_at` (`operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议操作留痕表';
