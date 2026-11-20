package net.hwyz.iov.cloud.mkt.cms.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * 当前协议发布包出参（完整成套快照）。
 * <p>
 * MKT-CMS-DSN-CR-001 §6.1：一次一致性读取返回发布上下文与两份协议正文，
 * 固定两项协议，调用方按 {@code agreementType} 识别，不依赖数组顺序。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementBundleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 发布包标识
     */
    private String bundleId;

    /**
     * 场景
     */
    private String scene;

    /**
     * 市场
     */
    private String market;

    /**
     * 语言
     */
    private String locale;

    /**
     * 品牌
     */
    private String brandCode;

    /**
     * 车系
     */
    private String carLineCode;

    /**
     * 服务端发布时间（ISO-8601，固定 3 位毫秒，带 offset）
     */
    private Instant publishedAt;

    /**
     * 固定两项协议内容
     */
    private List<AgreementContentResponse> agreements;
}
