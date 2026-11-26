package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 探索页频道草稿保存命令（整体替换组件与条目）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExplorePageSaveCmd {

    private final String channelCode;

    private final Integer lockVersion;

    private final List<Component> components;

    private final String operator;

    /**
     * 草稿组件
     */
    @Getter
    @Builder
    public static class Component {

        private final String type;

        private final Integer sortNo;

        private final Map<String, Object> config;

        private final List<Item> items;
    }

    /**
     * 草稿条目
     */
    @Getter
    @Builder
    public static class Item {

        private final String itemType;

        private final String refId;

        private final String refRevisionId;

        private final Integer sortNo;

        private final java.time.Instant startAt;

        private final java.time.Instant endAt;

        private final net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget target;
    }
}
