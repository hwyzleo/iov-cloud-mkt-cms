package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.Logical;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreAssetUploadRequest;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreAssetAppService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 探索页素材上传（CMS 作为调用方复用 EDD-OSS）。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/explore/v1/assets")
public class MptExploreAssetController {

    private final ExploreAssetAppService assetAppService;

    /**
     * 申请素材上传（返回 COS 预签名 PUT 地址）
     *
     * @param request 申请请求
     * @return 上传参数
     */
    @RequiresPermissions(value = {"mkt:cms:explore:add", "mkt:cms:explore:edit"}, logical = Logical.OR)
    @PostMapping("/uploads")
    public ApiResponse<Map<String, Object>> generate(@RequestBody @Valid ExploreAssetUploadRequest request) {
        return ApiResponse.ok(assetAppService.generate(request.getOriginalFilename(),
                request.getContentType(), request.getSize()));
    }

    /**
     * 完成素材上传确认
     *
     * @param objectId 上传任务标识
     * @return 已确认素材信息
     */
    @RequiresPermissions(value = {"mkt:cms:explore:add", "mkt:cms:explore:edit"}, logical = Logical.OR)
    @PostMapping("/uploads/{objectId}/complete")
    public ApiResponse<Map<String, Object>> complete(@PathVariable("objectId") String objectId) {
        return ApiResponse.ok(assetAppService.complete(objectId));
    }
}
