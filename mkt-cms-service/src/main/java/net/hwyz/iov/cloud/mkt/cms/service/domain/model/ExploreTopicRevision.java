package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;

import java.time.Instant;

/**
 * 探索页专题修订（不可变快照）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreTopicRevision {

    private Long id;

    private String revisionId;

    private String topicId;

    private int revisionNo;

    private ExploreRevisionStatus status;

    private String name;

    private String summary;

    private ExploreImage cover;

    private ExploreTarget target;

    private String contentHash;

    private Instant publishedAt;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;

    /**
     * 发布该修订
     *
     * @param operator 操作人
     * @param now      服务端时间
     * @return 已发布修订
     */
    public ExploreTopicRevision published(String operator, Instant now) {
        return toBuilder()
                .status(ExploreRevisionStatus.PUBLISHED)
                .publishedAt(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }
}
