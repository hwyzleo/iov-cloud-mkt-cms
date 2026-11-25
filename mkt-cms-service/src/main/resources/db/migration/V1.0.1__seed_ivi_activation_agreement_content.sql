# ============================================================
# MKT-CMS 车端激活协议首个基线内容（IVI_ACTIVATION / DEFAULT / zh-CN / DEFAULT / DEFAULT）
# Flyway 版本：V1.0.1
# 说明：首版上线时协议子域无任何草稿、修订与发布包，内部读取必然返回 105002。
#       本脚本按正式管理流程的落库结果，一次性写入“两份草稿 → 两份不可变修订 →
#       一个成套发布包 → 当前指针 → 操作留痕”，使车机激活可读取完整协议包。
# 注意：
#   1. content_hash 统一由 SHA2(body, 256) 计算，与 AgreementRevision.freezeOf 的
#      HexSha256.ofUtf8(body) 完全一致（脚本以 utf8mb4 连接，不会出现编码差异）。
#   2. 正文已按 html-allowlist-v1 允许列表书写（p/br/h1~h4/strong/em/u/ol/ul/li/a），
#      不包含脚本、样式、事件属性或自动加载资源。
#   3. 广告法务与合规复核后如需调整，不要在已发布修订上改正文，应按
#      “编辑草稿 → 定稿新修订 → 发布新包”生成新版本；本脚本仅做为初始基线，且可重复执行。
# ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1) 协议草稿（锁版本 1，与下方修订的 source_draft_version 对应）
-- ------------------------------------------------------------
INSERT INTO `cms_agreement_draft` (
    `draft_id`, `agreement_type`, `scene`, `market`, `locale`, `brand_code`, `car_line_code`,
    `title`, `version`, `agreement_date`, `content_type`, `body`, `sanitizer_version`,
    `lock_version`, `create_by`, `create_time`, `modify_by`, `modify_time`
) VALUES
    ('DRFXDYhHgqRtDWq5O90R_B1', 'PRIVACY_POLICY', 'IVI_ACTIVATION', 'DEFAULT', 'zh-CN', 'DEFAULT', 'DEFAULT',
     '车联网车载信息娱乐系统隐私政策', '1.0', '2026-11-25', 'text/html; charset=utf-8',
     '<p><strong>生效日期：2026年11月25日</strong></p>
<p>本隐私政策适用于您使用本车车载信息娱乐系统（以下简称车机系统）的激活与相关服务。请您在激活前仔细阅读本政策，特别是以粗体标注的内容。</p>
<h2>一、我们收集的信息</h2>
<ol>
<li><strong>车辆与设备信息</strong>：车辆识别代号（VIN）、车机硬件型号、设备标识、软件版本与车载网络状态，用于完成车机激活、身份校验与远程升级。</li>
<li><strong>激活与账号信息</strong>：您绑定的账号标识、脱敏展示的手机号、激活时间与激活结果，用于确认车辆与账号的绑定关系。</li>
<li><strong>使用与日志信息</strong>：功能使用记录、故障与诊断日志、网络连接日志，用于保障服务可用、排查故障与改进产品体验。</li>
<li><strong>位置信息</strong>：仅在您开启导航、道路救援等功能时收集，用于提供对应服务；关闭相关功能后我们不再持续收集。</li>
</ol>
<h2>二、我们如何使用信息</h2>
<p>我们仅在以下目的范围内使用上述信息：完成车辆激活与账号绑定；提供、维护与改进车联网服务；保障车辆与网络安全；履行法律法规要求的义务；在取得您单独同意的情形下向您推送服务通知。</p>
<p>我们不会将您的个人信息用于与上述目的无关的用途；如确需变更用途，我们将再次征得您的同意。</p>
<h2>三、信息的共享、转让与公开披露</h2>
<p>除以下情形外，我们不会向第三方共享您的个人信息：</p>
<ol>
<li>已取得您的单独同意；</li>
<li>为提供激活、导航、通信等必要服务，与合作服务商在最小必要范围内共享，并通过协议约束其个人信息保护义务；</li>
<li>依据法律法规规定，或应司法机关、行政机关的强制性要求。</li>
</ol>
<p>我们不会转让您的个人信息；如发生合并、分立或资产转让，我们将要求承继方继续受本政策约束。</p>
<h2>四、信息的存储与保护</h2>
<p>您的个人信息存储于中华人民共和国境内。我们仅在实现本政策所述目的所必需的最短期限内保留您的信息，法律法规另有规定的除外。</p>
<p>我们采用传输加密、访问控制、脱敏展示与审计留痕等安全措施保护您的信息，并定期开展安全评估。</p>
<h2>五、您的权利</h2>
<p>您可以依法查询、更正、复制、删除您的个人信息，撤回已作出的授权同意，或注销账号。您可通过车机系统内的隐私设置或本政策末尾的联系方式提出请求，我们将在十五个工作日内答复。</p>
<h2>六、未成年人保护</h2>
<p>本服务主要面向成年驾驶人。若您为未成年人，请在监护人陪同下阅读本政策，并在取得监护人同意后使用相关服务。</p>
<h2>七、本政策的更新</h2>
<p>本政策可能因业务调整或法律法规变化而更新。更新后我们会在车机系统内以显著方式提示您阅读；如更新涉及您的重大权益，我们将重新征得您的同意。</p>
<h2>八、联系我们</h2>
<p>如您对本政策有任何疑问、意见或投诉，可通过车机系统设置中的联系我们入口或客服热线与我们联系，我们将在收到请求后及时处理。</p>',
     'html-allowlist-v1', 1, 'init', UTC_TIMESTAMP(3), 'init', UTC_TIMESTAMP(3)),
    ('DRFdfdj_eAcmHKcXNltLvcf', 'USER_AGREEMENT', 'IVI_ACTIVATION', 'DEFAULT', 'zh-CN', 'DEFAULT', 'DEFAULT',
     '车联网车载信息娱乐系统用户协议', '1.0', '2026-11-25', 'text/html; charset=utf-8',
     '<p><strong>生效日期：2026年11月25日</strong></p>
<p>本协议是您与本车车载信息娱乐系统（以下简称车机系统）运营方之间，关于车机激活及服务使用的约定。请您在激活前完整阅读本协议与配套的隐私政策。</p>
<h2>一、定义</h2>
<p>车机系统：指安装于车辆内，提供导航、娱乐、通信、车辆设置等功能的信息娱乐系统。账号：指您在运营方平台注册并用于绑定车辆的账号。</p>
<h2>二、协议范围与生效</h2>
<p>您在激活车机并勾选同意本协议时，本协议即在您与运营方之间成立并生效。若您不同意本协议的任何条款，请勿完成激活，并停止使用相关服务。</p>
<h2>三、激活与车辆绑定</h2>
<ol>
<li>激活需完成车机联网、账号登录与车辆绑定校验，车辆应处于已完成交付的状态。</li>
<li>您承诺为车辆的合法所有人或已取得所有人授权，并保证提交的信息真实、准确、有效。</li>
<li>同一车辆与账号的绑定关系可按运营方规则变更或解除；变更前请确保车辆处于安全停放状态。</li>
</ol>
<h2>四、服务内容与使用规范</h2>
<p>车机系统提供导航、在线娱乐、语音交互、远程升级与道路救援等服务，部分服务可能需要另行开通，或受网络条件限制。您应遵守交通法规，不得在驾驶过程中进行影响行车安全的分神操作。</p>
<p>您不得实施下列行为：破解或篡改车机系统；干扰车联网网络与服务平台；利用车机系统从事违法活动；以任何方式侵犯他人合法权益。</p>
<h2>五、账号与安全</h2>
<p>您应妥善保管账号信息，不得出租、出借或转让。因您保管不当造成的损失由您自行承担；发现账号异常时请立即联系运营方。</p>
<h2>六、知识产权</h2>
<p>车机系统及其软件、界面、内容与商标的知识产权归运营方或相关权利人所有。未经书面许可，您不得复制、修改、分发或反向工程。</p>
<h2>七、服务的变更、中断与终止</h2>
<p>因系统维护、升级、网络故障或不可抗力导致服务中断的，运营方将尽力提前通知并及时恢复。运营方可依据业务发展调整服务内容，调整前将以合理方式告知您。</p>
<h2>八、责任限制</h2>
<p>在法律允许的范围内，运营方不对因不可抗力、第三方服务、您违反本协议或使用不当造成的间接损失承担责任。本条不排除依法应由运营方承担的责任。</p>
<h2>九、协议的更新</h2>
<p>运营方可依据法律法规或业务变化更新本协议。更新后的协议将在车机系统内公示，并在您下次激活或同意时生效；若您不同意更新内容，可停止使用相关服务。</p>
<h2>十、法律适用与争议解决</h2>
<p>本协议适用中华人民共和国法律。因本协议产生的争议，双方应友好协商解决；协商不成的，任一方可向运营方所在地有管辖权的人民法院提起诉讼。</p>
<h2>十一、联系我们</h2>
<p>如您对本协议有疑问，可通过车机系统设置中的联系我们入口或客服热线与我们联系。</p>',
     'html-allowlist-v1', 1, 'init', UTC_TIMESTAMP(3), 'init', UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
    `title` = VALUES(`title`),
    `version` = VALUES(`version`),
    `agreement_date` = VALUES(`agreement_date`),
    `content_type` = VALUES(`content_type`),
    `body` = VALUES(`body`),
    `sanitizer_version` = VALUES(`sanitizer_version`),
    `modify_by` = VALUES(`modify_by`),
    `modify_time` = VALUES(`modify_time`);

-- ------------------------------------------------------------
-- 2) 不可变修订（content_hash 与正文严格一致）
--    agreement_code 按 AgreementCodeRule 规则确定性派生；
--    content_hash 取 SHA2(body, 256)，与 HexSha256.ofUtf8(body) 等价
-- ------------------------------------------------------------
INSERT INTO `cms_agreement_revision` (
    `revision_id`, `agreement_code`, `agreement_type`, `scene`, `market`, `locale`, `brand_code`,
    `car_line_code`, `title`, `version`, `agreement_date`, `content_type`, `body`, `content_hash`,
    `sanitizer_version`, `source_draft_id`, `source_draft_version`, `frozen_by`, `frozen_at`
)
SELECT
    CASE `d`.`agreement_type`
        WHEN 'PRIVACY_POLICY' THEN 'REV7j5iUq5TLb0HJ_1tlRyI'
        ELSE 'REVZEtIDz5NW1b5kUvgvsPb'
    END,
    CONCAT('AGR-', `d`.`agreement_type`, '-', `d`.`scene`, '-',
           UPPER(SUBSTRING(SHA2(CONCAT(`d`.`scene`, '|', `d`.`market`, '|', `d`.`locale`, '|',
                                      `d`.`brand_code`, '|', `d`.`car_line_code`), 256), 1, 8))),
    `d`.`agreement_type`, `d`.`scene`, `d`.`market`, `d`.`locale`, `d`.`brand_code`, `d`.`car_line_code`,
    `d`.`title`, `d`.`version`, `d`.`agreement_date`, `d`.`content_type`, `d`.`body`,
    SHA2(`d`.`body`, 256), `d`.`sanitizer_version`, `d`.`draft_id`, `d`.`lock_version`,
    'init', UTC_TIMESTAMP(3)
