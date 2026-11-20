package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.AgreementWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.support.OperatorResolver;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.PublishAgreementBundleRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementPublishResponse;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementPublishAppService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协议发布包管理（MPT 管理后台）。
 * <p>
 * MKT-CMS-DSN-CR-001 §4.1/§5：人工立即发布完整协议包，要求 requestId 与 expectedBundleId。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/agreement/v1/agreement-bundles")
public class MptAgreementBundleController extends BaseController {

    private final AgreementPublishAppService agreementPublishAppService;

    /**
     * 立即发布完整协议包（原子切换当前指针）
     *
     * @param request 发布请求
     * @return 发布包
     */
    @RequiresPermissions("mkt:cms:agreement:publish")
    @PostMapping("/action/publish")
    public ApiResponse<AgreementPublishResponse> publish(@RequestBody @Valid PublishAgreementBundleRequest request) {
        return ApiResponse.ok(AgreementWebAssembler.toVo(
                agreementPublishAppService.publish(
                        AgreementWebAssembler.toCmd(request, OperatorResolver.currentOperator()))));
    }
}
