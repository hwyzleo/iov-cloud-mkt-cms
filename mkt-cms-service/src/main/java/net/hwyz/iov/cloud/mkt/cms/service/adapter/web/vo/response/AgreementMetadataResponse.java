package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 协议管理元数据（管理后台下拉与提示）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class AgreementMetadataResponse {

    private List<String> agreementTypes;

    private List<String> scenes;

    private List<String> markets;

    private List<String> locales;

    private List<String> brands;

    private List<String> carLines;

    private Map<String, List<String>> carLinesByBrand;

    private String contentType;

    private String sanitizerVersion;

    private int maxInputBytes;

    private int maxOutputBytes;
}
