package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新建协议草稿请求（管理后台）。
 * <p>
 * 市场、语言、品牌、车系可省略（服务端补默认并显式返回）；显式空串或不受支持值拒绝。
 *
 * @author hwyz_leo
 */
@Data
public class CreateAgreementDraftRequest {

    /**
     * 协议类型：PRIVACY_POLICY / USER_AGREEMENT
     */
    @NotBlank(message = "协议类型不能为空")
    private String agreementType;

    /**
     * 场景：IVI_ACTIVATION（必传）
     */
    @NotBlank(message = "场景不能为空")
    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    @NotBlank(message = "协议标题不能为空")
    private String title;

    @NotBlank(message = "协议业务版本不能为空")
    private String version;

    @NotNull(message = "协议日期不能为空")
    private LocalDate agreementDate;

    /**
     * 原始 HTML 片段；服务端返回清洗后的实际正文
     */
    @NotBlank(message = "协议正文不能为空")
    private String body;
}
