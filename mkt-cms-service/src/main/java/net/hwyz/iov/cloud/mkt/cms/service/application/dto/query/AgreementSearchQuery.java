package net.hwyz.iov.cloud.mkt.cms.service.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协议检索查询（管理侧列表）。
 * <p>
 * 所有条件可为空表示不过滤；语言维度显式存在，同一身份在不同 locale 下的内容分别维护。
 * 分页信息由 {@code BaseController.startPage()} 在数据库阶段介入，不使用内存分页。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementSearchQuery {

    private String agreementType;

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private String title;

    private String version;

    /**
     * 仅查已通过发布包发布的修订（仅修订检索使用）
     */
    private Boolean published;
}
