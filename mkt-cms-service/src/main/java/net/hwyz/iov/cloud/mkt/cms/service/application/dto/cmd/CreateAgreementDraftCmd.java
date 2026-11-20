package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 新建协议草稿命令。
 * <p>
 * 场景必传；市场、语言、品牌、车系可省略（由服务端统一补默认并在响应中显式返回）。
 * 操作人来自安全上下文，不接受请求体自报。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgreementDraftCmd {

    private String agreementType;

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private String title;

    private String version;

    private LocalDate agreementDate;

    private String body;

    private String operator;
}
