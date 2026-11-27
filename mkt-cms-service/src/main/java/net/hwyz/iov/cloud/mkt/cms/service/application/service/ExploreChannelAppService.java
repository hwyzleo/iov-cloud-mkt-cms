package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreChannelView;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreGuard;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreJson;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.HexSha256;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExplorePublishHash;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 探索页频道应用服务（MKT-CMS-DSN-CR-005 §4 / D-019）。
 * <p>
 * 频道采用“稳定根实体 + 可编辑草稿修订 + 不可变已发布修订 + 当前指针”；
 * 新增、草稿修改、引用保护删除与独立发布互不与页面发布混为一次操作，
 * 频道发布不调用页面发布服务，也不改变任何页面版本或当前页面指针。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreChannelAppService {

    /**
     * 频道编码规则（AC-068）：大写字母开头，仅大写字母、数字与下划线，最长 32 位
     */
    private static final Pattern CHANNEL_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,31}$");

    private static final int MAX_REQUEST_ID_LENGTH = 64;

    private static final int MAX_ATTEMPTS = 3;

    private final ExploreChannelRepository channelRepository;
    private final ExploreChannelPublishTxService publishTxService;
    private final ExploreOperationRepository operationRepository;
    private final Clock clock;

    /**
     * 全部频道（管理端，含草稿/当前发布/未发布变更/可删除性）
     *
     * @return 频道视图列表
     */
    public List<ExploreChannelView> list() {
        List<ExploreChannel> roots = channelRepository.findAll();
        Set<String> revisionIds = roots.stream()
                .flatMap(root -> java.util.stream.Stream.of(root.getDraftRevisionId(), root.getCurrentRevisionId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ExploreChannelRevision> revisions = channelRepository.findRevisionsByIds(revisionIds);
        Set<String> referenced = channelRepository.findReferencedChannelCodes(
                roots.stream().map(ExploreChannel::getChannelCode).collect(Collectors.toSet()));
        return roots.stream()
                .map(root -> buildView(root, revisions, referenced))
                .collect(Collectors.toList());
    }

    /**
     * 新增频道：创建稳定编码根实体与 revision_no=1 的初始草稿
     *
     * @param cmd 新增命令
     * @return 频道视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreChannelView create(ExploreChannelCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String channelCode = requireChannelCode(cmd.getChannelCode());
        String name = ExploreGuard.requireText(cmd.getName(), "频道名称", 64);
        int sortNo = requireSortNo(cmd.getSortNo());
        ExploreChannelStatus status = ExploreChannelStatus.parse(cmd.getStatus());
        if (channelRepository.findByCode(channelCode).isPresent()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "频道编码已存在: " + channelCode);
        }
        Instant now = Instant.now(clock);
        ExploreChannelRevision draft = ExploreChannelRevision.builder()
                .revisionId(ExploreIdGenerator.channelRevisionId())
                .channelCode(channelCode)
                .revisionNo(1)
                .name(name)
                .sortNo(sortNo)
                .status(status)
                .revisionStatus(ExploreRevisionStatus.DRAFT)
                .contentHash(contentHash(channelCode, name, sortNo, status))
                .createBy(operator)
                .createTime(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
        ExploreChannel root = ExploreChannel.builder()
                .channelCode(channelCode)
                .name(name)
                .sortNo(sortNo)
                .status(status)
                .draftRevisionId(draft.getRevisionId())
                .currentRevisionId(null)
                .lockVersion(0)
                .createBy(operator)
                .createTime(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
        channelRepository.insert(root);
        channelRepository.insertRevision(draft);
        audit(ExploreAction.CREATE_CHANNEL, channelCode, operator, now, null,
                "revisionId=" + draft.getRevisionId());
        log.info("探索页频道创建成功: channelCode={}, operator={}", channelCode, operator);
        return view(channelCode);
    }

    /**
     * 保存频道草稿：已有草稿原地更新，否则从当前发布修订复制生成下一修订
     *
     * @param cmd 修改命令
     * @return 频道视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreChannelView update(ExploreChannelCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String channelCode = ExploreGuard.requireText(cmd.getChannelCode(), "频道编码", 32);
        int expectedLockVersion = ExploreGuard.requireLockVersion(cmd.getLockVersion());
        String name = ExploreGuard.requireText(cmd.getName(), "频道名称", 64);
        int sortNo = requireSortNo(cmd.getSortNo());
        ExploreChannelStatus status = ExploreChannelStatus.parse(cmd.getStatus());

        ExploreChannel root = channelRepository.findByCodeForUpdate(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "频道不存在: " + channelCode));
        if (root.getLockVersion() != expectedLockVersion) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "频道已被修改，请基于最新乐观锁版本重试");
        }
        Instant now = Instant.now(clock);
        String hash = contentHash(channelCode, name, sortNo, status);
        ExploreChannelRevision draft = root.getDraftRevisionId() == null
                ? null : channelRepository.findRevision(root.getDraftRevisionId())
                .filter(revision -> revision.getRevisionStatus() == ExploreRevisionStatus.DRAFT)
                .orElse(null);
        ExploreChannelRevision saved;
        if (draft != null) {
            saved = draft.revised(name, sortNo, status, hash, operator, now);
            if (channelRepository.updateDraftRevision(saved) == 0) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                        "频道草稿已被并发修改，请重试");
            }
        } else {
            int nextRevisionNo = channelRepository.findLatestRevision(channelCode)
                    .map(latest -> latest.getRevisionNo() + 1)
                    .orElse(1);
            saved = ExploreChannelRevision.builder()
                    .revisionId(ExploreIdGenerator.channelRevisionId())
                    .channelCode(channelCode)
                    .revisionNo(nextRevisionNo)
                    .name(name)
                    .sortNo(sortNo)
                    .status(status)
                    .revisionStatus(ExploreRevisionStatus.DRAFT)
                    .contentHash(hash)
                    .createBy(operator)
                    .createTime(now)
                    .modifyBy(operator)
                    .modifyTime(now)
                    .build();
            channelRepository.insertRevision(saved);
        }
        ExploreChannel updated = root.drafted(saved.getRevisionId(), operator, now);
        if (channelRepository.update(updated, expectedLockVersion) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "频道已被并发修改，请重试");
        }
        audit(ExploreAction.UPDATE_CHANNEL, channelCode, operator, now, null,
                "revisionNo=" + saved.getRevisionNo());
        log.info("探索页频道草稿保存成功: channelCode={}, revisionNo={}, operator={}",
                channelCode, saved.getRevisionNo(), operator);
        return view(channelCode);
    }

    /**
     * 删除未被任何页面引用的频道（根实体与全部修订同事务物理删除，操作留痕保留）
     *
     * @param channelCode         频道编码
     * @param expectedLockVersion 期望乐观锁版本
     * @param operator            操作人
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String channelCode, Integer expectedLockVersion, String operator) {
        String currentOperator = ExploreGuard.requireOperator(operator);
        String code = ExploreGuard.requireText(channelCode, "频道编码", 32);
        int lockVersion = ExploreGuard.requireLockVersion(expectedLockVersion);
        ExploreChannel root = channelRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "频道不存在: " + code));
        if (root.getLockVersion() != lockVersion) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "频道已被修改，请基于最新乐观锁版本重试");
        }
        if (channelRepository.existsPageReference(code)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "频道已被页面引用，只能停用");
        }
        Instant now = Instant.now(clock);
        channelRepository.deleteRevisions(code);
        if (channelRepository.deleteByCode(code, lockVersion) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "频道已被并发修改，删除失败");
        }
        audit(ExploreAction.DELETE_CHANNEL, code, currentOperator, now, null, null);
        log.info("探索页频道删除成功: channelCode={}, operator={}", code, currentOperator);
    }

    /**
     * 独立发布当前草稿（幂等、乐观锁与发布前提校验；不触达任何页面版本）
     *
     * @param cmd 发布命令
     * @return 频道视图
     */
    public ExploreChannelView publish(ExploreChannelPublishCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String channelCode = ExploreGuard.requireText(cmd.getChannelCode(), "频道编码", 32);
        String requestId = ExploreGuard.requireText(cmd.getRequestId(), "requestId",
                MAX_REQUEST_ID_LENGTH);
        int lockVersion = ExploreGuard.requireLockVersion(cmd.getLockVersion());
        String expected = normalizeExpected(cmd.getExpectedCurrentRevisionId());

        ExploreChannelRevision existing = channelRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return idempotentView(existing, channelCode, expected);
        }

        ExploreChannel root = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "频道不存在: " + channelCode));
        String draftRevisionId = root.getDraftRevisionId();
        String requestHash = requestHash(channelCode, draftRevisionId, expected);
        ExploreChannelPublishCmd normalized = ExploreChannelPublishCmd.builder()
                .channelCode(channelCode)
                .requestId(requestId)
                .expectedCurrentRevisionId(expected)
                .lockVersion(lockVersion)
                .operator(operator)
                .build();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                publishTxService.publish(normalized, requestHash);
                return view(channelCode);
            } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
                ExploreChannelRevision winner = channelRepository.findByRequestId(requestId).orElse(null);
                if (winner != null) {
                    return idempotentView(winner, channelCode, expected);
                }
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("探索页频道发布并发竞争重试耗尽: channelCode={}, requestId={}",
                            channelCode, requestId);
                    throw new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE,
                            "频道发布并发竞争冲突，请重试", e);
                }
                log.info("探索页频道发布并发竞争，重试: channelCode={}, attempt={}", channelCode, attempt);
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE, "频道发布失败，请重试");
    }

    /**
     * 单频道视图
     *
     * @param channelCode 频道编码
     * @return 频道视图
     */
    public ExploreChannelView view(String channelCode) {
        ExploreChannel root = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "频道不存在: " + channelCode));
        Set<String> revisionIds = new LinkedHashSet<>();
        if (root.getDraftRevisionId() != null) {
            revisionIds.add(root.getDraftRevisionId());
        }
        if (root.getCurrentRevisionId() != null) {
            revisionIds.add(root.getCurrentRevisionId());
        }
        Map<String, ExploreChannelRevision> revisions = channelRepository.findRevisionsByIds(revisionIds);
        Set<String> referenced = channelRepository.existsPageReference(channelCode)
                ? Set.of(channelCode) : Set.of();
        return buildView(root, revisions, referenced);
    }

    private ExploreChannelView buildView(ExploreChannel root,
                                         Map<String, ExploreChannelRevision> revisions,
                                         Set<String> referenced) {
        ExploreChannelRevision draft = root.getDraftRevisionId() == null
                ? null : revisions.get(root.getDraftRevisionId());
        ExploreChannelRevision published = root.getCurrentRevisionId() == null
                ? null : revisions.get(root.getCurrentRevisionId());
        return ExploreChannelView.builder()
                .channelCode(root.getChannelCode())
                .draft(draft)
                .published(published)
                .hasUnpublishedChanges(draft != null)
                .deletable(!referenced.contains(root.getChannelCode()))
                .lockVersion(root.getLockVersion())
                .build();
    }

    /**
     * 幂等重试：同一 requestId 且命令一致时返回当前视图，否则 105014
     */
    private ExploreChannelView idempotentView(ExploreChannelRevision existing, String channelCode,
                                              String expected) {
        if (!channelCode.equals(existing.getChannelCode())
                || !Objects.equals(expected, existing.getExpectedCurrentRevisionId())
                || !requestHash(channelCode, existing.getRevisionId(), expected)
                .equals(existing.getRequestHash())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_IDEMPOTENCY_CONFLICT,
                    "同一 requestId 已用于不同发布命令");
        }
        return view(existing.getChannelCode());
    }

    private String requireChannelCode(String value) {
        if (!StringUtils.hasText(value) || !CHANNEL_CODE_PATTERN.matcher(value.trim()).matches()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "频道编码仅允许大写字母、数字和下划线，且必须以字母开头，长度不超过 32");
        }
        return value.trim();
    }

    private int requireSortNo(Integer sortNo) {
        if (sortNo == null || sortNo < 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "频道排序不能为空且不得为负");
        }
        return sortNo;
    }

    private String contentHash(String channelCode, String name, int sortNo, ExploreChannelStatus status) {
        return HexSha256.ofUtf8(channelCode + "|" + name + "|" + sortNo + "|" + status.name());
    }

    private String requestHash(String channelCode, String draftRevisionId, String expected) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channelCode", channelCode);
        payload.put("draftRevisionId", draftRevisionId);
        payload.put("expectedCurrentRevisionId", expected);
        return ExplorePublishHash.of(ExploreJson.write(payload));
    }

    private String normalizeExpected(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "expectedCurrentRevisionId 长度不得超过 64");
        }
        return trimmed;
    }

    private void audit(ExploreAction action, String channelCode, String operator, Instant now,
                       String requestId, String detail) {
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                action, "EXPLORE_CHANNEL", channelCode, operator, now, requestId,
                TraceContext.currentTraceId(), detail));
    }
}
