package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreComponentType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreItemType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreSourceType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreJson;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBanner;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBannerRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageComponent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageCurrent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreArticlePo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreArticleRevisionPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreBannerPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreBannerRevisionPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreChannelPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreChannelRevisionPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreOperationPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageComponentPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageCurrentPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageItemPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePagePo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreTopicItemPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreTopicPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreTopicRevisionPo;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探索页领域对象与持久化对象显式转换。
 * <p>
 * MKT-CMS-DSN-CR-003 §6.1：跨层使用显式转换，不把 PO 泄漏到领域/应用层；
 * JSON 列在此完成确定性序列化与解析。
 *
 * @author hwyz_leo
 */
public final class ExploreConverter {

    private ExploreConverter() {
    }

    public static ExploreChannelPo fromDomain(ExploreChannel domain) {
        ExploreChannelPo po = new ExploreChannelPo();
        po.setId(domain.getId());
        po.setChannelCode(domain.getChannelCode());
        po.setName(domain.getName());
        po.setSortNo(domain.getSortNo());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDraftRevisionId(domain.getDraftRevisionId());
        po.setCurrentRevisionId(domain.getCurrentRevisionId());
        po.setLockVersion(domain.getLockVersion());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreChannel toDomain(ExploreChannelPo po) {
        return ExploreChannel.builder()
                .id(po.getId())
                .channelCode(po.getChannelCode())
                .name(po.getName())
                .sortNo(po.getSortNo() == null ? 0 : po.getSortNo())
                .status(po.getStatus() == null ? null : ExploreChannelStatus.valueOf(po.getStatus()))
                .draftRevisionId(po.getDraftRevisionId())
                .currentRevisionId(po.getCurrentRevisionId())
                .lockVersion(po.getLockVersion() == null ? 0 : po.getLockVersion())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExploreChannelRevisionPo fromDomain(ExploreChannelRevision domain) {
        ExploreChannelRevisionPo po = new ExploreChannelRevisionPo();
        po.setId(domain.getId());
        po.setRevisionId(domain.getRevisionId());
        po.setChannelCode(domain.getChannelCode());
        po.setRevisionNo(domain.getRevisionNo());
        po.setName(domain.getName());
        po.setSortNo(domain.getSortNo());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setRevisionStatus(domain.getRevisionStatus() == null ? null : domain.getRevisionStatus().name());
        po.setContentHash(domain.getContentHash());
        po.setPublishedBy(domain.getPublishedBy());
        po.setPublishedAt(domain.getPublishedAt());
        po.setRequestId(domain.getRequestId());
        po.setRequestHash(domain.getRequestHash());
        po.setExpectedCurrentRevisionId(domain.getExpectedCurrentRevisionId());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreChannelRevision toDomain(ExploreChannelRevisionPo po) {
        return ExploreChannelRevision.builder()
                .id(po.getId())
                .revisionId(po.getRevisionId())
                .channelCode(po.getChannelCode())
                .revisionNo(po.getRevisionNo() == null ? 0 : po.getRevisionNo())
                .name(po.getName())
                .sortNo(po.getSortNo() == null ? 0 : po.getSortNo())
                .status(po.getStatus() == null ? null : ExploreChannelStatus.valueOf(po.getStatus()))
                .revisionStatus(ExploreRevisionStatus.valueOf(po.getRevisionStatus()))
                .contentHash(po.getContentHash())
                .publishedBy(po.getPublishedBy())
                .publishedAt(po.getPublishedAt())
                .requestId(po.getRequestId())
                .requestHash(po.getRequestHash())
                .expectedCurrentRevisionId(po.getExpectedCurrentRevisionId())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExploreArticlePo fromDomain(ExploreArticle domain) {
        ExploreArticlePo po = new ExploreArticlePo();
        po.setId(domain.getId());
        po.setArticleId(domain.getArticleId());
        po.setSourceType(domain.getSourceType() == null ? null : domain.getSourceType().name());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setCurrentRevisionId(domain.getCurrentRevisionId());
        po.setLockVersion(domain.getLockVersion());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreArticle toDomain(ExploreArticlePo po) {
        return ExploreArticle.builder()
                .id(po.getId())
                .articleId(po.getArticleId())
                .sourceType(po.getSourceType() == null ? null : ExploreSourceType.valueOf(po.getSourceType()))
                .status(po.getStatus() == null ? null : ExploreContentStatus.valueOf(po.getStatus()))
                .currentRevisionId(po.getCurrentRevisionId())
                .lockVersion(po.getLockVersion() == null ? 0 : po.getLockVersion())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExploreArticleRevisionPo fromDomain(ExploreArticleRevision domain) {
        ExploreArticleRevisionPo po = new ExploreArticleRevisionPo();
        po.setId(domain.getId());
        po.setRevisionId(domain.getRevisionId());
        po.setArticleId(domain.getArticleId());
        po.setRevisionNo(domain.getRevisionNo());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setTitle(domain.getTitle());
        po.setSummary(domain.getSummary());
        putImage(po, domain.getCover());
        po.setAuthorName(domain.getAuthorName());
        po.setAuthorAvatarUrl(domain.getAuthorAvatarUrl());
        po.setDisplayAt(domain.getDisplayAt());
        po.setContentType(domain.getContentType());
        po.setBody(domain.getBody());
        po.setContentHash(domain.getContentHash());
        po.setSanitizerVersion(domain.getSanitizerVersion());
        po.setSourceContentId(domain.getSourceContentId());
        po.setNativeRouteCode(domain.getNativeRouteCode());
        po.setPublishedAt(domain.getPublishedAt());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreArticleRevision toDomain(ExploreArticleRevisionPo po) {
        return ExploreArticleRevision.builder()
                .id(po.getId())
                .revisionId(po.getRevisionId())
                .articleId(po.getArticleId())
                .revisionNo(po.getRevisionNo() == null ? 0 : po.getRevisionNo())
                .status(po.getStatus() == null ? null : ExploreRevisionStatus.valueOf(po.getStatus()))
                .title(po.getTitle())
                .summary(po.getSummary())
                .cover(image(po.getCoverUrl(), po.getCoverWidth(), po.getCoverHeight(),
                        po.getCoverMime(), po.getCoverSize()))
                .authorName(po.getAuthorName())
                .authorAvatarUrl(po.getAuthorAvatarUrl())
                .displayAt(po.getDisplayAt())
                .contentType(po.getContentType())
                .body(po.getBody())
                .contentHash(po.getContentHash())
                .sanitizerVersion(po.getSanitizerVersion())
                .sourceContentId(po.getSourceContentId())
                .nativeRouteCode(po.getNativeRouteCode())
                .publishedAt(po.getPublishedAt())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExploreTopicPo fromDomain(ExploreTopic domain) {
        ExploreTopicPo po = new ExploreTopicPo();
        po.setId(domain.getId());
        po.setTopicId(domain.getTopicId());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setCurrentRevisionId(domain.getCurrentRevisionId());
        po.setLockVersion(domain.getLockVersion());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreTopic toDomain(ExploreTopicPo po) {
        return ExploreTopic.builder()
                .id(po.getId())
                .topicId(po.getTopicId())
                .status(po.getStatus() == null ? null : ExploreContentStatus.valueOf(po.getStatus()))
                .currentRevisionId(po.getCurrentRevisionId())
                .lockVersion(po.getLockVersion() == null ? 0 : po.getLockVersion())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExploreTopicRevisionPo fromDomain(ExploreTopicRevision domain) {
        ExploreTopicRevisionPo po = new ExploreTopicRevisionPo();
        po.setId(domain.getId());
        po.setRevisionId(domain.getRevisionId());
        po.setTopicId(domain.getTopicId());
        po.setRevisionNo(domain.getRevisionNo());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setName(domain.getName());
        po.setSummary(domain.getSummary());
        putImage(po, domain.getCover());
        putTarget(po, domain.getTarget());
        po.setContentHash(domain.getContentHash());
        po.setPublishedAt(domain.getPublishedAt());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreTopicRevision toDomain(ExploreTopicRevisionPo po) {
        return ExploreTopicRevision.builder()
                .id(po.getId())
                .revisionId(po.getRevisionId())
                .topicId(po.getTopicId())
                .revisionNo(po.getRevisionNo() == null ? 0 : po.getRevisionNo())
                .status(po.getStatus() == null ? null : ExploreRevisionStatus.valueOf(po.getStatus()))
                .name(po.getName())
                .summary(po.getSummary())
                .cover(image(po.getCoverUrl(), po.getCoverWidth(), po.getCoverHeight(),
                        po.getCoverMime(), po.getCoverSize()))
                .target(target(po.getTargetType(), po.getTargetRefId(), po.getTargetRevisionId(),
                        po.getTargetUrl(), po.getTargetRouteCode(), po.getTargetParams()))
                .contentHash(po.getContentHash())
                .publishedAt(po.getPublishedAt())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExploreTopicItemPo fromDomain(ExploreTopicItem domain) {
        ExploreTopicItemPo po = new ExploreTopicItemPo();
        po.setId(domain.getId());
        po.setTopicRevisionId(domain.getTopicRevisionId());
        po.setArticleRevisionId(domain.getArticleRevisionId());
        po.setSortNo(domain.getSortNo());
        po.setDisplaySnapshot(domain.getDisplaySnapshot() == null ? null
                : ExploreJson.write(domain.getDisplaySnapshot()));
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreTopicItem toDomain(ExploreTopicItemPo po) {
        return ExploreTopicItem.builder()
                .id(po.getId())
                .topicRevisionId(po.getTopicRevisionId())
                .articleRevisionId(po.getArticleRevisionId())
                .sortNo(po.getSortNo() == null ? 0 : po.getSortNo())
                .displaySnapshot(ExploreJson.readMap(po.getDisplaySnapshot()))
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExploreBannerPo fromDomain(ExploreBanner domain) {
        ExploreBannerPo po = new ExploreBannerPo();
        po.setId(domain.getId());
        po.setBannerId(domain.getBannerId());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setCurrentRevisionId(domain.getCurrentRevisionId());
        po.setLockVersion(domain.getLockVersion());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreBanner toDomain(ExploreBannerPo po) {
        return ExploreBanner.builder()
                .id(po.getId())
                .bannerId(po.getBannerId())
                .status(po.getStatus() == null ? null : ExploreContentStatus.valueOf(po.getStatus()))
                .currentRevisionId(po.getCurrentRevisionId())
                .lockVersion(po.getLockVersion() == null ? 0 : po.getLockVersion())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExploreBannerRevisionPo fromDomain(ExploreBannerRevision domain) {
        ExploreBannerRevisionPo po = new ExploreBannerRevisionPo();
        po.setId(domain.getId());
        po.setRevisionId(domain.getRevisionId());
        po.setBannerId(domain.getBannerId());
        po.setRevisionNo(domain.getRevisionNo());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setTitle(domain.getTitle());
        putImage(po, domain.getImage());
        putTarget(po, domain.getTarget());
        po.setDefaultSortNo(domain.getDefaultSortNo());
        po.setContentHash(domain.getContentHash());
        po.setPublishedAt(domain.getPublishedAt());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExploreBannerRevision toDomain(ExploreBannerRevisionPo po) {
        return ExploreBannerRevision.builder()
                .id(po.getId())
                .revisionId(po.getRevisionId())
                .bannerId(po.getBannerId())
                .revisionNo(po.getRevisionNo() == null ? 0 : po.getRevisionNo())
                .status(po.getStatus() == null ? null : ExploreRevisionStatus.valueOf(po.getStatus()))
                .title(po.getTitle())
                .image(image(po.getImageUrl(), po.getImageWidth(), po.getImageHeight(),
                        po.getImageMime(), po.getImageSize()))
                .target(target(po.getTargetType(), po.getTargetRefId(), po.getTargetRevisionId(),
                        po.getTargetUrl(), po.getTargetRouteCode(), po.getTargetParams()))
                .defaultSortNo(po.getDefaultSortNo() == null ? 0 : po.getDefaultSortNo())
                .contentHash(po.getContentHash())
                .publishedAt(po.getPublishedAt())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExplorePagePo fromDomain(ExplorePage domain) {
        ExplorePagePo po = new ExplorePagePo();
        po.setId(domain.getId());
        po.setPageId(domain.getPageId());
        po.setChannelCode(domain.getChannelCode());
        po.setPageVersion(domain.getPageVersion());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setEffectiveAt(domain.getEffectiveAt());
        po.setExpireAt(domain.getExpireAt());
        po.setRequestId(domain.getRequestId());
        po.setRequestHash(domain.getRequestHash());
        po.setExpectedReleaseId(domain.getExpectedReleaseId());
        po.setLockVersion(domain.getLockVersion());
        po.setPublishedBy(domain.getPublishedBy());
        po.setPublishedAt(domain.getPublishedAt());
        po.setOfflineAt(domain.getOfflineAt());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setModifyBy(domain.getModifyBy());
        po.setModifyTime(domain.getModifyTime());
        return po;
    }

    public static ExplorePage toDomain(ExplorePagePo po) {
        return ExplorePage.builder()
                .id(po.getId())
                .pageId(po.getPageId())
                .channelCode(po.getChannelCode())
                .pageVersion(po.getPageVersion() == null ? 0 : po.getPageVersion())
                .status(po.getStatus() == null ? null : ExplorePageStatus.valueOf(po.getStatus()))
                .effectiveAt(po.getEffectiveAt())
                .expireAt(po.getExpireAt())
                .requestId(po.getRequestId())
                .requestHash(po.getRequestHash())
                .expectedReleaseId(po.getExpectedReleaseId())
                .lockVersion(po.getLockVersion() == null ? 0 : po.getLockVersion())
                .publishedBy(po.getPublishedBy())
                .publishedAt(po.getPublishedAt())
                .offlineAt(po.getOfflineAt())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .modifyBy(po.getModifyBy())
                .modifyTime(po.getModifyTime())
                .build();
    }

    public static ExplorePageComponentPo fromDomain(ExplorePageComponent domain) {
        ExplorePageComponentPo po = new ExplorePageComponentPo();
        po.setId(domain.getId());
        po.setComponentId(domain.getComponentId());
        po.setPageId(domain.getPageId());
        po.setComponentType(domain.getComponentType() == null ? null : domain.getComponentType().name());
        po.setSortNo(domain.getSortNo());
        po.setConfigJson(domain.getConfig() == null ? null : ExploreJson.write(domain.getConfig()));
        return po;
    }

    public static ExplorePageComponent toDomain(ExplorePageComponentPo po) {
        return ExplorePageComponent.builder()
                .id(po.getId())
                .componentId(po.getComponentId())
                .pageId(po.getPageId())
                .componentType(po.getComponentType() == null
                        ? null : ExploreComponentType.valueOf(po.getComponentType()))
                .sortNo(po.getSortNo() == null ? 0 : po.getSortNo())
                .config(ExploreJson.readMap(po.getConfigJson()))
                .build();
    }

    public static ExplorePageItemPo fromDomain(ExplorePageItem domain) {
        ExplorePageItemPo po = new ExplorePageItemPo();
        po.setId(domain.getId());
        po.setItemId(domain.getItemId());
        po.setComponentId(domain.getComponentId());
        po.setItemType(domain.getItemType() == null ? null : domain.getItemType().name());
        po.setRefId(domain.getRefId());
        po.setRefRevisionId(domain.getRefRevisionId());
        po.setSortNo(domain.getSortNo());
        po.setStartAt(domain.getStartAt());
        po.setEndAt(domain.getEndAt());
        putTarget(po, domain.getTarget());
        po.setDisplaySnapshot(domain.getDisplaySnapshot() == null ? null
                : ExploreJson.write(domain.getDisplaySnapshot()));
        return po;
    }

    public static ExplorePageItem toDomain(ExplorePageItemPo po) {
        return ExplorePageItem.builder()
                .id(po.getId())
                .itemId(po.getItemId())
                .componentId(po.getComponentId())
                .itemType(po.getItemType() == null ? null : ExploreItemType.valueOf(po.getItemType()))
                .refId(po.getRefId())
                .refRevisionId(po.getRefRevisionId())
                .sortNo(po.getSortNo() == null ? 0 : po.getSortNo())
                .startAt(po.getStartAt())
                .endAt(po.getEndAt())
                .target(target(po.getTargetType(), po.getTargetRefId(), po.getTargetRevisionId(),
                        po.getTargetUrl(), po.getTargetRouteCode(), po.getTargetParams()))
                .displaySnapshot(ExploreJson.readMap(po.getDisplaySnapshot()))
                .build();
    }

    public static ExplorePageCurrentPo fromDomain(ExplorePageCurrent domain) {
        ExplorePageCurrentPo po = new ExplorePageCurrentPo();
        po.setChannelCode(domain.getChannelCode());
        po.setReleaseId(domain.getReleaseId());
        po.setLockVersion(domain.getLockVersion());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    public static ExplorePageCurrent toDomain(ExplorePageCurrentPo po) {
        return ExplorePageCurrent.builder()
                .channelCode(po.getChannelCode())
                .releaseId(po.getReleaseId())
                .lockVersion(po.getLockVersion() == null ? 0 : po.getLockVersion())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static ExploreOperationPo fromDomain(ExploreOperation domain) {
        ExploreOperationPo po = new ExploreOperationPo();
        po.setId(domain.getId());
        po.setOperationId(domain.getOperationId());
        po.setObjectType(domain.getObjectType());
        po.setObjectId(domain.getObjectId());
        po.setAction(domain.getAction() == null ? null : domain.getAction().name());
        po.setOperator(domain.getOperator());
        po.setOperatedAt(domain.getOperatedAt());
        po.setResult(domain.getResult());
        po.setRequestId(domain.getRequestId());
        po.setTraceId(domain.getTraceId());
        po.setDetail(domain.getDetail());
        return po;
    }

    public static ExploreImage image(String url, Integer width, Integer height, String mime, Long size) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        return ExploreImage.builder()
                .url(url)
                .width(width)
                .height(height)
                .mime(mime)
                .size(size)
                .build();
    }

    public static ExploreTarget target(String type, String refId, String revisionId,
                                       String url, String routeCode, String paramsJson) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        Map<String, String> params = ExploreJson.readStringMap(paramsJson);
        return ExploreTarget.builder()
                .type(ExploreTargetType.valueOf(type))
                .refId(refId)
                .revisionId(revisionId)
                .url(url)
                .routeCode(routeCode)
                .params(params.isEmpty() ? new LinkedHashMap<>() : params)
                .build();
    }

    private static void putImage(ExploreArticleRevisionPo po, ExploreImage image) {
        if (image == null) {
            return;
        }
        po.setCoverUrl(image.getUrl());
        po.setCoverWidth(image.getWidth());
        po.setCoverHeight(image.getHeight());
        po.setCoverMime(image.getMime());
        po.setCoverSize(image.getSize());
    }

    private static void putImage(ExploreTopicRevisionPo po, ExploreImage image) {
        if (image == null) {
            return;
        }
        po.setCoverUrl(image.getUrl());
        po.setCoverWidth(image.getWidth());
        po.setCoverHeight(image.getHeight());
        po.setCoverMime(image.getMime());
        po.setCoverSize(image.getSize());
    }

    private static void putImage(ExploreBannerRevisionPo po, ExploreImage image) {
        if (image == null) {
            return;
        }
        po.setImageUrl(image.getUrl());
        po.setImageWidth(image.getWidth());
        po.setImageHeight(image.getHeight());
        po.setImageMime(image.getMime());
        po.setImageSize(image.getSize());
    }

    private static void putTarget(ExploreTopicRevisionPo po, ExploreTarget target) {
        if (target == null) {
            return;
        }
        po.setTargetType(target.getType() == null ? null : target.getType().name());
        po.setTargetRefId(target.getRefId());
        po.setTargetRevisionId(target.getRevisionId());
        po.setTargetUrl(target.getUrl());
        po.setTargetRouteCode(target.getRouteCode());
        po.setTargetParams(target.getParams() == null || target.getParams().isEmpty()
                ? null : ExploreJson.write(target.getParams()));
    }

    private static void putTarget(ExploreBannerRevisionPo po, ExploreTarget target) {
        if (target == null) {
            return;
        }
        po.setTargetType(target.getType() == null ? null : target.getType().name());
        po.setTargetRefId(target.getRefId());
        po.setTargetRevisionId(target.getRevisionId());
        po.setTargetUrl(target.getUrl());
        po.setTargetRouteCode(target.getRouteCode());
        po.setTargetParams(target.getParams() == null || target.getParams().isEmpty()
                ? null : ExploreJson.write(target.getParams()));
    }

    private static void putTarget(ExplorePageItemPo po, ExploreTarget target) {
        if (target == null) {
            return;
        }
        po.setTargetType(target.getType() == null ? null : target.getType().name());
        po.setTargetRefId(target.getRefId());
        po.setTargetRevisionId(target.getRevisionId());
        po.setTargetUrl(target.getUrl());
        po.setTargetRouteCode(target.getRouteCode());
        po.setTargetParams(target.getParams() == null || target.getParams().isEmpty()
                ? null : ExploreJson.write(target.getParams()));
    }
}
