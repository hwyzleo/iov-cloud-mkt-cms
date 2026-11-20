package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;

/**
 * 协议类型（首版只开放两类）。
 *
 * @author hwyz_leo
 */
public enum AgreementType {

    /**
     * 隐私政策
     */
    PRIVACY_POLICY,

    /**
     * 用户协议
     */
    USER_AGREEMENT;

    /**
     * 解析协议类型，不区分大小写；空值或不受支持值拒绝。
     *
     * @param value 协议类型文本
     * @return 协议类型
     */
    public static AgreementType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议类型不能为空");
        }
        String normalized = value.trim();
        for (AgreementType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "不受支持的协议类型: " + value);
    }
}
