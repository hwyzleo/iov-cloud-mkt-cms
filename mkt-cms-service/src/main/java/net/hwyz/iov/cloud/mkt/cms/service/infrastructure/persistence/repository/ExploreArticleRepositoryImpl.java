package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExplorePageList;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.ExploreConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreArticleImageMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreArticleMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreArticleRevisionMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreArticleImagePo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreArticlePo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreArticleRevisionPo;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 探索页文章仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ExploreArticleRepositoryImpl implements ExploreArticleRepository {

    private final ExploreArticleMapper articleMapper;
    private final ExploreArticleRevisionMapper revisionMapper;
    private final ExploreArticleImageMapper imageMapper;

    @Override
    public void insert(ExploreArticle article) {
        articleMapper.insert(ExploreConverter.fromDomain(article));
    }

    @Override
    public int update(ExploreArticle article, int expectedLockVersion) {
        UpdateWrapper<ExploreArticlePo> wrapper = new UpdateWrapper<>();
        wrapper.eq("article_id", article.getArticleId())
                .eq("lock_version", expectedLockVersion)
                .set("status", article.getStatus() == null ? null : article.getStatus().name())
                .set("current_revision_id", article.getCurrentRevisionId())
                .set("lock_version", article.getLockVersion())
                .set("modify_by", article.getModifyBy())
                .set("modify_time", article.getModifyTime());
        return articleMapper.update(null, wrapper);
    }

    @Override
    public Optional<ExploreArticle> findByArticleId(String articleId) {
        return Optional.ofNullable(articleMapper.selectOne(new QueryWrapper<ExploreArticlePo>()
                        .eq("article_id", articleId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreArticle> findByArticleIdForUpdate(String articleId) {
        return Optional.ofNullable(articleMapper.selectOne(new QueryWrapper<ExploreArticlePo>()
                        .eq("article_id", articleId)
                        .last("FOR UPDATE")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public List<ExploreArticle> findByIds(Collection<String> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }
        return articleMapper.selectList(new QueryWrapper<ExploreArticlePo>()
                        .in("article_id", articleIds)).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExploreArticle> search(ExploreArticleSearchCriteria criteria) {
        QueryWrapper<ExploreArticlePo> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(criteria.getSourceType())) {
            wrapper.eq("source_type", criteria.getSourceType().trim());
        }
        if (StringUtils.hasText(criteria.getStatus())) {
            wrapper.eq("status", criteria.getStatus().trim());
        }
        if (StringUtils.hasText(criteria.getTitle())) {
            wrapper.apply("EXISTS (SELECT 1 FROM cms_explore_article_revision r"
                            + " WHERE r.article_id = cms_explore_article.article_id"
                            + " AND r.title LIKE CONCAT('%', {0}, '%'))", criteria.getTitle().trim());
        }
        wrapper.orderByDesc("create_time");
        return ExplorePageList.map(articleMapper.selectList(wrapper), ExploreConverter::toDomain);
    }

    @Override
    public void insertRevision(ExploreArticleRevision revision) {
        revisionMapper.insert(ExploreConverter.fromDomain(revision));
    }

    @Override
    public int updateDraftRevision(ExploreArticleRevision revision) {
        ExploreArticleRevisionPo po = ExploreConverter.fromDomain(revision);
        UpdateWrapper<ExploreArticleRevisionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("revision_id", revision.getRevisionId())
                .eq("status", ExploreRevisionStatus.DRAFT.name())
                .set("title", po.getTitle())
                .set("summary", po.getSummary())
                .set("cover_url", po.getCoverUrl())
                .set("cover_width", po.getCoverWidth())
                .set("cover_height", po.getCoverHeight())
                .set("cover_mime", po.getCoverMime())
                .set("cover_size", po.getCoverSize())
                .set("author_name", po.getAuthorName())
                .set("author_avatar_url", po.getAuthorAvatarUrl())
                .set("display_at", po.getDisplayAt())
                .set("content_type", po.getContentType())
                .set("body", po.getBody())
                .set("content_hash", po.getContentHash())
                .set("sanitizer_version", po.getSanitizerVersion())
                .set("source_content_id", po.getSourceContentId())
                .set("native_route_code", po.getNativeRouteCode())
                .set("modify_by", po.getModifyBy())
                .set("modify_time", po.getModifyTime());
        return revisionMapper.update(null, wrapper);
    }

    @Override
    public int publishRevision(ExploreArticleRevision revision) {
        UpdateWrapper<ExploreArticleRevisionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("revision_id", revision.getRevisionId())
                .eq("status", ExploreRevisionStatus.DRAFT.name())
                .set("status", ExploreRevisionStatus.PUBLISHED.name())
                .set("published_at", revision.getPublishedAt())
                .set("modify_by", revision.getModifyBy())
                .set("modify_time", revision.getModifyTime());
        return revisionMapper.update(null, wrapper);
    }

    @Override
    public Optional<ExploreArticleRevision> findRevision(String revisionId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreArticleRevisionPo>()
                        .eq("revision_id", revisionId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreArticleRevision> findLatestDraft(String articleId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreArticleRevisionPo>()
                        .eq("article_id", articleId)
                        .eq("status", ExploreRevisionStatus.DRAFT.name())
                        .orderByDesc("revision_no")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreArticleRevision> findLatestRevision(String articleId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreArticleRevisionPo>()
                        .eq("article_id", articleId)
                        .orderByDesc("revision_no")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public void replaceRevisionImages(String revisionId, List<ExploreImage> images) {
        imageMapper.delete(new QueryWrapper<ExploreArticleImagePo>()
                .eq("article_revision_id", revisionId));
        if (images == null || images.isEmpty()) {
            return;
        }
        int sortNo = 10;
        for (ExploreImage image : images) {
            ExploreArticleImagePo po = new ExploreArticleImagePo();
            po.setArticleRevisionId(revisionId);
            po.setSortNo(sortNo);
            po.setUrl(image.getUrl());
            po.setWidth(image.getWidth());
            po.setHeight(image.getHeight());
            po.setMime(image.getMime());
            po.setSize(image.getSize());
            imageMapper.insert(po);
            sortNo += 10;
        }
    }

    @Override
    public List<ExploreImage> findRevisionImages(String revisionId) {
        return imageMapper.selectList(new QueryWrapper<ExploreArticleImagePo>()
                        .eq("article_revision_id", revisionId)
                        .orderByAsc("sort_no")).stream()
                .map(po -> ExploreImage.builder()
                        .url(po.getUrl())
                        .width(po.getWidth())
                        .height(po.getHeight())
                        .mime(po.getMime())
                        .size(po.getSize())
                        .build())
                .collect(Collectors.toList());
    }
}
