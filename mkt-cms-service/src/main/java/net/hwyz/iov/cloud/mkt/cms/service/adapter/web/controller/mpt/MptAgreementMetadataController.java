package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.AgreementWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementMetadataResponse;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementMetadataAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协议管理元数据（MPT 管理后台）。
 * <p>
 * 下发受支持语言/市场/品牌/车系与清洗策略信息，前端不硬编码范围值：
 * 后续扩展内容语言或其他维度只需调整后端受支持清单。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/agreement/v1/agreement-metadata")
public class MptAgreementMetadataController extends BaseController {

    private final AgreementMetadataAppService agreementMetadataAppService;

    /**
     * 查询协议管理元数据
     *
     * @return 元数据
     */
    @RequiresPermissions("mkt:cms:agreement:query")
    @GetMapping
    public ApiResponse<AgreementMetadataResponse> metadata() {
        return ApiResponse.ok(AgreementWebAssembler.toVo(agreementMetadataAppService.metadata()));
    }
}
