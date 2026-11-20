package net.hwyz.iov.cloud.mkt.cms.service.common.util;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 工具（协议正文摘要与业务命令哈希）。
 * <p>
 * 摘要基于精确 UTF-8 字节计算，不按清洗规则重写正文；摘要不是数字签名，也不是用户同意证明。
 *
 * @author hwyz_leo
 */
public final class HexSha256 {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HexSha256() {
    }

    /**
     * 计算 UTF-8 文本的 SHA-256 小写十六进制摘要
     *
     * @param text 文本
     * @return 64 位小写十六进制摘要
     */
    public static String ofUtf8(String text) {
        return ofBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 计算字节摘要
     *
     * @param bytes 字节
     * @return 64 位小写十六进制摘要
     */
    public static String ofBytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            char[] out = new char[hashed.length * 2];
            for (int i = 0; i < hashed.length; i++) {
                int value = hashed[i] & 0xFF;
                out[i * 2] = HEX[value >>> 4];
                out[i * 2 + 1] = HEX[value & 0x0F];
            }
            return new String(out);
        } catch (NoSuchAlgorithmException e) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_TEMPORARILY_UNAVAILABLE, e);
        }
    }
}
