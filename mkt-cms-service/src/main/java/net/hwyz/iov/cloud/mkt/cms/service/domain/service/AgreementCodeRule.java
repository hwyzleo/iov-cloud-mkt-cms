package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.HexSha256;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;

/**
 * 协议身份标识规则。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.2：agreement_code 是协议身份的稳定业务标识，由服务端分配。
 * 本实现按协议身份（类型 + 五维范围）确定性派生，保证同一身份永远得到同一标识，
 * 不依赖插入顺序，也不需要额外的身份表。
 *
 * @author hwyz_leo
 */
public final class AgreementCodeRule {

    private static final String PREFIX = "AGR";

    private static final int DIGEST_LENGTH = 8;

    private AgreementCodeRule() {
    }

    /**
     * 派生协议身份标识
     *
     * @param type  协议类型
     * @param scope 规范化适用范围
     * @return 稳定协议身份标识
     */
    public static String of(AgreementType type, AgreementScope scope) {
        String digest = HexSha256.ofUtf8(scope.identityKey()).substring(0, DIGEST_LENGTH).toUpperCase();
        return PREFIX + "-" + type.name() + "-" + scope.getScene().name() + "-" + digest;
    }
}
