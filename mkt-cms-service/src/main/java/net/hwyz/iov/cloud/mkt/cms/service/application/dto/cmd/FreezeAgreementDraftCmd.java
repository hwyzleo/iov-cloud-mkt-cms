package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协议草稿定稿命令（对指定乐观锁版本定稿，重复定稿幂等）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreezeAgreementDraftCmd {

    private String draftId;

    private Integer lockVersion;

    private String operator;
}
