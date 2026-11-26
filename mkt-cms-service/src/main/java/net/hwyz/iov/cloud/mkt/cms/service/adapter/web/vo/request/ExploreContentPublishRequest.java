package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import lombok.Data;

/**
 * 探索页内容发布请求（不传 revisionId 时发布最新草稿修订）。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreContentPublishRequest {

    private String revisionId;
}
