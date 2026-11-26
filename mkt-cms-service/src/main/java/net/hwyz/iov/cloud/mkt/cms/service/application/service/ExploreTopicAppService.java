package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreContentPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreTopicCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreTopicView;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
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
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreContentSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreTopicRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreDisplaySnapshot;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreMediaValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探索页专题应用服务（不可变专题修订、有序文章条目、目标校验与发布/下线）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreTopicAppService {

    private final ExploreTopicRepository topicRepository;
    private final ExploreArticleRepository articleRepository;
    private final ExploreOperationRepository operationRepository;
    private final ExploreMediaValidator mediaValidator;
    private final ExploreTargetAppService targetAppService;
    private final Clock clock;

    public List<ExploreTopicView> search(ExploreContentSearchCriteria criteria) {
        return ExplorePageList.map(topicRepository.search(criteria), topic -> {
            ExploreTopicRevision revision = latestRevision(topic);
            return new ExploreTopicView(topic, revision,
                    revision == null ? List.of() : topicRepository.listItems(revision.getRevisionId()));
        });
    }

    public ExploreTopicView get(String topicId) {
        ExploreTopic topic = topicRepository.findByTopicId(topicId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "专题不存在: " + topicId));
        ExploreTopicRevision revision = latestRevision(topic);
        return new ExploreTopicView(topic, revision,
                revision == null ? List.of() : topicRepository.listItems(revision.getRevisionId()));
    }

    /**
     * 新建专题（根实体 + 草稿修订 + 有序文章条目）
     *
     * @param cmd 新建命令
     * @return 专题管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreTopicView create(ExploreTopicCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        Instant now = Instant.now(clock);
        String topicId = ExploreIdGenerator.topicId();
        ExploreTopic topic = ExploreTopic.create(topicId, operator, now);
        ExploreTopicRevision revision = buildRevision(topicId, 1, cmd, operator, now);
        topicRepository.insert(topic);
        topicRepository.insertRevision(revision);
        topicRepository.replaceItems(revision.getRevisionId(),
                buildItems(revision.getRevisionId(), cmd.getArticleRevisionIds(), operator, now));
        audit(ExploreAction.CREATE, topicId, operator, now, null);
        log.info("探索页专题创建成功: topicId={}, operator={}", topicId, operator);
        return get(topicId);
    }

    /**
     * 修改专题：草稿修订原地更新，否则创建新草稿修订
     *
     * @param cmd 修改命令
     * @return 专题管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreTopicView update(ExploreTopicCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        int expectedLockVersion = ExploreGuard.requireLockVersion(cmd.getLockVersion());
        String topicId = ExploreGuard.requireText(cmd.getTopicId(), "专题标识", 64);
        ExploreTopic topic = topicRepository.findByTopicIdForUpdate(topicId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "专题不存在: " + topicId));
        if (topic.getLockVersion() != expectedLockVersion) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "专题已被修改，请基于最新乐观锁版本重试");
        }
        Instant now = Instant.now(clock);
        ExploreTopicRevision draft = topicRepository.findLatestDraft(topicId).orElse(null);
        ExploreTopicRevision revision;
        if (draft != null) {
            revision = buildRevision(topicId, draft.getRevisionNo(), cmd, operator, now)
                    .toBuilder()
                    .revisionId(draft.getRevisionId())
                    .createBy(draft.getCreateBy())
                    .createTime(draft.getCreateTime())
                    .build();
            if (topicRepository.updateDraftRevision(revision) == 0) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                        "专题草稿修订已被并发修改，请重试");
            }
        } else {
            int nextRevisionNo = topicRepository.findLatestRevision(topicId)
                    .map(latest -> latest.getRevisionNo() + 1)
                    .orElse(1);
            revision = buildRevision(topicId, nextRevisionNo, cmd, operator, now);
            topicRepository.insertRevision(revision);
        }
        topicRepository.replaceItems(revision.getRevisionId(),
                buildItems(revision.getRevisionId(), cmd.getArticleRevisionIds(), operator, now));
        ExploreTopic updated = topic.touched(operator, now);
        if (topicRepository.update(updated, expectedLockVersion) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, "专题已被并发修改，请重试");
        }
        audit(ExploreAction.UPDATE, topicId, operator, now, "revisionNo=" + revision.getRevisionNo());
        log.info("探索页专题修改成功: topicId={}, revisionNo={}, operator={}",
                topicId, revision.getRevisionNo(), operator);
        return get(topicId);
    }

    /**
     * 发布指定草稿修订（默认最新草稿）
     *
     * @param cmd 发布命令
     * @return 专题管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreTopicView publish(ExploreContentPublishCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String topicId = ExploreGuard.requireText(cmd.getObjectId(), "专题标识", 64);
        ExploreTopic topic = topicRepository.findByTopicIdForUpdate(topicId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "专题不存在: " + topicId));
        ExploreTopicRevision revision = StringUtils.hasText(cmd.getRevisionId())
                ? topicRepository.findRevision(cmd.getRevisionId().trim()).orElse(null)
                : topicRepository.findLatestDraft(topicId).orElse(null);
        if (revision == null || !topicId.equals(revision.getTopicId())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND, "待发布专题修订不存在");
        }
        if (revision.getStatus() == ExploreRevisionStatus.PUBLISHED) {
            if (revision.getRevisionId().equals(topic.getCurrentRevisionId())) {
                return get(topicId);
            }
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "已发布修订不可重复发布，请创建新草稿修订");
        }
        ExploreTarget target = revision.getTarget();
        if (target != null && target.getType() != null) {
            targetAppService.validateTopicTarget(target, topicId);
            targetAppService.validate(target);
        }
        validateItemsForPublish(revision.getRevisionId());
        Instant now = Instant.now(clock);
        ExploreTopicRevision published = revision.published(operator, now);
        if (topicRepository.publishRevision(published) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, "修订状态已变化，发布失败");
        }
        ExploreTopic updated = topic.published(revision.getRevisionId(), operator, now);
        if (topicRepository.update(updated, topic.getLockVersion()) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "专题已被并发修改，发布失败");
        }
        audit(ExploreAction.PUBLISH, topicId, operator, now, "revisionId=" + revision.getRevisionId());
        log.info("探索页专题发布成功: topicId={}, revisionId={}", topicId, revision.getRevisionId());
        return get(topicId);
    }

    /**
     * 立即下线专题
     *
     * @param cmd 下线命令
     * @return 专题管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreTopicView offline(ExploreContentPublishCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String topicId = ExploreGuard.requireText(cmd.getObjectId(), "专题标识", 64);
        ExploreTopic topic = topicRepository.findByTopicIdForUpdate(topicId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "专题不存在: " + topicId));
        if (topic.getStatus() == ExploreContentStatus.OFFLINE) {
            return get(topicId);
        }
        Instant now = Instant.now(clock);
        ExploreTopic updated = topic.offline(operator, now);
        if (topicRepository.update(updated, topic.getLockVersion()) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, "专题已被并发修改，下线失败");
        }
        audit(ExploreAction.OFFLINE, topicId, operator, now, null);
        log.info("探索页专题下线成功: topicId={}", topicId);
        return get(topicId);
    }

    private ExploreTopicRevision latestRevision(ExploreTopic topic) {
        return topicRepository.findLatestDraft(topic.getTopicId())
                .or(() -> topic.getCurrentRevisionId() == null
                        ? topicRepository.findLatestRevision(topic.getTopicId())
                        : topicRepository.findRevision(topic.getCurrentRevisionId()))
                .orElse(null);
    }

    private ExploreTopicRevision buildRevision(String topicId, int revisionNo, ExploreTopicCmd cmd,
                                               String operator, Instant now) {
        String name = ExploreGuard.requireText(cmd.getName(), "专题名称", 128);
        String summary = ExploreGuard.optionalText(cmd.getSummary(), "专题摘要", 1024);
        ExploreImage cover = cmd.getCover();
        mediaValidator.validateOptionalImage(cover, "专题封面");
        ExploreTarget target = cmd.getTarget();
        if (target != null && target.getType() != null) {
            targetAppService.validateTopicTarget(target, topicId);
            targetAppService.validate(target);
        }
        List<String> articleRevisionIds = normalizeRevisionIds(cmd.getArticleRevisionIds());
        return ExploreTopicRevision.builder()
                .revisionId(ExploreIdGenerator.topicRevisionId())
                .topicId(topicId)
                .revisionNo(revisionNo)
                .status(ExploreRevisionStatus.DRAFT)
                .name(name)
                .summary(summary)
                .cover(cover)
                .target(target)
                .contentHash(contentHash(name, summary, cover, target, articleRevisionIds))
                .createBy(operator)
                .createTime(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    private List<ExploreTopicItem> buildItems(String topicRevisionId, List<String> articleRevisionIds,
                                              String operator, Instant now) {
        List<String> revisionIds = normalizeRevisionIds(articleRevisionIds);
        List<ExploreTopicItem> items = new ArrayList<>();
        int sortNo = 10;
        for (String revisionId : revisionIds) {
            ExploreArticleRevision revision = articleRepository.findRevision(revisionId)
                    .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                            "专题条目文章修订不存在: " + revisionId));
            if (revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "专题条目必须引用已发布文章修订: " + revisionId);
            }
            ExploreArticle article = articleRepository.findByArticleId(revision.getArticleId())
                    .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                            "专题条目文章不存在: " + revision.getArticleId()));
            if (article.getStatus() != ExploreContentStatus.PUBLISHED) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "专题条目引用的文章已下线: " + revision.getArticleId());
            }
            Map<String, Object> snapshot = ExploreDisplaySnapshot.article(
                    article.getArticleId(), revision.getRevisionId(), article.getSourceType().name(),
                    revision.getTitle(), revision.getSummary(), revision.getCover(),
                    revision.getAuthorName(), revision.getAuthorAvatarUrl(),
                    revision.getDisplayAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(revision.getDisplayAt()));
            items.add(ExploreTopicItem.builder()
                    .topicRevisionId(topicRevisionId)
                    .articleRevisionId(revisionId)
                    .sortNo(sortNo)
                    .displaySnapshot(snapshot)
                    .createBy(operator)
                    .createTime(now)
                    .modifyBy(operator)
                    .modifyTime(now)
                    .build());
            sortNo += 10;
        }
        return items;
    }

    private void validateItemsForPublish(String topicRevisionId) {
        for (ExploreTopicItem item : topicRepository.listItems(topicRevisionId)) {
            ExploreArticleRevision revision = articleRepository.findRevision(item.getArticleRevisionId())
                    .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                            "专题条目文章修订不存在: " + item.getArticleRevisionId()));
            if (revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "专题条目引用的文章修订未发布: " + item.getArticleRevisionId());
            }
            ExploreArticle article = articleRepository.findByArticleId(revision.getArticleId())
                    .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                            "专题条目文章不存在: " + revision.getArticleId()));
            if (article.getStatus() != ExploreContentStatus.PUBLISHED) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "专题条目引用的文章已下线，无法发布专题: " + revision.getArticleId());
            }
        }
    }

    private List<String> normalizeRevisionIds(List<String> articleRevisionIds) {
        if (articleRevisionIds == null || articleRevisionIds.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String id : articleRevisionIds) {
            if (!StringUtils.hasText(id)) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "专题条目文章修订标识不能为空");
            }
            if (ids.contains(id.trim())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "专题条目不允许重复引用同一文章修订: " + id);
            }
            ids.add(id.trim());
        }
        return ids;
    }

    private String contentHash(String name, String summary, ExploreImage cover, ExploreTarget target,
                               List<String> articleRevisionIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("summary", summary);
        payload.put("coverUrl", cover == null ? null : cover.getUrl());
        payload.put("target", target == null ? null : target.toView());
        payload.put("articleRevisionIds", articleRevisionIds);
        return HexSha256.ofUtf8(ExploreJson.write(payload));
    }

    private void audit(ExploreAction action, String topicId, String operator, Instant now, String detail) {
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                action, "EXPLORE_TOPIC", topicId, operator, now, null,
                TraceContext.currentTraceId(), detail));
    }
}
