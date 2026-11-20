package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.AgreementWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementRevisionResponse;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementContentAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协议修订管理（MPT 管理后台）。
 * <p>
 * 管理侧可查看未发布修订；本接口不暴露给普通业务读取方。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/agreement/v1/agreement-revisions")
public class MptAgreementRevisionController extends BaseController {

    private final AgreementContentAppService agreementContentAppService;

    /**
     * 查询修订
     *
     * @param revisionId 修订标识
     * @return 修订内容
     */
    @RequiresPermissions("mkt:cms:agreement:query")
    @GetMapping("/{revisionId}")
    public ApiResponse<AgreementRevisionResponse> get(@PathVariable("revisionId") String revisionId) {
        return ApiResponse.ok(AgreementWebAssembler.toVo(
                agreementContentAppService.getRevisionForManagement(revisionId)));
    }
}
