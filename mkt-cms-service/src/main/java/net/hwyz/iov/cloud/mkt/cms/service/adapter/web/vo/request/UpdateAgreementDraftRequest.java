package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 修改协议草稿请求（必须携带乐观锁版本，冲突不覆盖）。
 *
 * @author hwyz_leo
 */
@Data
public class UpdateAgreementDraftRequest {

    @NotNull(message = "lockVersion 不能为空")
    private Integer lockVersion;

    private String title;

    private String version;

    private LocalDate agreementDate;

    private String body;
}
