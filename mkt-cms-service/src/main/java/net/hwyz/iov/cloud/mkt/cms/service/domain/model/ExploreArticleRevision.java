package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;

import java.time.Instant;

/**
 * 探索页文章修订（不可变快照）。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.1 / §4.3：官方修订保存安全 HTML 与摘要；
 * UGC 修订保存来源内容标识、作者显示快照与原生路由，不保存互动数据。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreArticleRevision {

    private Long id;

    private String revisionId;

    private String articleId;

    private int revisionNo;

    private ExploreRevisionStatus status;

    private String title;

    private String summary;

    private ExploreImage cover;

    /**
     * 多图列表（最多 9 张，按展示顺序）
     */
    private java.util.List<ExploreImage> images;

    private String authorName;

    private String authorAvatarUrl;

    private Instant displayAt;

    private String contentType;

    private String body;

    private String contentHash;

    private String sanitizerVersion;

    private String sourceContentId;

    private String nativeRouteCode;

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
    public ExploreArticleRevision published(String operator, Instant now) {
        return toBuilder()
                .status(ExploreRevisionStatus.PUBLISHED)
                .publishedAt(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }
}
