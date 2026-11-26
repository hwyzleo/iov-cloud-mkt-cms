package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;

import java.time.Instant;

/**
 * 探索页文章新建/修改命令。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExploreArticleCmd {

    private final String articleId;

    private final String sourceType;

    private final String title;

    private final String summary;

    private final ExploreImage cover;

    /**
     * 多图列表（最多 9 张）；为空时兼容使用 cover
     */
    private final java.util.List<ExploreImage> images;

    private final String authorName;

    private final String authorAvatarUrl;

    private final Instant displayAt;

    private final String body;

    private final String sourceContentId;

    private final String nativeRouteCode;

    private final Integer lockVersion;

    private final String operator;
}
