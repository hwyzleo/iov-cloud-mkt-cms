package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 探索页预置频道编码。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.1 / AC-029：首版通过迁移预置 RECOMMEND / ACTIVITY / LOCAL；
 * 编码稳定，名称、排序与状态可维护，被引用后不得修改或删除。
 *
 * @author hwyz_leo
 */
public final class ExploreChannelCodes {

    /**
     * 推荐频道
     */
    public static final String RECOMMEND = "RECOMMEND";

    /**
     * 活动频道
     */
    public static final String ACTIVITY = "ACTIVITY";

    /**
     * 此地频道
     */
    public static final String LOCAL = "LOCAL";

    private static final Set<String> PRESET;

    static {
        Set<String> codes = new LinkedHashSet<>();
        Collections.addAll(codes, RECOMMEND, ACTIVITY, LOCAL);
        PRESET = Collections.unmodifiableSet(codes);
    }

    private ExploreChannelCodes() {
    }

    /**
     * 预置频道编码集合
     *
     * @return 不可变编码集合
     */
    public static Set<String> preset() {
        return PRESET;
    }
}
