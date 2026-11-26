package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreCursor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 探索页 keyset 游标编解码测试（MKT-CMS-DSN-CR-003 §6.2 / AT-016 / AT-018）。
 *
 * @author hwyz_leo
 */
@DisplayName("探索页游标 - 往返/篡改/畸形")
class ExploreCursorCodecTest {

    private final ExploreCursorCodec codec = new ExploreCursorCodec();

    @Test
    @DisplayName("游标往返保持 releaseId/componentId/sortNo/itemId")
    void 往返() {
        String token = codec.encode("er_01", "ecp_01", 30, "epi_01");
        ExploreCursor cursor = codec.decode(token);
        assertEquals("er_01", cursor.getReleaseId());
        assertEquals("ecp_01", cursor.getComponentId());
        assertEquals(30, cursor.getSortNo());
        assertEquals("epi_01", cursor.getItemId());
    }

    @Test
    @DisplayName("篡改内容导致校验失败")
    void 篡改() {
        String token = codec.encode("er_01", "ecp_01", 30, "epi_01");
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("A") ? "BB" : "AA");
        assertThrows(ExploreException.class, () -> codec.decode(tampered));
    }

    @Test
    @DisplayName("畸形/空游标拒绝")
    void 畸形() {
        assertThrows(ExploreException.class, () -> codec.decode(null));
        assertThrows(ExploreException.class, () -> codec.decode(""));
        assertThrows(ExploreException.class, () -> codec.decode("not-base64!!"));
        assertThrows(ExploreException.class, () -> codec.decode("YWJj"));
    }
}
