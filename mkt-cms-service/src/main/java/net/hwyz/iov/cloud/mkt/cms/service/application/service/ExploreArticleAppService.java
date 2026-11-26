package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreArticleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreContentPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreArticleView;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreSourceType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreGuard;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreJson;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExplorePageList;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.HexSha256;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.ExploreHtmlSanitizer;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreMediaValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探索页文章应用服务（官方富文本与 UGC 引用、不可变修订发布/下线）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreArticleAppService {

    private final ExploreArticleRepository articleRepository;
    private final ExploreOperationRepository operationRepository;
    private final ExploreHtmlSanitizer htmlSanitizer;
    private final ExploreMediaValidator mediaValidator;
    private final Clock clock;

    /**
     * 管理端检索（物理分页由调用方 PageHelper 提供）
     *
     * @param criteria 检索条件
     * @return 文章管理视图列表
     */
    public List<ExploreArticleView> search(ExploreArticleSearchCriteria criteria) {
        return ExplorePageList.map(articleRepository.search(criteria), article -> {
            ExploreArticleRevision revision = articleRepository
                    .findLatestDraft(article.getArticleId())
                    .or(() -> article.getCurrentRevisionId() == null
                            ? articleRepository.findLatestRevision(article.getArticleId())
                            : articleRepository.findRevision(article.getCurrentRevisionId()))
                    .orElse(null);
            return new ExploreArticleView(article, withImages(revision));
        });
    }

    /**
     * 查询文章（优先返回最新草稿修订）
     *
     * @param articleId 文章标识
     * @return 文章管理视图
     */
    public ExploreArticleView get(String articleId) {
        ExploreArticle article = articleRepository.findByArticleId(articleId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "文章不存在: " + articleId));
        ExploreArticleRevision revision = articleRepository.findLatestDraft(articleId)
                .or(() -> article.getCurrentRevisionId() == null
                        ? articleRepository.findLatestRevision(articleId)
                        : articleRepository.findRevision(article.getCurrentRevisionId()))
                .orElse(null);
        return new ExploreArticleView(article, withImages(revision));
    }

    /**
     * 新建文章（根实体 + 草稿修订）
     *
     * @param cmd 新建命令
     * @return 文章管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreArticleView create(ExploreArticleCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        ExploreSourceType sourceType = ExploreSourceType.parse(cmd.getSourceType());
        Instant now = Instant.now(clock);
        String articleId = ExploreIdGenerator.articleId();
        ExploreArticle article = ExploreArticle.create(articleId, sourceType, operator, now);
        ExploreArticleRevision revision = buildRevision(articleId, 1, sourceType, cmd, operator, now);
        articleRepository.insert(article);
        articleRepository.insertRevision(revision);
        articleRepository.replaceRevisionImages(revision.getRevisionId(), revision.getImages());
        audit(ExploreAction.CREATE, articleId, operator, now, "sourceType=" + sourceType.name());
        log.info("探索页文章创建成功: articleId={}, sourceType={}, operator={}", articleId, sourceType, operator);
        return new ExploreArticleView(article, revision);
    }

    /**
     * 修改文章：存在草稿修订时原地更新，否则创建新草稿修订（已发布修订永不覆盖）
     *
     * @param cmd 修改命令
     * @return 文章管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreArticleView update(ExploreArticleCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        int expectedLockVersion = ExploreGuard.requireLockVersion(cmd.getLockVersion());
        String articleId = ExploreGuard.requireText(cmd.getArticleId(), "文章标识", 64);
        ExploreArticle article = articleRepository.findByArticleIdForUpdate(articleId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "文章不存在: " + articleId));
        if (article.getLockVersion() != expectedLockVersion) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "文章已被修改，请基于最新乐观锁版本重试");
        }
        Instant now = Instant.now(clock);
        ExploreArticleRevision revision;
        ExploreArticleRevision draft = articleRepository.findLatestDraft(articleId).orElse(null);
        if (draft != null) {
            revision = buildRevision(articleId, draft.getRevisionNo(), article.getSourceType(), cmd, operator, now)
                    .toBuilder()
                    .revisionId(draft.getRevisionId())
                    .createBy(draft.getCreateBy())
                    .createTime(draft.getCreateTime())
                    .build();
            if (articleRepository.updateDraftRevision(revision) == 0) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                        "草稿修订已被并发修改，请重试");
            }
            articleRepository.replaceRevisionImages(revision.getRevisionId(), revision.getImages());
        } else {
            int nextRevisionNo = articleRepository.findLatestRevision(articleId)
                    .map(latest -> latest.getRevisionNo() + 1)
                    .orElse(1);
            revision = buildRevision(articleId, nextRevisionNo, article.getSourceType(), cmd, operator, now);
            articleRepository.insertRevision(revision);
            articleRepository.replaceRevisionImages(revision.getRevisionId(), revision.getImages());
        }
        ExploreArticle updated = article.touched(operator, now);
        if (articleRepository.update(updated, expectedLockVersion) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "文章已被并发修改，请重试");
        }
        audit(ExploreAction.UPDATE, articleId, operator, now, "revisionNo=" + revision.getRevisionNo());
        log.info("探索页文章修改成功: articleId={}, revisionNo={}, operator={}",
                articleId, revision.getRevisionNo(), operator);
        return new ExploreArticleView(updated, revision);
    }

    /**
     * 发布指定草稿修订（默认最新草稿）
     *
     * @param cmd 发布命令
     * @return 文章管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreArticleView publish(ExploreContentPublishCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String articleId = ExploreGuard.requireText(cmd.getObjectId(), "文章标识", 64);
        ExploreArticle article = articleRepository.findByArticleIdForUpdate(articleId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "文章不存在: " + articleId));
        ExploreArticleRevision revision = StringUtils.hasText(cmd.getRevisionId())
                ? articleRepository.findRevision(cmd.getRevisionId().trim()).orElse(null)
                : articleRepository.findLatestDraft(articleId).orElse(null);
        if (revision == null || !articleId.equals(revision.getArticleId())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND, "待发布文章修订不存在");
        }
        if (revision.getStatus() == ExploreRevisionStatus.PUBLISHED) {
            if (revision.getRevisionId().equals(article.getCurrentRevisionId())) {
                return new ExploreArticleView(article, withImages(revision));
            }
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "已发布修订不可重复发布，请创建新草稿修订");
        }
        validateRevivable(article.getSourceType(), revision);
        Instant now = Instant.now(clock);
        ExploreArticleRevision published = revision.published(operator, now);
        if (articleRepository.publishRevision(published) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "修订状态已变化，发布失败");
        }
        ExploreArticle updated = article.published(revision.getRevisionId(), operator, now);
        if (articleRepository.update(updated, article.getLockVersion()) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "文章已被并发修改，发布失败");
        }
        audit(ExploreAction.PUBLISH, articleId, operator, now,
                "revisionId=" + revision.getRevisionId());
        log.info("探索页文章发布成功: articleId={}, revisionId={}, operator={}",
                articleId, revision.getRevisionId(), operator);
        return new ExploreArticleView(updated, withImages(published));
    }

    /**
     * 立即下线文章（保留历史）
     *
     * @param cmd 下线命令
     * @return 文章管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreArticleView offline(ExploreContentPublishCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String articleId = ExploreGuard.requireText(cmd.getObjectId(), "文章标识", 64);
        ExploreArticle article = articleRepository.findByArticleIdForUpdate(articleId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "文章不存在: " + articleId));
        if (article.getStatus() == ExploreContentStatus.OFFLINE) {
            return new ExploreArticleView(article, currentRevision(article));
        }
        Instant now = Instant.now(clock);
        ExploreArticle updated = article.offline(operator, now);
        if (articleRepository.update(updated, article.getLockVersion()) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "文章已被并发修改，下线失败");
        }
        audit(ExploreAction.OFFLINE, articleId, operator, now, null);
        log.info("探索页文章下线成功: articleId={}, operator={}", articleId, operator);
        return new ExploreArticleView(updated, currentRevision(updated));
    }

    private ExploreArticleRevision currentRevision(ExploreArticle article) {
        if (article.getCurrentRevisionId() == null) {
            return withImages(articleRepository.findLatestRevision(article.getArticleId()).orElse(null));
        }
        return withImages(articleRepository.findRevision(article.getCurrentRevisionId()).orElse(null));
    }

    private ExploreArticleRevision buildRevision(String articleId, int revisionNo, ExploreSourceType sourceType,
                                                 ExploreArticleCmd cmd, String operator, Instant now) {
        String title = ExploreGuard.requireText(cmd.getTitle(), "标题", 255);
        String summary = ExploreGuard.optionalText(cmd.getSummary(), "摘要", 1024);
        List<ExploreImage> images = normalizeImages(cmd);
        ExploreImage cover = images.isEmpty() ? null : images.get(0);
        String authorName = ExploreGuard.optionalText(cmd.getAuthorName(), "作者显示名", 128);
        String avatarUrl = ExploreGuard.optionalText(cmd.getAuthorAvatarUrl(), "作者头像", 1024);
        if (avatarUrl != null) {
            mediaValidator.validateAssetUrl(avatarUrl, "作者头像");
        }
        Instant displayAt = cmd.getDisplayAt() == null ? now : cmd.getDisplayAt();

        ExploreArticleRevision.ExploreArticleRevisionBuilder builder = ExploreArticleRevision.builder()
                .revisionId(ExploreIdGenerator.articleRevisionId())
                .articleId(articleId)
                .revisionNo(revisionNo)
                .status(ExploreRevisionStatus.DRAFT)
                .title(title)
                .summary(summary)
                .cover(cover)
                .images(images)
                .authorName(authorName)
                .authorAvatarUrl(avatarUrl)
                .displayAt(displayAt)
                .createBy(operator)
                .createTime(now)
                .modifyBy(operator)
                .modifyTime(now);

        if (sourceType == ExploreSourceType.OFFICIAL) {
            SanitizedHtml sanitized = htmlSanitizer.sanitize(cmd.getBody());
            builder.contentType(SanitizedHtml.CONTENT_TYPE)
                    .body(sanitized.getBody())
                    .contentHash(HexSha256.ofUtf8(sanitized.getBody() + ExploreJson.write(imageHashPayload(images))))
                    .sanitizerVersion(sanitized.getSanitizerVersion());
        } else {
            validateUgc(cmd);
            builder.body(null)
                    .contentType(null)
                    .sourceContentId(cmd.getSourceContentId().trim())
                    .nativeRouteCode(cmd.getNativeRouteCode().trim())
                    .contentHash(ugcHash(title, summary, images, authorName, avatarUrl, displayAt,
                            cmd.getSourceContentId().trim(), cmd.getNativeRouteCode().trim()));
        }
        return builder.build();
    }

    private void validateUgc(ExploreArticleCmd cmd) {
        if (StringUtils.hasText(cmd.getBody())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "UGC 引用不得保存正文，仅保存展示快照");
        }
        String sourceContentId = ExploreGuard.requireText(cmd.getSourceContentId(), "来源内容标识", 128);
        String routeCode = ExploreGuard.requireText(cmd.getNativeRouteCode(), "原生路由编码", 128);
        if (!sourceContentId.isEmpty()) {
            mediaValidator.validateNativeRouteCode(routeCode);
        }
    }

    private void validateRevivable(ExploreSourceType sourceType, ExploreArticleRevision revision) {
        if (sourceType == ExploreSourceType.OFFICIAL) {
            if (!StringUtils.hasText(revision.getBody())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "官方文章正文不能为空");
            }
            return;
        }
        if (!StringUtils.hasText(revision.getSourceContentId())
                || !StringUtils.hasText(revision.getNativeRouteCode())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "UGC 引用必须包含来源内容标识与原生路由编码");
        }
    }

    private String ugcHash(String title, String summary, List<ExploreImage> images, String authorName,
                           String avatarUrl, Instant displayAt, String sourceContentId, String routeCode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceType", ExploreSourceType.UGC_REFERENCE.name());
        payload.put("title", title);
        payload.put("summary", summary);
        payload.put("images", imageHashPayload(images));
        payload.put("authorName", authorName);
        payload.put("authorAvatarUrl", avatarUrl);
        payload.put("displayAt", displayAt == null ? null : displayAt.toString());
        payload.put("sourceContentId", sourceContentId);
        payload.put("nativeRouteCode", routeCode);
        return HexSha256.ofUtf8(ExploreJson.write(payload));
    }

    private List<ExploreImage> normalizeImages(ExploreArticleCmd cmd) {
        List<ExploreImage> images = cmd.getImages() == null ? List.of() : cmd.getImages();
        if (images.isEmpty() && cmd.getCover() != null) {
            images = List.of(cmd.getCover());
        }
        if (images.size() > 9) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "文章图片最多 9 张");
        }
        for (int index = 0; index < images.size(); index++) {
            ExploreImage image = images.get(index);
            if (image == null) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "第 " + (index + 1) + " 张图片不能为空");
            }
            mediaValidator.validateRequiredImage(image, "第 " + (index + 1) + " 张图片");
        }
        return List.copyOf(images);
    }

    private List<Map<String, Object>> imageHashPayload(List<ExploreImage> images) {
        List<Map<String, Object>> payload = new java.util.ArrayList<>();
        for (ExploreImage image : images) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("url", image.getUrl());
            view.put("width", image.getWidth());
            view.put("height", image.getHeight());
            view.put("mime", image.getMime());
            view.put("size", image.getSize());
            payload.add(view);
        }
        return payload;
    }

    private ExploreArticleRevision withImages(ExploreArticleRevision revision) {
        if (revision == null) {
            return null;
        }
        List<ExploreImage> images = articleRepository.findRevisionImages(revision.getRevisionId());
        if (images.isEmpty() && revision.getCover() != null) {
            images = List.of(revision.getCover());
        }
        return revision.toBuilder().images(images).build();
    }

    private void audit(ExploreAction action, String articleId, String operator, Instant now, String detail) {
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                action, "EXPLORE_ARTICLE", articleId, operator, now, null,
                TraceContext.currentTraceId(), detail));
    }
}
