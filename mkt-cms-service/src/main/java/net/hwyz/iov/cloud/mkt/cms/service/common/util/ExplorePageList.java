package net.hwyz.iov.cloud.mkt.cms.service.common.util;

import com.github.pagehelper.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页列表映射工具。
 * <p>
 * MKT-CMS-DSN-CR-003 §6.1 / AC-052：列表必须在数据库层完成过滤与分页并返回总数；
 * 逐层映射时保留 PageHelper 的 Page 元数据，避免退化为“当前页条数”。
 *
 * @author hwyz_leo
 */
public final class ExplorePageList {

    private ExplorePageList() {
    }

    /**
     * 映射列表并保留分页元数据
     *
     * @param source 源列表（可能为 Page）
     * @param mapper 映射函数
     * @param <S>    源类型
     * @param <T>    目标类型
     * @return 映射后的列表；源为 Page 时返回同 total 的 Page
     */
    public static <S, T> List<T> map(List<S> source, Function<S, T> mapper) {
        List<T> mapped = source.stream().map(mapper).collect(Collectors.toList());
        if (source instanceof Page<?> page) {
            Page<T> paged = new Page<>(page.getPageNum(), page.getPageSize(), page.isCount());
            paged.setTotal(page.getTotal());
            paged.addAll(mapped);
            return paged;
        }
        return mapped;
    }
}
