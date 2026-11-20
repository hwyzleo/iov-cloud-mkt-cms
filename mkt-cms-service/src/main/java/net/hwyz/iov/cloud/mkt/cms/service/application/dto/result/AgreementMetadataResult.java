package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 协议管理元数据（供管理端渲染下拉与提示，避免前端硬编码范围值）。
 * <p>
 * 多语言扩展路径：后端受支持清单新增语言/市场/品牌/车系后，前端下拉自动生效，
 * 无需改动前端代码；协议内容语言与界面语言互相独立。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementMetadataResult {

    /**
     * 协议类型
     */
    private List<String> agreementTypes;

    /**
     * 场景
     */
    private List<String> scenes;

    /**
     * 受支持市场
     */
    private List<String> markets;

    /**
     * 受支持内容语言
     */
    private List<String> locales;

    /**
     * 受支持品牌
     */
    private List<String> brands;

    /**
     * 受支持车系
     */
    private List<String> carLines;

    /**
     * 品牌 → 车系归属
     */
    private Map<String, List<String>> carLinesByBrand;

    /**
     * 正文格式
     */
    private String contentType;

    /**
     * 清洗策略版本
     */
    private String sanitizerVersion;

    /**
     * 正文输入上限（字节）
     */
    private int maxInputBytes;

    /**
     * 正文输出上限（字节）
     */
    private int maxOutputBytes;
}
