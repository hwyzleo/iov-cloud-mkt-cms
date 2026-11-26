package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.hwyz.iov.cloud.framework.mysql.typehandler.UtcInstantTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.time.Instant;

/**
 * 探索页文章修订持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_article_revision")
public class ExploreArticleRevisionPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("revision_id")
    private String revisionId;

    @TableField("article_id")
    private String articleId;

    @TableField("revision_no")
    private Integer revisionNo;

    @TableField("status")
    private String status;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("cover_width")
    private Integer coverWidth;

    @TableField("cover_height")
    private Integer coverHeight;

    @TableField("cover_mime")
    private String coverMime;

    @TableField("cover_size")
    private Long coverSize;

    @TableField("author_name")
    private String authorName;

    @TableField("author_avatar_url")
    private String authorAvatarUrl;

    @TableField(value = "display_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant displayAt;

    @TableField("content_type")
    private String contentType;

    @TableField("body")
    private String body;

    @TableField("content_hash")
    private String contentHash;

    @TableField("sanitizer_version")
    private String sanitizerVersion;

    @TableField("source_content_id")
    private String sourceContentId;

    @TableField("native_route_code")
    private String nativeRouteCode;

    @TableField(value = "published_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant publishedAt;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "create_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant createTime;

    @TableField("modify_by")
    private String modifyBy;

    @TableField(value = "modify_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant modifyTime;
}
