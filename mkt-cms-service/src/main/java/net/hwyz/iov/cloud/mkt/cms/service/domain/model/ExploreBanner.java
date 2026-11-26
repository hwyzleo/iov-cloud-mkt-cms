package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;

import java.time.Instant;

/**
 * 探索页 Banner 根实体。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreBanner {

    private Long id;

    private String bannerId;

    private ExploreContentStatus status;

    private String currentRevisionId;

    private int lockVersion;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;

    /**
     * 新建 Banner 根实体
     *
     * @param bannerId Banner 标识
     * @param operator 操作人
     * @param now      服务端时间
     * @return 根实体
     */
    public static ExploreBanner create(String bannerId, String operator, Instant now) {
        return ExploreBanner.builder()
                .bannerId(bannerId)
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
    public ExploreBanner published(String revisionId, String operator, Instant now) {
        return toBuilder()
                .status(ExploreContentStatus.PUBLISHED)
                .currentRevisionId(revisionId)
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 立即下线
     *
     * @param operator 操作人
     * @param now      服务端时间
     * @return 更新后的根实体
     */
    public ExploreBanner offline(String operator, Instant now) {
        return toBuilder()
                .status(ExploreContentStatus.OFFLINE)
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 编辑后递增乐观锁
     *
     * @param operator 操作人
     * @param now      服务端时间
     * @return 更新后的根实体
     */
    public ExploreBanner touched(String operator, Instant now) {
        return toBuilder()
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }
}
