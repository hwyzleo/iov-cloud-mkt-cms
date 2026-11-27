package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;

import java.util.List;

/**
 * 探索页专题封面/详情图集派生（MKT-CMS-DSN-CR-005 §5.2 / D-020）。
 * <p>
 * 封面始终取完整有序图集第一张，不受该首图 showInDetail 取值影响；
 * 详情图集是 showInDetail=true 的有序子集，二者不得相互覆盖。
 * 页面展示快照、手机专题详情与管理视图必须复用同一实现，禁止各自写过滤逻辑。
 *
 * @author hwyz_leo
 */
public final class ExploreTopicGallery {

    private ExploreTopicGallery() {
    }

    /**
     * 封面：完整有序图集第一张
     *
     * @param images 完整有序图集
     * @return 封面；无图时为 null
     */
    public static ExploreImage cover(List<ExploreImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.get(0);
    }

    /**
     * 详情可见图片：保持原相对顺序的 showInDetail=true 子集
     *
     * @param images 完整有序图集
     * @return 详情可见图片；无图时为空列表
     */
    public static List<ExploreImage> detailImages(List<ExploreImage> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream().filter(ExploreImage::detailVisible).toList();
    }
}
