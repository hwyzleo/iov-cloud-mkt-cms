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
 * MKT-CMS-DSN-CR-005 §5.1：专题图片增加详情可见标志，缺省与存量数据均为 true；
 * 文章图集不开放该运营配置。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreImage {

    private String url;

    private Integer width;

    private Integer height;

    private String mime;

    private Long size;

    /**
     * 是否进入详情图集；仅专题图片使用，null 按 true 兼容
     */
    private Boolean showInDetail;

    /**
     * 详情可见标志（null 视为 true）
     *
     * @return 是否进入详情图集
     */
    public boolean detailVisible() {
        return showInDetail == null || showInDetail;
    }
}
