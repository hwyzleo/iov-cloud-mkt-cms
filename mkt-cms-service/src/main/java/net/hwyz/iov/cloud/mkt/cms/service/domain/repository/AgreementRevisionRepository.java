package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;

import java.util.Optional;

/**
 * 协议修订仓储（域层接口，基础设施层实现）。
 * <p>
 * 修订插入后不可修改或删除：本接口只提供新增与读取。
 *
 * @author hwyz_leo
 */
public interface AgreementRevisionRepository {

    /**
     * 新增不可变修订
     *
     * @param revision 修订
     * @return 落库后的修订
     */
    AgreementRevision insert(AgreementRevision revision);

    /**
     * 按修订标识查询（管理侧：可查看未发布修订）
     *
     * @param revisionId 修订标识
     * @return 修订
     */
    Optional<AgreementRevision> findByRevisionId(String revisionId);

    /**
     * 按来源草稿与草稿乐观锁版本查询（定稿幂等）
     *
     * @param sourceDraftId      来源草稿标识
     * @param sourceDraftVersion 来源草稿乐观锁版本
     * @return 已生成的修订
     */
    Optional<AgreementRevision> findBySourceDraft(String sourceDraftId, int sourceDraftVersion);

    /**
     * 按修订标识查询“已被至少一个已提交发布包引用”的修订（业务读取）
     *
     * @param revisionId 修订标识
     * @return 修订
     */
    Optional<AgreementRevision> findPublishedByRevisionId(String revisionId);
}
