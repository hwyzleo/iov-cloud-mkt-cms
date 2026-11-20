package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementRevisionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 协议修订 Mapper（只增只读，不提供更新与删除语句）。
 *
 * @author hwyz_leo
 */
@Mapper
public interface AgreementRevisionMapper {

    /**
     * 新增不可变修订
     *
     * @param po 修订
     * @return 影响行数
     */
    int insertPo(AgreementRevisionPo po);

    /**
     * 按修订标识查询
     *
     * @param revisionId 修订标识
     * @return 修订
     */
    AgreementRevisionPo selectByRevisionId(@Param("revisionId") String revisionId);

    /**
     * 按来源草稿与草稿版本查询
     *
     * @param sourceDraftId      来源草稿标识
     * @param sourceDraftVersion 来源草稿版本
     * @return 修订
     */
    AgreementRevisionPo selectBySourceDraft(@Param("sourceDraftId") String sourceDraftId,
                                            @Param("sourceDraftVersion") int sourceDraftVersion);

    /**
     * 查询已被至少一个已提交发布包引用的修订
     *
     * @param revisionId 修订标识
     * @return 修订
     */
    AgreementRevisionPo selectPublishedByRevisionId(@Param("revisionId") String revisionId);
}
