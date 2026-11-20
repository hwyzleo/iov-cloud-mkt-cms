package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 当前发布包一致性读取结果持久化对象。
 * <p>
 * 一次 JOIN 读取“当前指针 + 发布包 + 两份修订”，列名以 p_（隐私政策）/ u_（用户协议）前缀区分，
 * 避免两次分别查询造成版本混搭（MKT-CMS-DSN-CR-001 §6.1）。
 * 时间列在 Mapper XML 的 resultMap 中显式声明 UTC TypeHandler。
 *
 * @author hwyz_leo
 */
@Data
public class CurrentBundleViewPo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String bundleId;

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private Instant publishedAt;

    private String pRevisionId;

    private String pAgreementCode;

    private String pAgreementType;

    private String pTitle;

    private String pVersion;

    private LocalDate pAgreementDate;

    private String pContentType;

    private String pBody;

    private String pContentHash;

    private String pSanitizerVersion;

    private String pLocale;

    private String pScene;

    private String pMarket;

    private String pBrandCode;

    private String pCarLineCode;

    private String uRevisionId;

    private String uAgreementCode;

    private String uAgreementType;

    private String uTitle;

    private String uVersion;

    private LocalDate uAgreementDate;

    private String uContentType;

    private String uBody;

    private String uContentHash;

    private String uSanitizerVersion;

    private String uLocale;

    private String uScene;

    private String uMarket;

    private String uBrandCode;

    private String uCarLineCode;
}
