package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreChannelView;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreJson;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExplorePublishHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 探索页频道独立生命周期测试（MKT-CMS-DSN-CR-005 §4 / AT-026 / AT-027）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("频道独立生命周期 - 草稿/删除保护/独立发布")
class ExploreChannelAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-27T10:00:00Z");

    private static final String OPERATOR = "tester";

    private static final String CHANNEL = "NEWS";

    @Mock
    private ExploreChannelRepository channelRepository;

    @Mock
    private ExploreChannelPublishTxService publishTxService;

    @Mock
    private ExploreOperationRepository operationRepository;

    private ExploreChannelAppService channelAppService;

    @BeforeEach
    void setUp() {
        channelAppService = new ExploreChannelAppService(channelRepository, publishTxService,
                operationRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(channelRepository.findRevisionsByIds(any())).thenReturn(Map.of());
        when(channelRepository.findReferencedChannelCodes(any())).thenReturn(Set.of());
        when(channelRepository.existsPageReference(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("新增频道：合法编码生成根实体与 revision_no=1 草稿，手机不可见")
    void 新增频道生成初始草稿() {
        when(channelRepository.findByCode(CHANNEL)).thenReturn(Optional.empty(), Optional.of(root(null, null, 0)));

        ExploreChannelView view = channelAppService.create(cmd(CHANNEL, 0));

        ArgumentCaptor<ExploreChannel> rootCaptor = ArgumentCaptor.forClass(ExploreChannel.class);
        verify(channelRepository).insert(rootCaptor.capture());
        ArgumentCaptor<ExploreChannelRevision> revisionCaptor =
                ArgumentCaptor.forClass(ExploreChannelRevision.class);
        verify(channelRepository).insertRevision(revisionCaptor.capture());
        ExploreChannelRevision draft = revisionCaptor.getValue();

        assertEquals(CHANNEL, rootCaptor.getValue().getChannelCode());
        assertEquals(draft.getRevisionId(), rootCaptor.getValue().getDraftRevisionId());
        assertNull(rootCaptor.getValue().getCurrentRevisionId());
        assertEquals(1, draft.getRevisionNo());
        assertEquals(ExploreRevisionStatus.DRAFT, draft.getRevisionStatus());
        assertEquals(ExploreChannelStatus.ENABLED, draft.getStatus());
        assertNotNull(draft.getContentHash());
        assertEquals(64, draft.getContentHash().length());
        assertNull(view.getPublished());
    }

    @Test
    @DisplayName("编码矩阵：小写/数字开头/非法字符/超长均返回 105009")
    void 编码矩阵拒绝非法值() {
        for (String code : List.of("news", "1NEWS", "NEWS-1", "NEWS 1", "", "A234567890123456789012345678901234")) {
            ExploreException exception = assertThrows(ExploreException.class,
                    () -> channelAppService.create(cmd(code, 0)));
            assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, exception.getErrorCode());
            assertEquals("105009", exception.getErrorCode().getCode());
        }
        verify(channelRepository, never()).insert(any());
        verify(channelRepository, never()).insertRevision(any());
    }

    @Test
    @DisplayName("重复编码返回 105009")
    void 重复编码被拒绝() {
        when(channelRepository.findByCode(CHANNEL)).thenReturn(Optional.of(root(null, null, 0)));

        ExploreException exception = assertThrows(ExploreException.class,
                () -> channelAppService.create(cmd(CHANNEL, 0)));

        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, exception.getErrorCode());
        verify(channelRepository, never()).insert(any());
    }

    @Test
    @DisplayName("已有草稿：原地更新同一修订并递增根乐观锁")
    void 修改更新已有草稿() {
        ExploreChannel root = root("ecr_1", "ecr_0", 3);
        ExploreChannelRevision draft = revision("ecr_1", 1, ExploreRevisionStatus.DRAFT);
        when(channelRepository.findByCodeForUpdate(CHANNEL)).thenReturn(Optional.of(root));
        when(channelRepository.findRevision("ecr_1")).thenReturn(Optional.of(draft));
        when(channelRepository.updateDraftRevision(any())).thenReturn(1);
        when(channelRepository.update(any(), eq(3))).thenReturn(1);
        when(channelRepository.findByCode(CHANNEL)).thenReturn(Optional.of(root));

        channelAppService.update(cmd(CHANNEL, 3));

        ArgumentCaptor<ExploreChannelRevision> revisionCaptor =
                ArgumentCaptor.forClass(ExploreChannelRevision.class);
        verify(channelRepository).updateDraftRevision(revisionCaptor.capture());
        assertEquals("ecr_1", revisionCaptor.getValue().getRevisionId());
        assertEquals(1, revisionCaptor.getValue().getRevisionNo());
        verify(channelRepository, never()).insertRevision(any());
        ArgumentCaptor<ExploreChannel> rootCaptor = ArgumentCaptor.forClass(ExploreChannel.class);
        verify(channelRepository).update(rootCaptor.capture(), eq(3));
        assertEquals("ecr_1", rootCaptor.getValue().getDraftRevisionId());
        assertEquals(4, rootCaptor.getValue().getLockVersion());
    }

    @Test
    @DisplayName("无草稿：从当前发布修订复制生成下一 revision_no 草稿")
    void 无草稿时复制生成下一修订() {
        ExploreChannel root = root(null, "ecr_0", 0);
        when(channelRepository.findByCodeForUpdate(CHANNEL)).thenReturn(Optional.of(root));
        when(channelRepository.findLatestRevision(CHANNEL))
                .thenReturn(Optional.of(revision("ecr_0", 2, ExploreRevisionStatus.PUBLISHED)));
        when(channelRepository.update(any(), eq(0))).thenReturn(1);
        when(channelRepository.findByCode(CHANNEL)).thenReturn(Optional.of(root));

        channelAppService.update(cmd(CHANNEL, 0));

        ArgumentCaptor<ExploreChannelRevision> revisionCaptor =
                ArgumentCaptor.forClass(ExploreChannelRevision.class);
        verify(channelRepository).insertRevision(revisionCaptor.capture());
        assertEquals(3, revisionCaptor.getValue().getRevisionNo());
        assertEquals(ExploreRevisionStatus.DRAFT, revisionCaptor.getValue().getRevisionStatus());
        ArgumentCaptor<ExploreChannel> rootCaptor = ArgumentCaptor.forClass(ExploreChannel.class);
        verify(channelRepository).update(rootCaptor.capture(), eq(0));
        assertEquals(revisionCaptor.getValue().getRevisionId(), rootCaptor.getValue().getDraftRevisionId());
    }

    @Test
    @DisplayName("修改携带过期 lockVersion 返回 105012")
    void 修改乐观锁冲突() {
        when(channelRepository.findByCodeForUpdate(CHANNEL)).thenReturn(Optional.of(root(null, null, 2)));

        ExploreException exception = assertThrows(ExploreException.class,
                () -> channelAppService.update(cmd(CHANNEL, 1)));

        assertEquals(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, exception.getErrorCode());
        verify(channelRepository, never()).update(any(), anyInt());
    }

    @Test
    @DisplayName("被页面草稿/当前/历史发布版本引用时禁止删除并提示只能停用")
    void 引用中的频道禁止删除() {
        when(channelRepository.findByCodeForUpdate(CHANNEL)).thenReturn(Optional.of(root(null, null, 4)));
        when(channelRepository.existsPageReference(CHANNEL)).thenReturn(true);

        ExploreException exception = assertThrows(ExploreException.class,
                () -> channelAppService.delete(CHANNEL, 4, OPERATOR));

        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, exception.getErrorCode());
        assertEquals("105009", exception.getErrorCode().getCode());
        verify(channelRepository, never()).deleteRevisions(anyString());
        verify(channelRepository, never()).deleteByCode(anyString(), anyInt());
    }

    @Test
    @DisplayName("未引用频道删除根与全部修订")
    void 未引用频道可删除() {
        when(channelRepository.findByCodeForUpdate(CHANNEL)).thenReturn(Optional.of(root(null, null, 4)));
        when(channelRepository.deleteByCode(CHANNEL, 4)).thenReturn(1);

        channelAppService.delete(CHANNEL, 4, OPERATOR);

        verify(channelRepository).deleteRevisions(CHANNEL);
        verify(channelRepository).deleteByCode(CHANNEL, 4);
    }

    @Test
    @DisplayName("列表返回草稿/发布/未发布变更/可删除性")
    void 列表组合草稿与发布视图() {
        ExploreChannel root = root("ecr_1", "ecr_0", 2);
        when(channelRepository.findAll()).thenReturn(List.of(root));
        when(channelRepository.findRevisionsByIds(any())).thenReturn(Map.of(
                "ecr_1", revision("ecr_1", 2, ExploreRevisionStatus.DRAFT),
                "ecr_0", revision("ecr_0", 1, ExploreRevisionStatus.PUBLISHED)));
        when(channelRepository.findReferencedChannelCodes(any())).thenReturn(Set.of(CHANNEL));

        List<ExploreChannelView> views = channelAppService.list();

        assertEquals(1, views.size());
        assertTrue(views.get(0).isHasUnpublishedChanges());
        assertFalse(views.get(0).isDeletable());
        assertEquals("ecr_0", views.get(0).getPublished().getRevisionId());
        assertEquals(2, views.get(0).getLockVersion());
    }

    @Test
    @DisplayName("发布：无同 requestId 且事务成功时返回最新视图")
    void 发布调用事务服务() {
        ExploreChannel root = root("ecr_1", null, 0);
        when(channelRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
        when(channelRepository.findByCode(CHANNEL)).thenReturn(Optional.of(root));
        when(channelRepository.findRevisionsByIds(any())).thenReturn(Map.of());

        ExploreChannelView view = channelAppService.publish(publishCmd("req-1", null, 0));

        verify(publishTxService).publish(any(), anyString());
        assertEquals(0, view.getLockVersion());
    }

    @Test
    @DisplayName("发布重试：同 requestId 同哈希直接返回，不再重复发布")
    void 同哈希幂等重试() {
        String hash = requestHash(CHANNEL, "ecr_1", null);
        ExploreChannelRevision existing = publishedRevision("req-1", hash, null);
        when(channelRepository.findByRequestId("req-1")).thenReturn(Optional.of(existing));
        when(channelRepository.findByCode(CHANNEL)).thenReturn(Optional.of(root(null, "ecr_1", 1)));

        channelAppService.publish(publishCmd("req-1", null, 0));

        verify(publishTxService, never()).publish(any(), anyString());
    }

    @Test
    @DisplayName("发布重试：同 requestId 不同命令返回 105014")
    void 不同命令复用requestId被拒绝() {
        String hash = requestHash(CHANNEL, "ecr_1", "ecr_0");
        ExploreChannelRevision existing = publishedRevision("req-1", hash, "ecr_0");
        when(channelRepository.findByRequestId("req-1")).thenReturn(Optional.of(existing));

        ExploreException exception = assertThrows(ExploreException.class,
                () -> channelAppService.publish(publishCmd("req-1", "ecr_other", 1)));

        assertEquals(ExploreErrorCode.EXPLORE_IDEMPOTENCY_CONFLICT, exception.getErrorCode());
        assertEquals("105014", exception.getErrorCode().getCode());
        verify(publishTxService, never()).publish(any(), anyString());
    }

    private ExploreChannelCmd cmd(String channelCode, int lockVersion) {
        return ExploreChannelCmd.builder()
                .channelCode(channelCode)
                .name("资讯")
                .sortNo(40)
                .status("ENABLED")
                .lockVersion(lockVersion)
                .operator(OPERATOR)
                .build();
    }

    private ExploreChannelPublishCmd publishCmd(String requestId, String expected, int lockVersion) {
        return ExploreChannelPublishCmd.builder()
                .channelCode(CHANNEL)
                .requestId(requestId)
                .expectedCurrentRevisionId(expected)
                .lockVersion(lockVersion)
                .operator(OPERATOR)
                .build();
    }

    private ExploreChannel root(String draftRevisionId, String currentRevisionId, int lockVersion) {
        return ExploreChannel.builder()
                .channelCode(CHANNEL)
                .name("资讯")
                .sortNo(40)
                .status(ExploreChannelStatus.ENABLED)
                .draftRevisionId(draftRevisionId)
                .currentRevisionId(currentRevisionId)
                .lockVersion(lockVersion)
                .build();
    }

    private ExploreChannelRevision revision(String revisionId, int revisionNo,
                                            ExploreRevisionStatus status) {
        return ExploreChannelRevision.builder()
                .revisionId(revisionId)
                .channelCode(CHANNEL)
                .revisionNo(revisionNo)
                .name("资讯")
                .sortNo(40)
                .status(ExploreChannelStatus.ENABLED)
                .revisionStatus(status)
                .contentHash("a".repeat(64))
                .build();
    }

    private ExploreChannelRevision publishedRevision(String requestId, String requestHash, String expected) {
        return revision("ecr_1", 1, ExploreRevisionStatus.PUBLISHED).toBuilder()
                .requestId(requestId)
                .requestHash(requestHash)
                .expectedCurrentRevisionId(expected)
                .build();
    }

    private String requestHash(String channelCode, String draftRevisionId, String expected) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channelCode", channelCode);
        payload.put("draftRevisionId", draftRevisionId);
        payload.put("expectedCurrentRevisionId", expected);
        return ExplorePublishHash.of(ExploreJson.write(payload));
    }
}
