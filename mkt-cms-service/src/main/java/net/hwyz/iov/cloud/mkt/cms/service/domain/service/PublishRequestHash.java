package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.util.HexSha256;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;

/**
 * 发布命令幂等哈希规则。
 * <p>
 * MKT-CMS-DSN-CR-001 §5.1：对规范化后的业务命令计算 requestHash（不含 requestId 与服务端字段），
 * 五维 Scope 共同参与哈希；显式默认与省略默认必须生成相同哈希，
 * 因此哈希只接受已规范化的 {@link AgreementScope}。
 *
 * @author hwyz_leo
 */
public final class PublishRequestHash {

    private static final String VERSION = "agreement-publish/v1";

    private static final String EMPTY_BUNDLE = "NONE";

    private static final String SEPARATOR = "|";

    private PublishRequestHash() {
    }

    /**
     * 计算发布命令哈希
     *
     * @param scope             规范化适用范围
     * @param privacyRevisionId 隐私政策修订标识
     * @param userRevisionId    用户协议修订标识
     * @param expectedBundleId  期望的当前包标识（首次发布为 null）
     * @return 64 位小写十六进制哈希
     */
    public static String of(AgreementScope scope, String privacyRevisionId,
                            String userRevisionId, String expectedBundleId) {
        String canonical = VERSION
                + SEPARATOR + "scene=" + scope.getScene().name()
                + SEPARATOR + "market=" + scope.getMarket()
                + SEPARATOR + "locale=" + scope.getLocale()
                + SEPARATOR + "brand=" + scope.getBrandCode()
                + SEPARATOR + "carLine=" + scope.getCarLineCode()
                + SEPARATOR + "privacy=" + privacyRevisionId
                + SEPARATOR + "user=" + userRevisionId
                + SEPARATOR + "expected=" + (expectedBundleId == null ? EMPTY_BUNDLE : expectedBundleId);
        return HexSha256.ofUtf8(canonical);
    }
}
