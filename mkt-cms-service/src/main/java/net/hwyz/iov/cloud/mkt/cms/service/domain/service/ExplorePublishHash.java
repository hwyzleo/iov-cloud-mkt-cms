package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.util.HexSha256;

/**
 * 探索页发布命令哈希。
 * <p>
 * MKT-CMS-DSN-CR-003 §5.2：先规范化命令（不含 requestId 与服务端字段）并计算 requestHash，
 * 同一 requestId 对应不同哈希即幂等冲突。
 *
 * @author hwyz_leo
 */
public final class ExplorePublishHash {

    private ExplorePublishHash() {
    }

    /**
     * 计算规范化发布命令哈希
     *
     * @param canonicalJson 规范化 JSON（Map 键排序、紧凑输出）
     * @return SHA-256 小写十六进制
     */
    public static String of(String canonicalJson) {
        return HexSha256.ofUtf8(canonicalJson);
    }
}
