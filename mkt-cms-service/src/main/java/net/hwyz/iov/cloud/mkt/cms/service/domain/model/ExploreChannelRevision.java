package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;

import java.time.Instant;

/**
 * 探索页频道配置修订（MKT-CMS-DSN-CR-005 §3.1 / D-019）。
 * <p>
 * 每个频道最多一个可编辑 DRAFT 修订与一个当前 PUBLISHED 修订；PUBLISHED 后配置不可改删，
 * 手机端只读取根实体 current_revision_id 指向的已发布修订。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreChannelRevision {

    private Long id;

    private String revisionId;

    private String channelCode;

    private int revisionNo;

    private String name;

    private int sortNo;

    private ExploreChannelStatus status;

    private ExploreRevisionStatus revisionStatus;

    private String contentHash;

    private String publishedBy;

    private Instant publishedAt;

    private String requestId;

    private String requestHash;

    private String expectedCurrentRevisionId;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;

    /**
     * 草稿内容更新（保留修订标识、序号与创建信息）
     *
     * @param name        名称
     * @param sortNo      排序
     * @param status      启停状态
     * @param contentHash 内容哈希
     * @param operator    操作人
     * @param now         服务端时间
     * @return 更新后的草稿修订
     */
    public ExploreChannelRevision revised(String name, int sortNo, ExploreChannelStatus status,
                                          String contentHash, String operator, Instant now) {
        return toBuilder()
                .name(name)
                .sortNo(sortNo)
                .status(status)
                .contentHash(contentHash)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 草稿定稿为不可变已发布修订（首次发布写入发布事实，后续不可重写）
     *
     * @param requestId                  幂等请求标识
     * @param requestHash                规范命令哈希
     * @param expectedCurrentRevisionId  发布前提
     * @param operator                   操作人
     * @param now                        服务端时间
     * @return 已发布修订
     */
    public ExploreChannelRevision published(String requestId, String requestHash,
                                            String expectedCurrentRevisionId,
                                            String operator, Instant now) {
        return toBuilder()
                .revisionStatus(ExploreRevisionStatus.PUBLISHED)
                .publishedBy(operator)
                .publishedAt(now)
                .requestId(requestId)
                .requestHash(requestHash)
                .expectedCurrentRevisionId(expectedCurrentRevisionId)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }
}
