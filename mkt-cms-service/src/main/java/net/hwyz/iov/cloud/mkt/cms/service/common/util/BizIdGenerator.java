package net.hwyz.iov.cloud.mkt.cms.service.common.util;

import cn.hutool.core.util.IdUtil;

/**
 * 协议子域业务标识生成器。
 * <p>
 * 服务器侧生成草稿/修订/发布包/操作标识，不采用客户端提交的标识作为权威标识。
 *
 * @author hwyz_leo
 */
public final class BizIdGenerator {

    private static final int RANDOM_LENGTH = 20;

    private BizIdGenerator() {
    }

    /**
     * 草稿标识
     */
    public static String draftId() {
        return "DRF" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 修订标识
     */
    public static String revisionId() {
        return "REV" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 发布包标识
     */
    public static String bundleId() {
        return "BDL" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 操作标识
     */
    public static String operationId() {
        return "OPS" + IdUtil.nanoId(RANDOM_LENGTH);
    }
}
