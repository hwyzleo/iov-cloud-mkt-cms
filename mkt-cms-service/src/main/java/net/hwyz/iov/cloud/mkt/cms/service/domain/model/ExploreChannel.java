package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;

import java.time.Instant;

/**
 * 探索页频道根实体。
 * <p>
 * MKT-CMS-DSN-CR-005 §3.1 / D-019：根实体只保留稳定编码、双修订指针与乐观锁；
 * 名称、排序与启停由修订承载。旧 name/sort_no/status 列仅用于迁移与回滚兼容，
 * 不作为管理或手机读取的权威配置。
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

    /**
     * 回滚兼容镜像：发布成功时与当前发布修订同步，不参与读取判断
     */
    private String name;

    /**
     * 回滚兼容镜像：发布成功时与当前发布修订同步，不参与读取判断
     */
    private int sortNo;

    /**
     * 回滚兼容镜像：发布成功时与当前发布修订同步，不参与读取判断
     */
    private ExploreChannelStatus status;

    /**
     * 当前唯一可编辑草稿修订；为空表示没有草稿
     */
    private String draftRevisionId;

    /**
     * 当前已发布配置修订；手机端只读取此指针
     */
    private String currentRevisionId;

    private int lockVersion;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;

    /**
     * 绑定/更新可编辑草稿指针（递增根乐观锁）
     *
     * @param draftRevisionId 草稿修订标识
     * @param operator        操作人
     * @param now             服务端时间
     * @return 更新后的根实体
     */
    public ExploreChannel drafted(String draftRevisionId, String operator, Instant now) {
        return toBuilder()
                .draftRevisionId(draftRevisionId)
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 切换当前发布指针并清空草稿；回滚兼容镜像与已发布修订同步
     *
     * @param revisionId 新当前修订标识
     * @param name       已发布名称
     * @param sortNo     已发布排序
     * @param status     已发布启停状态
     * @param operator   操作人
     * @param now        服务端时间
     * @return 更新后的根实体
     */
    public ExploreChannel published(String revisionId, String name, int sortNo,
                                    ExploreChannelStatus status, String operator, Instant now) {
        return toBuilder()
                .currentRevisionId(revisionId)
                .draftRevisionId(null)
                .name(name)
                .sortNo(sortNo)
                .status(status)
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }
}
