package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreBannerCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreContentPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreBannerView;
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
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBanner;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBannerRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreContentSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreBannerRepository;
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
 * 探索页 Banner 应用服务。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreBannerAppService {

    private final ExploreBannerRepository bannerRepository;
    private final ExploreOperationRepository operationRepository;
    private final ExploreMediaValidator mediaValidator;
    private final ExploreTargetAppService targetAppService;
    private final Clock clock;

    public List<ExploreBannerView> search(ExploreContentSearchCriteria criteria) {
        return ExplorePageList.map(bannerRepository.search(criteria),
                banner -> new ExploreBannerView(banner, latestRevision(banner)));
    }

    public ExploreBannerView get(String bannerId) {
        ExploreBanner banner = bannerRepository.findByBannerId(bannerId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "Banner 不存在: " + bannerId));
        return new ExploreBannerView(banner, latestRevision(banner));
    }

    /**
     * 新建 Banner（根实体 + 草稿修订）
     *
     * @param cmd 新建命令
     * @return Banner 管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreBannerView create(ExploreBannerCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        Instant now = Instant.now(clock);
        String bannerId = ExploreIdGenerator.bannerId();
        ExploreBanner banner = ExploreBanner.create(bannerId, operator, now);
        ExploreBannerRevision revision = buildRevision(bannerId, 1, cmd, operator, now);
        bannerRepository.insert(banner);
        bannerRepository.insertRevision(revision);
        audit(ExploreAction.CREATE, bannerId, operator, now, null);
        log.info("探索页 Banner 创建成功: bannerId={}, operator={}", bannerId, operator);
        return new ExploreBannerView(banner, revision);
    }

    /**
     * 修改 Banner：草稿修订原地更新，否则创建新草稿修订
     *
     * @param cmd 修改命令
     * @return Banner 管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreBannerView update(ExploreBannerCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        int expectedLockVersion = ExploreGuard.requireLockVersion(cmd.getLockVersion());
        String bannerId = ExploreGuard.requireText(cmd.getBannerId(), "Banner 标识", 64);
        ExploreBanner banner = bannerRepository.findByBannerIdForUpdate(bannerId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "Banner 不存在: " + bannerId));
        if (banner.getLockVersion() != expectedLockVersion) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "Banner 已被修改，请基于最新乐观锁版本重试");
        }
        Instant now = Instant.now(clock);
        ExploreBannerRevision draft = bannerRepository.findLatestDraft(bannerId).orElse(null);
        ExploreBannerRevision revision;
        if (draft != null) {
            revision = buildRevision(bannerId, draft.getRevisionNo(), cmd, operator, now)
                    .toBuilder()
                    .revisionId(draft.getRevisionId())
                    .createBy(draft.getCreateBy())
                    .createTime(draft.getCreateTime())
                    .build();
            if (bannerRepository.updateDraftRevision(revision) == 0) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                        "Banner 草稿修订已被并发修改，请重试");
            }
        } else {
            int nextRevisionNo = bannerRepository.findLatestRevision(bannerId)
                    .map(latest -> latest.getRevisionNo() + 1)
                    .orElse(1);
            revision = buildRevision(bannerId, nextRevisionNo, cmd, operator, now);
            bannerRepository.insertRevision(revision);
        }
        ExploreBanner updated = banner.touched(operator, now);
        if (bannerRepository.update(updated, expectedLockVersion) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, "Banner 已被并发修改，请重试");
        }
        audit(ExploreAction.UPDATE, bannerId, operator, now, "revisionNo=" + revision.getRevisionNo());
        log.info("探索页 Banner 修改成功: bannerId={}, revisionNo={}", bannerId, revision.getRevisionNo());
        return new ExploreBannerView(updated, revision);
    }

    /**
     * 发布指定草稿修订
     *
     * @param cmd 发布命令
     * @return Banner 管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreBannerView publish(ExploreContentPublishCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String bannerId = ExploreGuard.requireText(cmd.getObjectId(), "Banner 标识", 64);
        ExploreBanner banner = bannerRepository.findByBannerIdForUpdate(bannerId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "Banner 不存在: " + bannerId));
        ExploreBannerRevision revision = StringUtils.hasText(cmd.getRevisionId())
                ? bannerRepository.findRevision(cmd.getRevisionId().trim()).orElse(null)
                : bannerRepository.findLatestDraft(bannerId).orElse(null);
        if (revision == null || !bannerId.equals(revision.getBannerId())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND, "待发布 Banner 修订不存在");
        }
        if (revision.getStatus() == ExploreRevisionStatus.PUBLISHED) {
            if (revision.getRevisionId().equals(banner.getCurrentRevisionId())) {
                return get(bannerId);
            }
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "已发布修订不可重复发布，请创建新草稿修订");
        }
        if (revision.getTarget() == null || revision.getTarget().getType() == null) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "Banner 跳转目标不能为空");
        }
        targetAppService.validate(revision.getTarget());
        Instant now = Instant.now(clock);
        ExploreBannerRevision published = revision.published(operator, now);
        if (bannerRepository.publishRevision(published) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, "修订状态已变化，发布失败");
        }
        ExploreBanner updated = banner.published(revision.getRevisionId(), operator, now);
        if (bannerRepository.update(updated, banner.getLockVersion()) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "Banner 已被并发修改，发布失败");
        }
        audit(ExploreAction.PUBLISH, bannerId, operator, now, "revisionId=" + revision.getRevisionId());
        log.info("探索页 Banner 发布成功: bannerId={}, revisionId={}", bannerId, revision.getRevisionId());
        return new ExploreBannerView(updated, published);
    }

    /**
     * 立即下线 Banner
     *
     * @param cmd 下线命令
     * @return Banner 管理视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreBannerView offline(ExploreContentPublishCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String bannerId = ExploreGuard.requireText(cmd.getObjectId(), "Banner 标识", 64);
        ExploreBanner banner = bannerRepository.findByBannerIdForUpdate(bannerId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "Banner 不存在: " + bannerId));
        if (banner.getStatus() == ExploreContentStatus.OFFLINE) {
            return get(bannerId);
        }
        Instant now = Instant.now(clock);
        ExploreBanner updated = banner.offline(operator, now);
        if (bannerRepository.update(updated, banner.getLockVersion()) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, "Banner 已被并发修改，下线失败");
        }
        audit(ExploreAction.OFFLINE, bannerId, operator, now, null);
        log.info("探索页 Banner 下线成功: bannerId={}", bannerId);
        return get(bannerId);
    }

    private ExploreBannerRevision latestRevision(ExploreBanner banner) {
        return bannerRepository.findLatestDraft(banner.getBannerId())
                .or(() -> banner.getCurrentRevisionId() == null
                        ? bannerRepository.findLatestRevision(banner.getBannerId())
                        : bannerRepository.findRevision(banner.getCurrentRevisionId()))
                .orElse(null);
    }

    private ExploreBannerRevision buildRevision(String bannerId, int revisionNo, ExploreBannerCmd cmd,
                                                String operator, Instant now) {
        String title = ExploreGuard.requireText(cmd.getTitle(), "Banner 标题", 255);
        mediaValidator.validateRequiredImage(cmd.getImage(), "Banner 主图");
        ExploreTarget target = cmd.getTarget();
        if (target == null || target.getType() == null) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "Banner 跳转目标不能为空");
        }
        targetAppService.validate(target);
        int defaultSortNo = cmd.getDefaultSortNo() == null ? 0 : cmd.getDefaultSortNo();
        if (defaultSortNo < 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "Banner 默认排序不得为负");
        }
        return ExploreBannerRevision.builder()
                .revisionId(ExploreIdGenerator.bannerRevisionId())
                .bannerId(bannerId)
                .revisionNo(revisionNo)
                .status(ExploreRevisionStatus.DRAFT)
                .title(title)
                .image(cmd.getImage())
                .target(target)
                .defaultSortNo(defaultSortNo)
                .contentHash(contentHash(title, cmd, target, defaultSortNo))
                .createBy(operator)
                .createTime(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    private String contentHash(String title, ExploreBannerCmd cmd, ExploreTarget target, int defaultSortNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("imageUrl", cmd.getImage().getUrl());
        payload.put("imageWidth", cmd.getImage().getWidth());
        payload.put("imageHeight", cmd.getImage().getHeight());
        payload.put("imageMime", cmd.getImage().getMime());
        payload.put("imageSize", cmd.getImage().getSize());
        payload.put("target", target.toView());
        payload.put("defaultSortNo", defaultSortNo);
        return HexSha256.ofUtf8(ExploreJson.write(payload));
    }

    private void audit(ExploreAction action, String bannerId, String operator, Instant now, String detail) {
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                action, "EXPLORE_BANNER", bannerId, operator, now, null,
                TraceContext.currentTraceId(), detail));
    }
}
