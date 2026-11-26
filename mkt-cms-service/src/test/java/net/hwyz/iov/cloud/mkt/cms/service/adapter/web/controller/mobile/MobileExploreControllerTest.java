package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mobile;

import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler.AgreementExceptionHandler;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreMobileQueryAppService;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreComponentView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageView;
import net.hwyz.iov.cloud.mkt.cms.service.support.JsonTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 探索页手机只读契约测试（MKT-CMS-DSN-CR-003 / AT-016）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("手机探索页 - schemaVersion/no-cache/ETag/详情")
class MobileExploreControllerTest {

    @Mock
    private ExploreMobileQueryAppService mobileQueryAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MobileExploreController controller = new MobileExploreController(mobileQueryAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2MessageConverter())
                .build();
    }

    @Test
    @DisplayName("当前页返回 schemaVersion、关联 Etag 与 no-cache，条件请求返回 304")
    void 当前页契约() throws Exception {
        ExplorePageView view = ExplorePageView.builder()
                .channelCode("RECOMMEND")
                .channelName("推荐")
                .releaseId("er_01")
                .publishedAt("2026-11-26T10:00:00Z")
                .components(List.of(ExploreComponentView.builder()
                        .type("ARTICLE_FEED")
                        .componentId("ecp_01")
                        .sortNo(30)
                        .items(List.of())
                        .build()))
                .build();
        when(mobileQueryAppService.currentPage("RECOMMEND", 10)).thenReturn(view);

        mockMvc.perform(get("/api/mobile/explore/v1/pages/current")
                        .param("channelCode", "RECOMMEND")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(header().string("ETag", "\"er_01\""))
                .andExpect(jsonPath("$.data.schemaVersion").value(1))
                .andExpect(jsonPath("$.data.releaseId").value("er_01"))
                .andExpect(jsonPath("$.data.components[0].type").value("ARTICLE_FEED"));

        mockMvc.perform(get("/api/mobile/explore/v1/pages/current")
                        .param("channelCode", "RECOMMEND")
                        .param("pageSize", "10")
                        .header("If-None-Match", "\"er_01\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    @DisplayName("文章流翻页必须携带 componentId 并返回对应组件")
    void 文章流翻页组件契约() throws Exception {
        when(mobileQueryAppService.articleFeed("RECOMMEND", "ecp_02", "er_01", "cursor_01", 10))
                .thenReturn(Map.of("releaseId", "er_01", "componentId", "ecp_02",
                        "items", List.of(), "hasMore", false));

        mockMvc.perform(get("/api/mobile/explore/v1/article-feed")
                        .param("channelCode", "RECOMMEND")
                        .param("componentId", "ecp_02")
                        .param("releaseId", "er_01")
                        .param("cursor", "cursor_01")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.releaseId").value("er_01"))
                .andExpect(jsonPath("$.data.componentId").value("ecp_02"));

        mockMvc.perform(get("/api/mobile/explore/v1/article-feed")
                        .param("channelCode", "RECOMMEND")
                        .param("releaseId", "er_01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(105009));
    }

    @Test
    @DisplayName("文章详情返回官方正文，UGC 引用不返回正文")
    void 文章详情契约() throws Exception {
        when(mobileQueryAppService.articleDetail("ea_01", "ear_01"))
                .thenReturn(Map.of("articleId", "ea_01", "revisionId", "ear_01",
                        "sourceType", "OFFICIAL", "body", "<p>正文</p>",
                        "contentHash", "hash", "publishedAt", Instant.parse("2026-11-26T10:00:00Z")));

        mockMvc.perform(get("/api/mobile/explore/v1/articles/ea_01/revisions/ear_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("<p>正文</p>"))
                .andExpect(jsonPath("$.data.sourceType").value("OFFICIAL"));
    }

    private static final class MappingJackson2MessageConverter extends MappingJackson2HttpMessageConverter {
        private MappingJackson2MessageConverter() {
            super(JsonTestSupport.objectMapper());
        }
    }
}
