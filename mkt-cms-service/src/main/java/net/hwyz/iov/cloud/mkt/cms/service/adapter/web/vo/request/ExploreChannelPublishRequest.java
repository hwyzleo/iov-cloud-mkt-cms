package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 探索页频道独立发布请求（MKT-CMS-DSN-CR-005 §4.2）。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreChannelPublishRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    /**
     * 发布前提：期望的当前修订标识；首次发布显式为空
     */
    private String expectedCurrentRevisionId;

    @NotNull(message = "lockVersion 不能为空")
    @Min(value = 0, message = "lockVersion 不得为负")
    private Integer lockVersion;
}
