package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 协议草稿定稿请求（对指定乐观锁版本定稿）。
 *
 * @author hwyz_leo
 */
@Data
public class FreezeAgreementDraftRequest {

    @NotNull(message = "lockVersion 不能为空")
    private Integer lockVersion;
}
