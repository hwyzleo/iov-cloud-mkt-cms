package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 专题封面/详情图集派生测试（MKT-CMS-DSN-CR-005 §5.2 / AT-028）。
 *
 * @author hwyz_leo
 */
@DisplayName("专题封面与详情图集派生")
class ExploreTopicGalleryTest {

    @Test
    @DisplayName("封面始终取完整图集首图，首图 showInDetail=false 不影响封面")
    void 封面取完整首图() {
        ExploreImage first = image("first.png", false);
        ExploreImage second = image("second.png", true);

        assertEquals("first.png", ExploreTopicGallery.cover(List.of(first, second)).getUrl());
        assertEquals(List.of(second), ExploreTopicGallery.detailImages(List.of(first, second)));
    }

    @Test
    @DisplayName("详情图集保持原相对顺序并过滤 false")
    void 详情子集保序() {
        List<ExploreImage> images = List.of(
                image("a.png", true),
                image("b.png", false),
                image("c.png", null),
                image("d.png", true));

        List<String> detailUrls = ExploreTopicGallery.detailImages(images).stream()
                .map(ExploreImage::getUrl)
                .toList();

        assertEquals(List.of("a.png", "c.png", "d.png"), detailUrls);
    }

    @Test
    @DisplayName("空图集：封面为空、详情为空")
    void 空图集() {
        assertNull(ExploreTopicGallery.cover(List.of()));
        assertNull(ExploreTopicGallery.cover(null));
        assertTrue(ExploreTopicGallery.detailImages(List.of()).isEmpty());
        assertTrue(ExploreTopicGallery.detailImages(null).isEmpty());
    }

    @Test
    @DisplayName("全部 showInDetail=false：详情为空但封面保留完整首图")
    void 全部隐藏() {
        List<ExploreImage> images = List.of(image("a.png", false), image("b.png", false));

        assertTrue(ExploreTopicGallery.detailImages(images).isEmpty());
        assertEquals("a.png", ExploreTopicGallery.cover(images).getUrl());
    }

    @Test
    @DisplayName("showInDetail 为空按 true 兼容")
    void 空标志按详情可见() {
        ExploreImage image = image("a.png", null);

        assertTrue(image.detailVisible());
        assertEquals(1, ExploreTopicGallery.detailImages(List.of(image)).size());
        assertFalse(image("b.png", false).detailVisible());
    }

    private ExploreImage image(String name, Boolean showInDetail) {
        return ExploreImage.builder()
                .url(name)
                .width(1600)
                .height(900)
                .mime("image/png")
                .size(2048L)
                .showInDetail(showInDetail)
                .build();
    }
}
