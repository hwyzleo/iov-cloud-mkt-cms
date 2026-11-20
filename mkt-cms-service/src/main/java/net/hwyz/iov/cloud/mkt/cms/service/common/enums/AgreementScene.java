package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;

/**
 * 协议场景（首版只开放车联网车端激活场景）。
 *
 * @author hwyz_leo
 */
public enum AgreementScene {

    /**
     * 车端激活场景
     */
    IVI_ACTIVATION;

    /**
     * 解析场景；场景为唯一必传范围维度，空值或不受支持值拒绝。
     *
     * @param value 场景文本
     * @return 场景
     */
    public static AgreementScene parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "场景不能为空");
        }
        String normalized = value.trim();
        for (AgreementScene scene : values()) {
            if (scene.name().equalsIgnoreCase(normalized)) {
                return scene;
            }
        }
        throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "不受支持的场景: " + value);
    }
}