FROM `cms_agreement_draft` `d`
WHERE `d`.`draft_id` IN ('DRFXDYhHgqRtDWq5O90R_B1', 'DRFdfdj_eAcmHKcXNltLvcf')
  AND NOT EXISTS (
      SELECT 1 FROM `cms_agreement_revision` `r`
      WHERE `r`.`source_draft_id` = `d`.`draft_id`
        AND `r`.`source_draft_version` = `d`.`lock_version`
  );

-- ------------------------------------------------------------
-- 3) 成套发布包（首次发布，expected_bundle_id 为空）
--    request_hash 与 PublishRequestHash.of(scope, privacy, user, null) 等价
-- ------------------------------------------------------------
INSERT INTO `cms_agreement_bundle` (
    `bundle_id`, `scene`, `market`, `locale`, `brand_code`, `car_line_code`,
    `privacy_revision_id`, `user_revision_id`, `published_by`, `published_at`,
    `request_id`, `request_hash`, `expected_bundle_id`
) VALUES (
    'BDLkjQyv9B4N3Ud9h00IdGb', 'IVI_ACTIVATION', 'DEFAULT', 'zh-CN', 'DEFAULT', 'DEFAULT',
    'REV7j5iUq5TLb0HJ_1tlRyI', 'REVZEtIDz5NW1b5kUvgvsPb', 'init', UTC_TIMESTAMP(3),
    'REQ-INIT-IVI-ACTIVATION-0001',
    '309fa9eca242f3b7935bd876cf2c7731b2f9cdccb3e440ecd7d969804911c5d2', NULL
)
ON DUPLICATE KEY UPDATE
    `privacy_revision_id` = VALUES(`privacy_revision_id`),
    `user_revision_id` = VALUES(`user_revision_id`),
    `published_at` = VALUES(`published_at`);

