package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;

/**
 * 协议适用范围值对象（五维：场景、市场、语言、品牌、车系）。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1：值一律由唯一的服务端 Scope 解析器补默认并校验，
 * 入库、发布、读取、幂等哈希与唯一约束使用同一规范化范围；本对象只承载已规范化的非空值。
 *
 * @author hwyz_leo
 */
@Getter
@EqualsAndHashCode
public final class AgreementScope {

    /**
     * 未细分市场
     */
    public static final String DEFAULT_MARKET = "DEFAULT";

    /**
     * 首批语言：简体中文
     */
    public static final String DEFAULT_LOCALE = "zh-CN";

    /**
     * 未细分品牌
     */
    public static final String DEFAULT_BRAND = "DEFAULT";

    /**
     * 未细分车系
     */
    public static final String DEFAULT_CAR_LINE = "DEFAULT";

    private static final String KEY_SEPARATOR = "|";

    private final AgreementScene scene;

    private final String market;

    private final String locale;

    private final String brandCode;

    private final String carLineCode;

    private AgreementScope(AgreementScene scene, String market, String locale,
                           String brandCode, String carLineCode) {
        this.scene = scene;
        this.market = market;
        this.locale = locale;
        this.brandCode = brandCode;
        this.carLineCode = carLineCode;
    }

    /**
     * 构建已规范化的适用范围
     *
     * @param scene       场景
     * @param market      市场
     * @param locale      语言
     * @param brandCode   品牌
     * @param carLineCode 车系
     * @return 适用范围值对象
     */
    public static AgreementScope of(AgreementScene scene, String market, String locale,
                                    String brandCode, String carLineCode) {
        if (scene == null || market == null || locale == null || brandCode == null || carLineCode == null) {
            throw new IllegalArgumentException("协议适用范围不允许空维度");
        }
        return new AgreementScope(scene, market, locale, brandCode, carLineCode);
    }

    /**
     * 稳定性标识（用于派生协议身份、哈希与比较）
     *
     * @return 五维拼接标识
     */
    public String identityKey() {
        return scene.name() + KEY_SEPARATOR + market + KEY_SEPARATOR + locale
                + KEY_SEPARATOR + brandCode + KEY_SEPARATOR + carLineCode;
    }

    @Override
    public String toString() {
        return identityKey();
    }
}
