package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 协议检索条件（列表查询，不含正文）。
 * <p>
 * 管理端按类型、范围与标题/版本检索；所有维度可为空表示不过滤，
 * 语言维度显式存在，便于同一身份在不同 locale 下的内容分别维护。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementSearchCriteria {

    /**
     * 协议类型，可空
     */
    private String agreementType;

    /**
     * 场景，可空
     */
    private String scene;

    /**
     * 市场，可空
     */
    private String market;

    /**
     * 语言，可空
     */
    private String locale;

    /**
     * 品牌，可空
     */
    private String brandCode;

    /**
     * 车系，可空
     */
    private String carLineCode;

    /**
     * 标题模糊匹配，可空
     */
    private String title;

    /**
     * 业务版本精确匹配，可空
     */
    private String version;

    /**
     * 仅查已被发布包引用的修订（仅修订检索使用），可空
     */
    private Boolean published;
}
