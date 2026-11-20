package net.hwyz.iov.cloud.mkt.cms.service.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协议范围查询（当前包读取）。
 * <p>
 * 场景必传；其余四维省略时由服务端补默认，显式空值或不受支持值拒绝；
 * 不接收 VIN、账号等用户标识。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementScopeQuery {

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;
}
