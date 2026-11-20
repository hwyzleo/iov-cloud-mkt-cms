package net.hwyz.iov.cloud.mkt.cms.api.service;

import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementBundleResponse;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementContentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 协议内容读取服务接口（内部服务调用）。
 * <p>
 * MKT-CMS-DSN-CR-001 §6（D-004）：路径前缀固定 {@code /api/service/content/v1}，
 * 只服务受信下游（TSP 等），不接受匿名车端直连，也不提供客户端自选草稿的通道。
 * 当前包查询不接收 VIN / Account，只按完整五维 Scope 精确读取，缺省维度由服务端统一补齐。
 * <p>
 * 业务错误以平台统一错误包装返回（非 2xx HTTP 语义 + 错误码），调用方必须校验后才可展示。
 *
 * @author hwyz_leo
 */
@FeignClient(name = "mkt-cms", path = "/api/service/content/v1")
public interface AgreementContentService {

    /**
     * 按范围读取当前完整协议发布包
     *
     * @param scene       场景，必传（首版只开放 IVI_ACTIVATION）
     * @param market      市场，缺省规范化为 DEFAULT；显式空值或不受支持值拒绝
     * @param locale      语言，缺省规范化为 zh-CN；显式空值或不受支持值拒绝
     * @param brandCode   品牌，缺省规范化为 DEFAULT；显式空值或不受支持值拒绝
     * @param carLineCode 车系，缺省规范化为 DEFAULT；显式空值或不受支持值拒绝
     * @return 当前发布包与两份协议正文
     */
    @GetMapping("/agreement-bundles/current")
    AgreementBundleResponse getCurrentBundle(@RequestParam("scene") String scene,
                                             @RequestParam(value = "market", required = false) String market,
                                             @RequestParam(value = "locale", required = false) String locale,
                                             @RequestParam(value = "brandCode", required = false) String brandCode,
                                             @RequestParam(value = "carLineCode", required = false) String carLineCode);

    /**
     * 按已发布修订标识精确回读历史正文
     *
     * @param revisionId 修订标识
     * @return 单份协议内容；仅被至少一个已提交发布包引用时返回
     */
    @GetMapping("/agreement-revisions/{revisionId}")
    AgreementContentResponse getPublishedRevision(@PathVariable("revisionId") String revisionId);
}
