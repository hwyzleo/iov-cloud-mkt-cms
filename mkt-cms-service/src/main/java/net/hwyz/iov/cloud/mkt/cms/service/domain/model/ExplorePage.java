package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;

import java.time.Instant;

/**
 * 探索页频道页面版本。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.2 / D-013：版本 0 为唯一草稿（可改，乐观锁）；
 * 版本 &gt; 0 为不可变发布版本（当前指针显式确定）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExplorePage {

    private Long id;

    private String pageId;

    private String channelCode;

    private int pageVersion;

    private ExplorePageStatus status;

    private Instant effectiveAt;

    private Instant expireAt;

    private String requestId;

    private String requestHash;

    private String expectedReleaseId;

    private int lockVersion;

    private String publishedBy;

    private Instant publishedAt;

    private Instant offlineAt;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;

    /**
     * 新建频道草稿（版本 0）
     *
     * @param pageId      页面标识
     * @param channelCode 频道编码
     * @param operator    操作人
     * @param now         服务端时间
     * @return 草稿
     */
    public static ExplorePage draft(String pageId, String channelCode, String operator, Instant now) {
        return ExplorePage.builder()
                .pageId(pageId)
                .channelCode(channelCode)
                .pageVersion(0)
                .status(ExplorePageStatus.DRAFT)
                .lockVersion(0)
                .createBy(operator)
                .createTime(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 保存草稿后递增乐观锁
     *
     * @param operator 操作人
     * @param now      服务端时间
     * @return 更新后的草稿
     */
    public ExplorePage saved(String operator, Instant now) {
        return toBuilder()
                .lockVersion(lockVersion + 1)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 基于草稿创建不可变发布版本
     *
     * @param releaseId         发布版本标识
     * @param pageVersion       版本号（&gt;0）
     * @param status            发布状态
     * @param effectiveAt       生效时间
     * @param expireAt          到期时间
     * @param requestId         幂等请求标识
     * @param requestHash       发布命令哈希
     * @param expectedReleaseId 发布前提
     * @param operator          操作人
     * @param now               服务端时间
     * @return 发布版本
     */
    public static ExplorePage releaseOf(ExplorePage draft, String releaseId, int pageVersion,
                                        ExplorePageStatus status, Instant effectiveAt, Instant expireAt,
                                        String requestId, String requestHash, String expectedReleaseId,
                                        String operator, Instant now) {
        return ExplorePage.builder()
                .pageId(releaseId)
                .channelCode(draft.getChannelCode())
                .pageVersion(pageVersion)
                .status(status)
                .effectiveAt(effectiveAt)
                .expireAt(expireAt)
                .requestId(requestId)
                .requestHash(requestHash)
                .expectedReleaseId(expectedReleaseId)
                .lockVersion(0)
                .publishedBy(operator)
                .publishedAt(now)
                .createBy(operator)
                .createTime(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 切换页面状态
     *
     * @param target   目标状态
     * @param operator 操作人
     * @param now      服务端时间
     * @return 更新后的页面
     */
    public ExplorePage withStatus(ExplorePageStatus target, String operator, Instant now) {
        return toBuilder()
                .status(target)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }

    /**
     * 立即下线
     *
     * @param operator 操作人
     * @param now      服务端时间
     * @return 更新后的页面
     */
    public ExplorePage offline(String operator, Instant now) {
        return toBuilder()
                .status(ExplorePageStatus.OFFLINE)
                .offlineAt(now)
                .modifyBy(operator)
                .modifyTime(now)
                .build();
    }
}
