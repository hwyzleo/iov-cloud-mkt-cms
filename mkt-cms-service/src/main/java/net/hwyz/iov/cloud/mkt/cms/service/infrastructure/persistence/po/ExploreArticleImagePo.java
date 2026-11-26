package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 探索页文章修订图片持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_article_image")
public class ExploreArticleImagePo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("article_revision_id")
    private String articleRevisionId;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("url")
    private String url;

    @TableField("width")
    private Integer width;

    @TableField("height")
    private Integer height;

    @TableField("mime")
    private String mime;

    @TableField("size")
    private Long size;
}