-- ------------------------------------------------------------
-- 4) 当前发布指针（同一范围唯一）
-- ------------------------------------------------------------
INSERT INTO `cms_agreement_current` (
    `scene`, `market`, `locale`, `brand_code`, `car_line_code`, `bundle_id`, `lock_version`, `updated_at`
) VALUES (
    'IVI_ACTIVATION', 'DEFAULT', 'zh-CN', 'DEFAULT', 'DEFAULT', 'BDLkjQyv9B4N3Ud9h00IdGb', 1, UTC_TIMESTAMP(3)
)
ON DUPLICATE KEY UPDATE
    `bundle_id` = VALUES(`bundle_id`),
    `updated_at` = VALUES(`updated_at`);

-- ------------------------------------------------------------
-- 5) 操作留痕（两次定稿 + 一次发布，与业务事实一致）
-- ------------------------------------------------------------
INSERT INTO `cms_agreement_operation` (
    `operation_id`, `action`, `object_type`, `object_id`, `operator`, `operated_at`, `result`, `trace_id`
) VALUES
    ('OPShhJUUEtUeFy9i2SROvWu', 'FREEZE', 'AGREEMENT_REVISION', 'REV7j5iUq5TLb0HJ_1tlRyI', 'init', UTC_TIMESTAMP(3), 'SUCCESS', NULL),
    ('OPSpAnmKBocu3FGKRbXq0Jz', 'FREEZE', 'AGREEMENT_REVISION', 'REVZEtIDz5NW1b5kUvgvsPb', 'init', UTC_TIMESTAMP(3), 'SUCCESS', NULL),
    ('OPSkPiEx2LHNL_bkMvm390s', 'PUBLISH', 'AGREEMENT_BUNDLE', 'BDLkjQyv9B4N3Ud9h00IdGb', 'init', UTC_TIMESTAMP(3), 'SUCCESS', NULL)
ON DUPLICATE KEY UPDATE
    `operated_at` = VALUES(`operated_at`),
    `result` = VALUES(`result`);

