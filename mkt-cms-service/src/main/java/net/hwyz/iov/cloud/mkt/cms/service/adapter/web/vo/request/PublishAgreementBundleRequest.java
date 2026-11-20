package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 协议包发布请求（管理后台）。
 * <p>
 * MKT-CMS-DSN-CR-001 §5.1：{@code expectedBundleId} 字段本身必须出现；
 * 首次发布显式传 {@code null} 表示“预期尚无当前包”。本 VO 通过自定义 setter 记录字段存在性，
 * 避免框架无法区分“字段缺失”和“显式 null”。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
public class PublishAgreementBundleRequest {

    @NotBlank(message = "场景不能为空")
    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    @NotBlank(message = "隐私政策修订标识不能为空")
    private String privacyRevisionId;

    @NotBlank(message = "用户协议修订标识不能为空")
    private String userAgreementRevisionId;

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    private String expectedBundleId;

    /**
     * expectedBundleId 是否在请求体中出现
     */
    @JsonIgnore
    private boolean expectedBundleIdPresent;

    /**
     * 记录 expectedBundleId 字段存在性（显式 null 同样标记为已出现）
     *
     * @param expectedBundleId 期望的当前包标识
     */
    @JsonProperty("expectedBundleId")
    public void setExpectedBundleId(String expectedBundleId) {
        this.expectedBundleId = expectedBundleId;
        this.expectedBundleIdPresent = true;
    }
}
