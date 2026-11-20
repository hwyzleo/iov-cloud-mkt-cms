package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementSearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * 协议草稿仓储（域层接口，基础设施层实现）。
 *
 * @author hwyz_leo
 */
public interface AgreementDraftRepository {

    /**
     * 新增草稿
     *
     * @param draft 草稿
     * @return 落库后的草稿
     */
    AgreementDraft insert(AgreementDraft draft);

    /**
     * 按乐观锁版本更新草稿内容（范围与类型不可变）
     *
     * @param draft               新草稿内容（lockVersion 为递增后的值）
     * @param expectedLockVersion 期望的原乐观锁版本
     * @return 影响行数，0 表示版本已被并发修改
     */
    int updateContent(AgreementDraft draft, int expectedLockVersion);

    /**
     * 按业务标识查询草稿
     *
     * @param draftId 草稿标识
     * @return 草稿
     */
    Optional<AgreementDraft> findByDraftId(String draftId);

    /**
     * 条件检索草稿（列表不返回正文；分页由调用方通过 PageHelper 在数据库阶段介入）
     *
     * @param criteria 检索条件
     * @return 草稿列表（不含正文）
     */
    List<AgreementDraft> search(AgreementSearchCriteria criteria);
}
