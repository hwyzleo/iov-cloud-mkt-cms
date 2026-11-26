package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;

import java.time.Instant;

/**
 * 探索页频道。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreChannel {

    private Long id;

    private String channelCode;

    private String name;

    private int sortNo;

    private ExploreChannelStatus status;

    private int lockVersion;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;

    /**
     * 修改频道名称、排序与启停状态（编码不可改）
     *
     * @param name     名称
     * @param sortNo   排序
     * @param status   状态
     * @param operator 操作人
     * @param now      服务端时间
     * @return 修改后的频道
     */
    public ExploreChannel updated(String name, int sortNo, ExploreChannelStatus status,
                                  String operator, Instant now) {
        return toBuilder()
                .name(name)
                .sortNo(sortNo)
                .status(status)
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }
}
