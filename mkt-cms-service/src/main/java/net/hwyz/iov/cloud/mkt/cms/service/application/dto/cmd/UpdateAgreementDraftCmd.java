package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 修改协议草稿命令（必须携带乐观锁版本，冲突不覆盖）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgreementDraftCmd {

    private String draftId;

    private Integer lockVersion;

    private String title;

    private String version;

    private LocalDate agreementDate;

    private String body;

    private String operator;
}
