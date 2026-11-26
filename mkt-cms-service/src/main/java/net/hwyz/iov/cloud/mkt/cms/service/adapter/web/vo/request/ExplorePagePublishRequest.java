package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

/**
 * 探索页频道发布请求。
 *
 * @author hwyz_leo
 */
@Data
public class ExplorePagePublishRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    /**
     * 发布前提：读取到的当前版本；首次发布显式传 null
     */
    private String expectedReleaseId;

    @NotBlank(message = "发布类型不能为空")
    private String publishType;

    private Instant effectiveAt;

    private Instant expireAt;
}
