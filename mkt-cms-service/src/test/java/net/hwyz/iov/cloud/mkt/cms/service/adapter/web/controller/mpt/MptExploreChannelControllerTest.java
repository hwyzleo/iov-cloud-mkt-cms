package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler.AgreementExceptionHandler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreChannelView;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreChannelAppService;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MPT 探索页频道管理契约测试（MKT-CMS-DSN-CR-005 §6.1 / AT-026）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MPT 探索页 - 频道生命周期契约")
class MptExploreChannelControllerTest {

    @Mock
    private ExploreChannelAppService channelAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MptExploreChannelController controller = new MptExploreChannelController(channelAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("列表返回草稿/发布/未发布变更/可删除性")
    void 列表契约() throws Exception {
        when(channelAppService.list()).thenReturn(List.of(ExploreChannelView.builder()
                .channelCode("NEWS")
                .draft(revision("ecr_2", ExploreRevisionStatus.DRAFT))
                .published(revision("ecr_1", ExploreRevisionStatus.PUBLISHED))
                .hasUnpublishedChanges(true)
                .deletable(false)
                .lockVersion(1)
                .build()));

        mockMvc.perform(get("/api/mpt/explore/v1/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].channelCode").value("NEWS"))
                .andExpect(jsonPath("$.data[0].draft.revisionId").value("ecr_2"))
                .andExpect(jsonPath("$.data[0].draft.name").value("资讯"))
                .andExpect(jsonPath("$.data[0].published.revisionId").value("ecr_1"))
                .andExpect(jsonPath("$.data[0].published.status").value("ENABLED"))
                .andExpect(jsonPath("$.data[0].hasUnpublishedChanges").value(true))
                .andExpect(jsonPath("$.data[0].deletable").value(false))
                .andExpect(jsonPath("$.data[0].lockVersion").value(1));
    }

    @Test
    @DisplayName("新增频道携带编码/名称/排序/状态")
    void 新增契约() throws Exception {
        when(channelAppService.create(any())).thenReturn(ExploreChannelView.builder()
                .channelCode("NEWS")
                .draft(revision("ecr_2", ExploreRevisionStatus.DRAFT))
                .hasUnpublishedChanges(true)
                .deletable(true)
                .lockVersion(0)
                .build());

        mockMvc.perform(post("/api/mpt/explore/v1/channels")
                        .contentType("application/json")
                        .content("{\"channelCode\":\"NEWS\",\"name\":\"资讯\",\"sortNo\":40,"
                                + "\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelCode").value("NEWS"));

        ArgumentCaptor<ExploreChannelCmd> captor = ArgumentCaptor.forClass(ExploreChannelCmd.class);
        verify(channelAppService).create(captor.capture());
        assertEquals("NEWS", captor.getValue().getChannelCode());
        assertEquals(40, captor.getValue().getSortNo());
        assertEquals("ENABLED", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("发布携带 requestId/expectedCurrentRevisionId/lockVersion")
    void 发布契约() throws Exception {
        when(channelAppService.publish(any())).thenReturn(ExploreChannelView.builder()
                .channelCode("NEWS")
                .published(revision("ecr_2", ExploreRevisionStatus.PUBLISHED))
                .deletable(true)
                .lockVersion(2)
                .build());

        mockMvc.perform(post("/api/mpt/explore/v1/channels/NEWS/action/publish")
                        .contentType("application/json")
                        .content("{\"requestId\":\"req-1\",\"expectedCurrentRevisionId\":\"ecr_1\","
                                + "\"lockVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published.revisionId").value("ecr_2"));

        ArgumentCaptor<ExploreChannelPublishCmd> captor =
                ArgumentCaptor.forClass(ExploreChannelPublishCmd.class);
        verify(channelAppService).publish(captor.capture());
        assertEquals("req-1", captor.getValue().getRequestId());
        assertEquals("ecr_1", captor.getValue().getExpectedCurrentRevisionId());
        assertEquals(1, captor.getValue().getLockVersion());
        assertEquals("NEWS", captor.getValue().getChannelCode());
    }

    @Test
    @DisplayName("删除携带 lockVersion")
    void 删除契约() throws Exception {
        mockMvc.perform(delete("/api/mpt/explore/v1/channels/NEWS").param("lockVersion", "3"))
                .andExpect(status().isOk());

        verify(channelAppService).delete(eq("NEWS"), eq(3), any());
    }

    private ExploreChannelRevision revision(String revisionId, ExploreRevisionStatus status) {
        return ExploreChannelRevision.builder()
                .revisionId(revisionId)
                .channelCode("NEWS")
                .revisionNo(status == ExploreRevisionStatus.DRAFT ? 2 : 1)
                .name("资讯")
                .sortNo(40)
                .status(ExploreChannelStatus.ENABLED)
                .revisionStatus(status)
                .build();
    }
}
