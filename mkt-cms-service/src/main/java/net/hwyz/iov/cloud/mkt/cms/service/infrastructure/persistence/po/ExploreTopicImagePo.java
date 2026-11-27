package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 探索页专题修订图片持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_topic_image")
public class ExploreTopicImagePo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("topic_revision_id")
    private String topicRevisionId;

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

    @TableField("show_in_detail")
    private Boolean showInDetail;
}
