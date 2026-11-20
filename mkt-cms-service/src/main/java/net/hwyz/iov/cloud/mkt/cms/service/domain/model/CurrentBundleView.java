package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 当前发布包视图（一次一致性读取结果）。
 * <p>
 * MKT-CMS-DSN-CR-001 §6.1：连接当前指针、发布包与两份修订一次读出，
 * 不允许两次分别查询“最新协议”，也不返回草稿或无发布包引用的修订。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentBundleView {

    private String bundleId;

    private AgreementScope scope;

    private Instant publishedAt;

    private AgreementRevision privacyPolicy;

    private AgreementRevision userAgreement;
}
