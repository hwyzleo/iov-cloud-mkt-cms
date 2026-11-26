package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 文章多图布局建议测试（MKT-CMS-DSN-CR-003 v0.4）。
 *
 * @author hwyz_leo
 */
@DisplayName("文章多图 - 1/2/3/4/5-9/>9 布局")
class ExploreImageLayoutTest {

    @Test
    @DisplayName("按数量选择最合适布局")
    void 布局映射() {
        assertEquals(ExploreImageLayout.SINGLE, ExploreImageLayout.of(0));
        assertEquals(ExploreImageLayout.SINGLE, ExploreImageLayout.of(1));
        assertEquals(ExploreImageLayout.TWO_COLUMNS, ExploreImageLayout.of(2));
        assertEquals(ExploreImageLayout.ONE_LARGE_TWO_SMALL, ExploreImageLayout.of(3));
        assertEquals(ExploreImageLayout.TWO_BY_TWO, ExploreImageLayout.of(4));
        assertEquals(ExploreImageLayout.THREE_COLUMNS, ExploreImageLayout.of(5));
        assertEquals(ExploreImageLayout.THREE_COLUMNS, ExploreImageLayout.of(9));
        assertEquals(ExploreImageLayout.NINE_GRID_WITH_COUNT, ExploreImageLayout.of(10));
    }
}
