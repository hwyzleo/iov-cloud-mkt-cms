package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 探索页专题文章条目（人工顺序 + 展示快照）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreTopicItem {

    private Long id;

    private String topicRevisionId;

    private String articleRevisionId;

    private int sortNo;

    private Map<String, Object> displaySnapshot;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;
}
