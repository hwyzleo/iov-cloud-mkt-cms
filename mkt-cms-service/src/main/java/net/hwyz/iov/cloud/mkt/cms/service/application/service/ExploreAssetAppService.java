package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import net.hwyz.iov.cloud.edd.oss.api.vo.request.GenerateImageUploadRequest;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageObjectResponse;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageUploadResponse;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreGuard;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 探索页素材上传应用服务。
 * <p>
 * MKT-CMS-DSN-CR-003 §3 / §8：CMS 不新建对象存储，素材二进制复用 EDD-OSS；
 * 由 CMS 服务作为调用方申请、完成上传（EDD-OSS 记录 serviceName=mkt-cms），
 * 管理端只拿到预签名地址并直传 COS，CMS 最终保存 URL 与宽高/MIME/大小。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreAssetAppService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final ImageService imageService;

    /**
     * 申请素材上传（由 CMS 调用 EDD-OSS，调用方服务名为 mkt-cms）
     *
     * @param originalFilename 原始文件名
     * @param contentType      声明 MIME
     * @param size             声明字节数
     * @return 预签名上传参数
     */
    public Map<String, Object> generate(String originalFilename, String contentType, Long size) {
        String filename = ExploreGuard.requireText(originalFilename, "文件名", 255);
        String mime = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(mime)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "仅允许 JPEG / PNG / WebP 图片");
        }
        if (size == null || size <= 0 || size > MAX_SIZE_BYTES) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "图片大小必须在 1 字节到 10MB 之间");
        }
        try {
            ImageUploadResponse upload = imageService.generateUpload(GenerateImageUploadRequest.builder()
                    .originalFilename(filename)
                    .contentType(mime)
                    .size(size)
                    .build());
            if (upload == null || !StringUtils.hasText(upload.getObjectId())
                    || !StringUtils.hasText(upload.getUploadUrl())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE,
                        "素材服务未返回有效上传任务");
            }
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("objectId", upload.getObjectId());
            view.put("objectKey", upload.getObjectKey());
            view.put("uploadUrl", upload.getUploadUrl());
            view.put("httpMethod", upload.getHttpMethod());
            view.put("contentType", upload.getContentType());
            view.put("expiresAt", upload.getExpiresAt());
            log.info("探索页素材上传任务创建成功: objectId={}, contentType={}, size={}",
                    upload.getObjectId(), mime, size);
            return view;
        } catch (ExploreException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("探索页素材上传任务创建失败: filename={}, contentType={}, size={}, message={}",
                    filename, mime, size, exception.getMessage());
            throw new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE,
                    "素材服务暂不可用，请稍后重试", exception);
        }
    }

    /**
     * 完成素材上传确认
     *
     * @param objectId 上传任务标识
     * @return 已确认素材信息
     */
    public Map<String, Object> complete(String objectId) {
        String id = ExploreGuard.requireText(objectId, "上传任务标识", 64);
        try {
            ImageObjectResponse image = imageService.completeUpload(id);
            if (image == null || !StringUtils.hasText(image.getAccessUrl())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE,
                        "素材服务未返回可访问地址");
            }
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("objectId", image.getObjectId());
            view.put("objectKey", image.getObjectKey());
            view.put("accessUrl", image.getAccessUrl());
            view.put("contentType", image.getActualContentType());
            view.put("size", image.getActualSize());
            view.put("status", image.getStatus());
            log.info("探索页素材上传确认成功: objectId={}, contentType={}, size={}",
                    image.getObjectId(), image.getActualContentType(), image.getActualSize());
            return view;
        } catch (ExploreException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("探索页素材上传确认失败: objectId={}, message={}", id, exception.getMessage());
            throw new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE,
                    "素材上传确认失败，请重试", exception);
        }
    }
}
