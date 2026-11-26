package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreMetadataAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 探索页管理元数据（组件、来源/目标类型、白名单与约束）。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/explore/v1")
public class MptExploreMetadataController {

    private final ExploreMetadataAppService metadataAppService;

    /**
     * 元数据
     *
     * @return 元数据
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/explore-metadata")
    public ApiResponse<Map<String, Object>> metadata() {
        return ApiResponse.ok(metadataAppService.metadata());
    }
}
