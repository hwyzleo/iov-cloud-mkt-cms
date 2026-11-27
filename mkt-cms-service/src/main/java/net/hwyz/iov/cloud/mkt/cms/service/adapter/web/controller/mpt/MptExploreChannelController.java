package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.ExploreWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.support.OperatorResolver;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreChannelCreateRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreChannelPublishRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreChannelUpdateRequest;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreChannelAppService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 探索页频道管理（MKT-CMS-DSN-CR-005 §6.1 / D-019）。
 * <p>
 * 频道拥有独立草稿与当前发布修订；新增、修改、删除与发布互不影响页面版本。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/explore/v1/channels")
public class MptExploreChannelController {

    private final ExploreChannelAppService channelAppService;

    /**
     * 查询全部频道（草稿/当前发布/未发布变更/可删除性）
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
     * 新增频道（编码创建后不可变）
     *
     * @param request 新增请求
     * @return 频道视图
     */
    @RequiresPermissions("mkt:cms:explore:add")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody @Valid ExploreChannelCreateRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toChannelView(channelAppService.create(
                ExploreWebAssembler.toCreateCmd(request, OperatorResolver.currentOperator()))));
    }

    /**
     * 保存频道草稿（不再直接修改线上配置）
     *
     * @param channelCode 频道编码
     * @param request     维护请求
     * @return 频道视图
     */
    @RequiresPermissions("mkt:cms:explore:edit")
    @PutMapping("/{channelCode}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("channelCode") String channelCode,
                                                   @RequestBody @Valid ExploreChannelUpdateRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toChannelView(channelAppService.update(
                ExploreWebAssembler.toCmd(channelCode, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 删除未被任何页面草稿或发布版本引用的频道
     *
     * @param channelCode 频道编码
     * @param lockVersion 期望乐观锁版本
     * @return 空响应
     */
    @RequiresPermissions("mkt:cms:explore:edit")
    @DeleteMapping("/{channelCode}")
    public ApiResponse<Void> delete(@PathVariable("channelCode") String channelCode,
                                    @RequestParam("lockVersion") Integer lockVersion) {
        channelAppService.delete(channelCode, lockVersion, OperatorResolver.currentOperator());
        return ApiResponse.ok(null);
    }

    /**
     * 独立发布当前草稿（幂等、乐观锁与发布前提校验）
     *
     * @param channelCode 频道编码
     * @param request     发布请求
     * @return 频道视图
     */
    @RequiresPermissions("mkt:cms:explore:publish")
    @PostMapping("/{channelCode}/action/publish")
    public ApiResponse<Map<String, Object>> publish(@PathVariable("channelCode") String channelCode,
                                                    @RequestBody @Valid ExploreChannelPublishRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toChannelView(channelAppService.publish(
                ExploreWebAssembler.toChannelPublishCmd(channelCode, request,
                        OperatorResolver.currentOperator()))));
    }
}
