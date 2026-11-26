package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 探索页素材、H5 与 NATIVE 白名单校验器。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.3 / D-012 / INV-019：图片、H5 origin 与原生路由均由服务端白名单约束，
 * 管理端不得硬编码白名单；白名单为空时拒绝任何外链（安全默认）。
 *
 * @author hwyz_leo
 */
public class ExploreMediaValidator {

    private static final Set<String> ALLOWED_IMAGE_MIME = Set.of("image/jpeg", "image/png", "image/webp");

    private static final Set<String> SENSITIVE_PARAM_KEYS = Set.of(
            "token", "access_token", "authorization", "cookie", "session", "phone", "mobile", "vin", "account");

    private final Set<String> allowedAssetHosts;

    private final Set<String> allowedH5Origins;

    private final Set<String> nativeRouteCodes;

    private final Set<String> nativeParamKeys;

    public ExploreMediaValidator(Set<String> allowedAssetHosts, Set<String> allowedH5Origins,
                                 Set<String> nativeRouteCodes, Set<String> nativeParamKeys) {
        this.allowedAssetHosts = normalize(allowedAssetHosts);
        this.allowedH5Origins = normalize(allowedH5Origins);
        this.nativeRouteCodes = normalize(nativeRouteCodes);
        this.nativeParamKeys = normalize(nativeParamKeys);
    }

    /**
     * 校验必填图片
     *
     * @param image 图片元数据
     * @param field 字段名（用于错误定位）
     */
    public void validateRequiredImage(ExploreImage image, String field) {
        if (image == null || !StringUtils.hasText(image.getUrl())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, field + " 不能为空");
        }
        validateImage(image, field);
    }

    /**
     * 校验可选图片（为空时跳过）
     *
     * @param image 图片元数据
     * @param field 字段名
     */
    public void validateOptionalImage(ExploreImage image, String field) {
        if (image == null || !StringUtils.hasText(image.getUrl())) {
            return;
        }
        validateImage(image, field);
    }

    private void validateImage(ExploreImage image, String field) {
        validateAssetUrl(image.getUrl(), field);
        String mime = image.getMime() == null ? "" : image.getMime().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_MIME.contains(mime)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    field + " 仅允许 JPEG/PNG/WebP");
        }
        if (image.getWidth() == null || image.getWidth() <= 0 || image.getHeight() == null || image.getHeight() <= 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, field + " 必须记录正确的宽高");
        }
        if (image.getSize() == null || image.getSize() <= 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, field + " 必须记录文件大小");
        }
    }

    /**
     * 校验素材 URL：绝对 HTTPS 且 host 命中白名单
     *
     * @param url   URL
     * @param field 字段名
     */
    public void validateAssetUrl(String url, String field) {
        URI uri = parseHttps(url, field);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        String hostWithPort = port == -1 || port == 443 ? host : host + ":" + port;
        if (!allowedAssetHosts.contains(host) && !allowedAssetHosts.contains(hostWithPort)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    field + " 域名不在素材白名单中");
        }
    }

    /**
     * 校验 H5 URL：绝对 HTTPS 且 origin 命中白名单
     *
     * @param url URL
     */
    public void validateH5Url(String url) {
        URI uri = parseHttps(url, "H5 目标");
        String origin = "https://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (uri.getPort() == -1 || uri.getPort() == 443 ? "" : ":" + uri.getPort());
        if (!allowedH5Origins.contains(origin)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "H5 目标 origin 不在白名单中");
        }
    }

    /**
     * 校验 NATIVE 跳转：routeCode 与参数键均命中白名单
     *
     * @param routeCode 路由编码
     * @param params    受控参数
     */
    public void validateNative(String routeCode, Map<String, String> params) {
        validateNativeRouteCode(routeCode);
        if (params == null) {
            return;
        }
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            if (!nativeParamKeys.contains(key)) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "NATIVE 参数键不在白名单中: " + entry.getKey());
            }
            if (SENSITIVE_PARAM_KEYS.contains(key)) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "NATIVE 参数不得携带敏感身份信息: " + entry.getKey());
            }
            if (entry.getValue() != null && entry.getValue().length() > 512) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "NATIVE 参数值过长: " + entry.getKey());
            }
        }
    }

    /**
     * 校验原生路由编码
     *
     * @param routeCode 路由编码
     */
    public void validateNativeRouteCode(String routeCode) {
        if (!StringUtils.hasText(routeCode)
                || !nativeRouteCodes.contains(routeCode.trim().toLowerCase(Locale.ROOT))) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "NATIVE 路由编码不在白名单中");
        }
    }

    /**
     * 校验跳转目标结构（存在性与发布状态由应用服务另行校验）
     *
     * @param target 跳转目标
     */
    public void validateTarget(ExploreTarget target) {
        if (target == null || target.getType() == null) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "跳转目标不能为空");
        }
        switch (target.getType()) {
            case ARTICLE, TOPIC -> {
                if (!StringUtils.hasText(target.getRefId()) || !StringUtils.hasText(target.getRevisionId())) {
                    throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                            target.getType().name() + " 目标必须包含业务标识与修订标识");
                }
            }
            case H5 -> validateH5Url(target.getUrl());
            case NATIVE -> validateNative(target.getRouteCode(), target.getParams());
            default -> throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的跳转目标");
        }
    }

    private URI parseHttps(String url, String field) {
        if (!StringUtils.hasText(url) || url.startsWith("//")) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, field + " 必须为绝对 HTTPS 地址");
        }
        String trimmed = url.trim();
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (Character.isWhitespace(current) || Character.isISOControl(current) || current == '\\') {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, field + " 包含非法字符");
            }
        }
        try {
            URI uri = new URI(trimmed);
            if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        field + " 必须为绝对 HTTPS 地址");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, field + " 不是合法 URL", e);
        }
    }

    private Set<String> normalize(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
