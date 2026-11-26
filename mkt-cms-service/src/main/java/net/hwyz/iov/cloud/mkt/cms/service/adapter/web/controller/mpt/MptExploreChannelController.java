package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.ExploreWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.support.OperatorResolver;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreChannelUpdateRequest;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreChannelAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 探索页频道管理。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/explore/v1/channels")
public class MptExploreChannelController {

    private final ExploreChannelAppService channelAppService;

    /**
     * 查询全部频道
     *
     * @return 频道列表
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(channelAppService.list().stream()
                .map(ExploreWebAssembler::toChannelView)
                .collect(Collectors.toList()));
    }

    /**
     * 维护频道名称、排序与启停状态（编码不可改）
     *
     * @param channelCode 频道编码
     * @param request     维护请求
     * @return 更新后的频道
     */
    @RequiresPermissions("mkt:cms:explore:edit")
    @PutMapping("/{channelCode}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("channelCode") String channelCode,
                                                   @RequestBody @Valid ExploreChannelUpdateRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toChannelView(channelAppService.update(
                ExploreWebAssembler.toCmd(channelCode, request, OperatorResolver.currentOperator()))));
    }
}
