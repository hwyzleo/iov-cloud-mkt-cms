package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 探索页素材（图片）元数据。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.3：仅允许 JPEG/PNG/WebP，必须记录宽高、MIME、大小和 HTTPS URL；
 * 手机端按宽高字段预留位置，避免布局跳动。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreImage {

    private String url;

    private Integer width;

    private Integer height;

    private String mime;

    private Long size;
}
