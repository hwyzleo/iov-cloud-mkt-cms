package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import net.hwyz.iov.cloud.edd.oss.api.vo.request.GenerateImageUploadRequest;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageObjectResponse;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageUploadResponse;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 探索页素材上传应用服务测试（MKT-CMS-DSN-CR-003 / 发布后 EDD-OSS 归属修复）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("探索页素材 - CMS 调用 EDD-OSS 申请/确认")
class ExploreAssetAppServiceTest {

    @Mock
    private ImageService imageService;

    private ExploreAssetAppService service;

    @BeforeEach
    void setUp() {
        service = new ExploreAssetAppService(imageService);
    }

    @Test
    @DisplayName("申请上传返回 objectId 与预签名地址，不伪造成功")
    void 申请上传() {
        when(imageService.generateUpload(any(GenerateImageUploadRequest.class)))
                .thenReturn(ImageUploadResponse.builder()
                        .objectId("obj-1")
                        .objectKey("images/mkt-cms/obj-1.png")
                        .uploadUrl("https://cos.example/put")
                        .httpMethod("PUT")
                        .contentType("image/png")
                        .expiresAt(1L)
                        .build());

        Map<String, Object> view = service.generate("cover.png", "image/png", 1024L);
        assertEquals("obj-1", view.get("objectId"));
        assertEquals("https://cos.example/put", view.get("uploadUrl"));
    }

    @Test
    @DisplayName("非法 MIME/大小在调用 OSS 前拒绝")
    void 非法参数() {
        ExploreException mime = assertThrows(ExploreException.class,
                () -> service.generate("cover.gif", "image/gif", 1024L));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, mime.getErrorCode());
        ExploreException size = assertThrows(ExploreException.class,
                () -> service.generate("cover.png", "image/png", 0L));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, size.getErrorCode());
    }

    @Test
    @DisplayName("完成确认返回 accessUrl/实际 MIME 与大小")
    void 完成确认() {
        when(imageService.completeUpload("obj-1")).thenReturn(ImageObjectResponse.builder()
                .objectId("obj-1")
                .accessUrl("https://openiov-1471221926.cos.ap-guangzhou.myqcloud.com/a.png")
                .actualContentType("image/png")
                .actualSize(2048L)
                .status("UPLOADED")
                .build());

        Map<String, Object> view = service.complete("obj-1");
        assertEquals("https://openiov-1471221926.cos.ap-guangzhou.myqcloud.com/a.png", view.get("accessUrl"));
        assertEquals(2048L, view.get("size"));
    }

    @Test
    @DisplayName("OSS 不可用时返回 105016，不返回伪成功")
    void OSS不可用() {
        when(imageService.generateUpload(any(GenerateImageUploadRequest.class)))
                .thenThrow(new IllegalStateException("oss down"));
        ExploreException exception = assertThrows(ExploreException.class,
                () -> service.generate("cover.png", "image/png", 1024L));
        assertEquals(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE, exception.getErrorCode());
    }
}
