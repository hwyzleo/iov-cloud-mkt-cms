package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreSourceType;

import java.time.Instant;

/**
 * 探索页文章根实体。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.1：状态与当前修订共同控制可见性；下线是手机端实时过滤条件。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreArticle {

    private Long id;

    private String articleId;

    private ExploreSourceType sourceType;

    private ExploreContentStatus status;

    private String currentRevisionId;

    private int lockVersion;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;

    /**
     * 新建文章根实体（尚无已发布修订）
     *
     * @param articleId  文章标识
     * @param sourceType 来源类型
     * @param operator   操作人
     * @param now        服务端时间
     * @return 根实体
     */
    public static ExploreArticle create(String articleId, ExploreSourceType sourceType,
                                        String operator, Instant now) {
        return ExploreArticle.builder()
                .articleId(articleId)
                .sourceType(sourceType)
                .status(ExploreContentStatus.PUBLISHED)
                .lockVersion(0)
                .createBy(operator)
                .createTime(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 发布新修订
     *
     * @param revisionId 新当前修订
     * @param operator   操作人
     * @param now        服务端时间
     * @return 更新后的根实体
     */
    public ExploreArticle published(String revisionId, String operator, Instant now) {
        return toBuilder()
                .status(ExploreContentStatus.PUBLISHED)
                .currentRevisionId(revisionId)
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 立即下线（保留历史）
     *
     * @param operator 操作人
     * @param now      服务端时间
     * @return 更新后的根实体
     */
    public ExploreArticle offline(String operator, Instant now) {
        return toBuilder()
                .status(ExploreContentStatus.OFFLINE)
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 编辑后仅递增乐观锁（不改变当前修订与状态）
     *
     * @param operator 操作人
     * @param now      服务端时间
     * @return 更新后的根实体
     */
    public ExploreArticle touched(String operator, Instant now) {
        return toBuilder()
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }
}
