package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 探索页信息流不透明游标内容（服务端解析，客户端不得依赖格式）。
 * <p>
 * MKT-CMS-DSN-CR-003 §6.2 / D-014：排序键固定为 {@code sortNo + itemId}，
 * 游标必须携带 releaseId、componentId、sortNo 与 itemId，禁止 OFFSET 或内存分页。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public class ExploreCursor {

    private final String releaseId;

    private final String componentId;

    private final int sortNo;

    private final String itemId;
}
