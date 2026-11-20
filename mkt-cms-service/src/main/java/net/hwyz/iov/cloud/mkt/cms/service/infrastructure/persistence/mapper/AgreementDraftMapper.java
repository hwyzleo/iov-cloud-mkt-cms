package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementDraftPo;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementSearchCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 协议草稿 Mapper。
 *
 * @author hwyz_leo
 */
@Mapper
public interface AgreementDraftMapper {

    /**
     * 新增草稿
     *
     * @param po 草稿
     * @return 影响行数
     */
    int insertPo(AgreementDraftPo po);

    /**
     * 按乐观锁版本更新草稿内容
     *
     * @param po                草稿（lockVersion 为递增后的值）
     * @param expectedLockVersion 期望的原乐观锁版本
     * @return 影响行数
     */
    int updateContentWithLock(@Param("po") AgreementDraftPo po, @Param("expectedLockVersion") int expectedLockVersion);

    /**
     * 按业务标识查询草稿
     *
     * @param draftId 草稿标识
     * @return 草稿
     */
    AgreementDraftPo selectByDraftId(@Param("draftId") String draftId);

    /**
     * 条件检索草稿（不返回正文，分页由 PageHelper 在数据库阶段介入）
     *
     * @param criteria 检索条件
     * @return 草稿列表
     */
    List<AgreementDraftPo> selectPoByCriteria(@Param("criteria") AgreementSearchCriteria criteria);
}
