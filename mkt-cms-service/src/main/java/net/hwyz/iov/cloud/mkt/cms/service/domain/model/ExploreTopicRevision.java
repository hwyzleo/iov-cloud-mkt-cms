package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;

import java.time.Instant;
import java.util.List;

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

    /**
     * 有序图集（MKT-CMS-DSN-CR-004 §5.1：最多 9 张，cover 为 images[0]）
     */
    private List<ExploreImage> images;

    /**
     * 跳转目标；为空表示专题详情模式（打开本专题详情页）
     */
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
