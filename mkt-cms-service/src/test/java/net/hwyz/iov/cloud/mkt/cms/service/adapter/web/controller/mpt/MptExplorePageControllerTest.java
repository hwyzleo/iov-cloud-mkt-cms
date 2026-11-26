package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler.AgreementExceptionHandler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExplorePagePublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExplorePageData;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExplorePublishResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExplorePageAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExplorePublishAppService;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.support.JsonTestSupport;
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

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 探索页页面发布与预览契约测试（MKT-CMS-DSN-CR-003 / AT-014）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MPT 探索页 - 发布命令/预览契约")
class MptExplorePageControllerTest {

    @Mock
    private ExplorePageAppService pageAppService;
    @Mock
    private ExplorePublishAppService publishAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MptExplorePageController controller =
                new MptExplorePageController(pageAppService, publishAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2MessageConverter())
                .build();
    }

    @Test
    @DisplayName("发布请求携带 requestId/expectedReleaseId/时间并返回不可变版本")
    void 发布契约() throws Exception {
        ExplorePage release = ExplorePage.builder()
                .pageId("er_01")
                .channelCode("RECOMMEND")
                .pageVersion(1)
                .status(ExplorePageStatus.PUBLISHED)
                .publishedAt(Instant.parse("2026-11-26T10:00:00Z"))
                .publishedBy("tester")
                .requestId("req-1")
                .build();
        when(publishAppService.publish(any())).thenReturn(new ExplorePublishResult(release));

        mockMvc.perform(post("/api/mpt/explore/v1/pages/RECOMMEND/action/publish")
                        .contentType("application/json")
                        .content("{\"requestId\":\"req-1\",\"expectedReleaseId\":null,"
                                + "\"publishType\":\"IMMEDIATE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.releaseId").value("er_01"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        ArgumentCaptor<ExplorePagePublishCmd> captor = ArgumentCaptor.forClass(ExplorePagePublishCmd.class);
        verify(publishAppService).publish(captor.capture());
        assertEquals("req-1", captor.getValue().getRequestId());
        assertEquals("IMMEDIATE", captor.getValue().getPublishType());
        assertEquals("RECOMMEND", captor.getValue().getChannelCode());
    }

    @Test
    @DisplayName("草稿读取在无草稿时返回 exists=false 与 lockVersion=0")
    void 草稿契约() throws Exception {
        when(pageAppService.getDraft("RECOMMEND"))
                .thenReturn(new ExplorePageData(null, List.of()));

        mockMvc.perform(get("/api/mpt/explore/v1/pages/RECOMMEND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(false))
                .andExpect(jsonPath("$.data.lockVersion").value(0))
                .andExpect(jsonPath("$.data.components").isArray());
    }

    /**
     * 与运行形态一致的 JSON 转换器（JavaTimeModule + 非时间戳）
     */
    private static final class MappingJackson2MessageConverter extends MappingJackson2HttpMessageConverter {
        private MappingJackson2MessageConverter() {
            super(JsonTestSupport.objectMapper());
        }
    }
}
