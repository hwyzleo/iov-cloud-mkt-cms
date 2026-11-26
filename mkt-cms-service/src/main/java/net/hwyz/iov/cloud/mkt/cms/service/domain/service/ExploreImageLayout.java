package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

/**
 * 探索页文章多图布局建议。
 * <p>
 * MKT-CMS-DSN-CR-003 v0.4：上传最多 9 张；客户端按数量选择最合适布局，
 * 超过 9 张时由客户端在第九格显示总数（CMS 仍返回 imageCount 供展示）。
 *
 * @author hwyz_leo
 */
public final class ExploreImageLayout {

    public static final String SINGLE = "SINGLE";

    public static final String TWO_COLUMNS = "TWO_COLUMNS";

    public static final String ONE_LARGE_TWO_SMALL = "ONE_LARGE_TWO_SMALL";

    public static final String TWO_BY_TWO = "TWO_BY_TWO";

    public static final String THREE_COLUMNS = "THREE_COLUMNS";

    public static final String NINE_GRID_WITH_COUNT = "NINE_GRID_WITH_COUNT";

    private ExploreImageLayout() {
    }

    /**
     * 按图片数量给出展示布局
     *
     * @param count 图片数量
     * @return 布局标识
     */
    public static String of(int count) {
        if (count <= 1) {
            return SINGLE;
        }
        return switch (count) {
            case 2 -> TWO_COLUMNS;
            case 3 -> ONE_LARGE_TWO_SMALL;
            case 4 -> TWO_BY_TWO;
            case 5, 6, 7, 8, 9 -> THREE_COLUMNS;
            default -> NINE_GRID_WITH_COUNT;
        };
    }
}
