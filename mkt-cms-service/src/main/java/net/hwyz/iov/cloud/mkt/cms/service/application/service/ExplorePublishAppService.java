package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExplorePagePublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreComponentData;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExplorePageData;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExplorePublishResult;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePublishType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreGuard;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreJson;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExplorePublishHash;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探索页页面发布应用服务（发布编排、幂等哈希与并发竞争重试）。
 * <p>
 * MKT-CMS-DSN-CR-003 §5.2 / D-013：相同 requestId 复用原命令；不同 requestId 的并发发布
 * 基于旧 expectedReleaseId 失败；锁竞争由有限重试兜底，不静默覆盖。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExplorePublishAppService {

    private static final int MAX_ATTEMPTS = 3;

    private static final int REQUEST_ID_MAX_LENGTH = 64;

    private final ExplorePageRepository pageRepository;
    private final ExplorePageAppService pageAppService;
    private final ExplorePageContentService contentService;
    private final ExplorePublishTxService publishTxService;
    private final ExploreScheduleTxService scheduleTxService;

    /**
     * 立即或定时发布频道页面
     *
     * @param cmd 发布命令
     * @return 发布结果
     */
    public ExplorePublishResult publish(ExplorePagePublishCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String channelCode = ExploreGuard.requireText(cmd.getChannelCode(), "频道编码", 32);
        pageAppService.requireChannel(channelCode);
        validateRequestId(cmd.getRequestId());
        pageAppService.validatePublishTimes(cmd);
        ExplorePagePublishCmd normalized = normalize(cmd, operator, channelCode);

        ExplorePage draft = pageRepository.findDraft(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "频道尚无页面草稿，无法发布"));
        int expectedDraftLockVersion = draft.getLockVersion();
        String requestHash = requestHash(draft,
                contentService.load(draft.getPageId()), normalized);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ExplorePage release = publishTxService.publish(normalized, requestHash,
                        expectedDraftLockVersion);
                return new ExplorePublishResult(release);
            } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
                ExplorePage winner = pageRepository.findByRequestId(normalized.getRequestId()).orElse(null);
                if (winner != null) {
                    if (!requestHash.equals(winner.getRequestHash())) {
                        throw new ExploreException(ExploreErrorCode.EXPLORE_IDEMPOTENCY_CONFLICT,
                                "同一 requestId 已用于不同发布命令");
                    }
                    return new ExplorePublishResult(winner);
                }
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("探索页发布并发竞争重试耗尽: channelCode={}, requestId={}",
                            channelCode, normalized.getRequestId());
                    throw new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE,
                            "发布并发竞争冲突，请重试", e);
                }
                log.info("探索页发布并发竞争，重试: channelCode={}, attempt={}", channelCode, attempt);
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE, "发布失败，请重试");
    }

    /**
     * 取消频道待生效版本（不改变当前页面）
     *
     * @param channelCode 频道编码
     * @param operator    操作人
     * @return 被取消的定时版本
     */
    public ExplorePage cancelSchedule(String channelCode, String operator) {
        String currentOperator = ExploreGuard.requireOperator(operator);
        pageAppService.requireChannel(channelCode);
        return scheduleTxService.cancelSchedule(channelCode.trim(), currentOperator);
    }

    /**
     * 立即下线频道页面（同时取消待生效版本）
     *
     * @param channelCode 频道编码
     * @param operator    操作人
     * @return 下线结果
     */
    public ExplorePage offline(String channelCode, String operator) {
        String currentOperator = ExploreGuard.requireOperator(operator);
        pageAppService.requireChannel(channelCode);
        return scheduleTxService.offline(channelCode.trim(), currentOperator);
    }

    private ExplorePagePublishCmd normalize(ExplorePagePublishCmd cmd, String operator, String channelCode) {
        String publishType = ExplorePublishType.parse(cmd.getPublishType()).name();
        Instant effectiveAt = "SCHEDULED".equals(publishType) ? cmd.getEffectiveAt() : null;
        return ExplorePagePublishCmd.builder()
                .channelCode(channelCode)
                .requestId(cmd.getRequestId().trim())
                .expectedReleaseId(cmd.getExpectedReleaseId())
                .publishType(publishType)
                .effectiveAt(effectiveAt)
                .expireAt(cmd.getExpireAt())
                .operator(operator)
                .build();
    }

    private String requestHash(ExplorePage draft, List<ExploreComponentData> content,
                               ExplorePagePublishCmd cmd) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("channelCode", draft.getChannelCode());
        root.put("draftLockVersion", draft.getLockVersion());
        root.put("publishType", cmd.getPublishType());
        root.put("effectiveAt", iso(cmd.getEffectiveAt()));
        root.put("expireAt", iso(cmd.getExpireAt()));
        root.put("expectedReleaseId", cmd.getExpectedReleaseId());
        List<Map<String, Object>> components = new ArrayList<>();
        for (ExploreComponentData data : content) {
            Map<String, Object> component = new LinkedHashMap<>();
            component.put("type", data.getComponent().getComponentType().name());
            component.put("sortNo", data.getComponent().getSortNo());
            component.put("config", data.getComponent().getConfig());
            List<Map<String, Object>> items = new ArrayList<>();
            for (ExplorePageItem item : data.getItems()) {
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("itemId", item.getItemId());
                view.put("itemType", item.getItemType().name());
                view.put("refId", item.getRefId());
                view.put("refRevisionId", item.getRefRevisionId());
                view.put("sortNo", item.getSortNo());
                view.put("startAt", iso(item.getStartAt()));
                view.put("endAt", iso(item.getEndAt()));
                view.put("target", item.getTarget() == null ? null : item.getTarget().toView());
                view.put("displaySnapshot", item.getDisplaySnapshot());
                items.add(view);
            }
            component.put("items", items);
            components.add(component);
        }
        root.put("components", components);
        return ExplorePublishHash.of(ExploreJson.write(root));
    }

    private String iso(Instant value) {
        return value == null ? null : DateTimeFormatter.ISO_INSTANT.format(value);
    }

    private void validateRequestId(String requestId) {
        String value = ExploreGuard.requireText(requestId, "requestId", REQUEST_ID_MAX_LENGTH);
        if (value.length() > REQUEST_ID_MAX_LENGTH) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "requestId 长度不得超过 " + REQUEST_ID_MAX_LENGTH);
        }
    }
}
