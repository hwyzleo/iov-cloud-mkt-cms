package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.AgreementWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.support.OperatorResolver;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.CreateAgreementDraftRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.FreezeAgreementDraftRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.UpdateAgreementDraftRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementDraftResponse;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementRevisionResponse;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.FreezeAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementDraftAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协议草稿管理（MPT 管理后台）。
 * <p>
 * MKT-CMS-DSN-CR-001 §4.1：首版只提供管理 API，不含管理 UI、PDF 或定时生效；
 * 权限认证复用工程已有机制，管理入口不得无鉴权。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/agreement/v1/agreement-drafts")
public class MptAgreementDraftController extends BaseController {

    private final AgreementDraftAppService agreementDraftAppService;

    /**
     * 新建草稿
     *
     * @param request 新建请求
     * @return 保存后的草稿（含清洗后的正文、清洗策略版本与调整标记）
     */
    @RequiresPermissions("mkt:cms:agreement:add")
    @PostMapping
    public ApiResponse<AgreementDraftResponse> create(@RequestBody @Valid CreateAgreementDraftRequest request) {
        return ApiResponse.ok(AgreementWebAssembler.toVo(
                agreementDraftAppService.create(AgreementWebAssembler.toCmd(request, OperatorResolver.currentOperator()))));
    }

    /**
     * 修改草稿
     *
     * @param draftId 草稿标识
     * @param request 修改请求
     * @return 保存后的草稿
     */
    @RequiresPermissions("mkt:cms:agreement:edit")
    @PutMapping("/{draftId}")
    public ApiResponse<AgreementDraftResponse> update(@PathVariable("draftId") String draftId,
                                                      @RequestBody @Valid UpdateAgreementDraftRequest request) {
        return ApiResponse.ok(AgreementWebAssembler.toVo(agreementDraftAppService.update(
                AgreementWebAssembler.toCmd(draftId, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 查询草稿
     *
     * @param draftId 草稿标识
     * @return 草稿
     */
    @RequiresPermissions("mkt:cms:agreement:query")
    @GetMapping("/{draftId}")
    public ApiResponse<AgreementDraftResponse> get(@PathVariable("draftId") String draftId) {
        return ApiResponse.ok(AgreementWebAssembler.toVo(agreementDraftAppService.get(draftId)));
    }

    /**
     * 定稿：对指定乐观锁版本生成不可变修订（重复定稿幂等）
     *
     * @param draftId 草稿标识
     * @param request 定稿请求
     * @return 不可变修订
     */
    @RequiresPermissions("mkt:cms:agreement:freeze")
    @PostMapping("/{draftId}/action/freeze")
    public ApiResponse<AgreementRevisionResponse> freeze(@PathVariable("draftId") String draftId,
                                                         @RequestBody @Valid FreezeAgreementDraftRequest request) {
        FreezeAgreementDraftCmd cmd = FreezeAgreementDraftCmd.builder()
                .draftId(draftId)
                .lockVersion(request.getLockVersion())
                .operator(OperatorResolver.currentOperator())
                .build();
        return ApiResponse.ok(AgreementWebAssembler.toVo(agreementDraftAppService.freeze(cmd)));
    }
}
