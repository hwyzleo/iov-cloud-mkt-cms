package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreTopicRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreMediaValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 探索页跳转目标校验与解析。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.3 / AC-031 / AC-036：ARTICLE / TOPIC 目标必须指向存在且已发布、
 * 根实体未下线的修订；H5 / NATIVE 走服务端白名单；专题目标不允许形成循环。
 *
 * @author hwyz_leo
 */
@Service
@RequiredArgsConstructor
public class ExploreTargetAppService {

    private final ExploreMediaValidator mediaValidator;
    private final ExploreArticleRepository articleRepository;
    private final ExploreTopicRepository topicRepository;

    /**
     * 校验目标结构与存在性
     *
     * @param target 跳转目标
     */
    public void validate(ExploreTarget target) {
        mediaValidator.validateTarget(target);
        switch (target.getType()) {
            case ARTICLE -> validateArticleTarget(target);
            case TOPIC -> validateTopicTarget(target, null);
            case H5, NATIVE -> {
                // 白名单已在结构校验中完成
            }
            default -> throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的跳转目标");
        }
    }

    /**
     * 校验可选目标（为空时跳过）
     *
     * @param target 跳转目标
     */
    public void validateOptional(ExploreTarget target) {
        if (target != null && target.getType() != null) {
            validate(target);
        }
    }

    /**
     * 校验专题目标，拒绝自引用与一跳循环
     *
     * @param target        目标
     * @param currentTopicId 当前专题标识（可为空）
     */
    public void validateTopicTarget(ExploreTarget target, String currentTopicId) {
        if (target.getType() != ExploreTargetType.TOPIC) {
            return;
        }
        if (StringUtils.hasText(currentTopicId) && currentTopicId.equals(target.getRefId())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "专题目标不允许指向自身");
        }
        ExploreTopicRevision revision = topicRepository.findRevision(target.getRevisionId())
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "专题目标修订不存在"));
        if (!target.getRefId().equals(revision.getTopicId())
                || revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "专题目标必须指向已发布专题修订");
        }
        ExploreTopic topic = topicRepository.findByTopicId(target.getRefId())
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "专题目标不存在"));
        if (topic.getStatus() != ExploreContentStatus.PUBLISHED) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "专题目标已下线");
        }
        ExploreTarget nested = revision.getTarget();
        if (StringUtils.hasText(currentTopicId) && nested != null
                && nested.getType() == ExploreTargetType.TOPIC
                && currentTopicId.equals(nested.getRefId())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "专题目标形成循环引用");
        }
    }

    private void validateArticleTarget(ExploreTarget target) {
        ExploreArticleRevision revision = articleRepository.findRevision(target.getRevisionId())
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "文章目标修订不存在"));
        if (!target.getRefId().equals(revision.getArticleId())
                || revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "文章目标必须指向已发布文章修订");
        }
        ExploreArticle article = articleRepository.findByArticleId(target.getRefId())
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "文章目标不存在"));
        if (article.getStatus() != ExploreContentStatus.PUBLISHED) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "文章目标已下线");
        }
    }
}
