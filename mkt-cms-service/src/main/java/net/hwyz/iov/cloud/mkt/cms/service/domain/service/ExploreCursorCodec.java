package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.HexSha256;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreCursor;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 探索页 keyset 游标编解码器。
 * <p>
 * 游标为不透明 Base64URL 字符串：服务端解析并校验 releaseId、componentId、sortNo、itemId，
 * 附带内容校验位以尽早识别畸形游标（MKT-CMS-DSN-CR-003 §6.2 / AT-018）。
 *
 * @author hwyz_leo
 */
public class ExploreCursorCodec {

    private static final String VERSION = "v1";

    private static final String SEPARATOR = "\n";

    private static final int PARTS = 6;

    private static final int CHECKSUM_LENGTH = 16;

    /**
     * 编码游标
     *
     * @param releaseId   发布版本标识
     * @param componentId 组件标识
     * @param sortNo      最后一条条目排序
     * @param itemId      最后一条条目标识
     * @return 不透明游标
     */
    public String encode(String releaseId, String componentId, int sortNo, String itemId) {
        String payload = String.join(SEPARATOR, VERSION, releaseId, componentId,
                String.valueOf(sortNo), itemId);
        String checksum = HexSha256.ofUtf8(payload).substring(0, CHECKSUM_LENGTH);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + SEPARATOR + checksum).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解码并校验游标
     *
     * @param token 不透明游标
     * @return 游标内容
     */
    public ExploreCursor decode(String token) {
        if (!StringUtils.hasText(token)) {
            throw invalid();
        }
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(token.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
        String[] parts = decoded.split(SEPARATOR, -1);
        if (parts.length != PARTS || !VERSION.equals(parts[0])) {
            throw invalid();
        }
        String payload = String.join(SEPARATOR, parts[0], parts[1], parts[2], parts[3], parts[4]);
        String expected = HexSha256.ofUtf8(payload).substring(0, CHECKSUM_LENGTH);
        if (!expected.equals(parts[5])) {
            throw invalid();
        }
        if (!StringUtils.hasText(parts[1]) || !StringUtils.hasText(parts[2]) || !StringUtils.hasText(parts[4])) {
            throw invalid();
        }
        int sortNo;
        try {
            sortNo = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            throw invalid();
        }
        if (sortNo < 0) {
            throw invalid();
        }
        return new ExploreCursor(parts[1], parts[2], sortNo, parts[4]);
    }

    private ExploreException invalid() {
        return new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "游标格式非法");
    }
}
