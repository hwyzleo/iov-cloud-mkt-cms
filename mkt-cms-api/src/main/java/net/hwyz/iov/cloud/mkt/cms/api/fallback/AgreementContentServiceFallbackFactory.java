package net.hwyz.iov.cloud.mkt.cms.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.api.service.AgreementContentService;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementBundleResponse;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementContentResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 协议内容读取服务降级处理。
 * <p>
 * 降级不返回伪成功内容：协议内容缺失时抛异常，由调用方按自身策略处理，
 * 不允许以空正文或缓存内容伪装为 CMS 已返回（MKT-CMS-DSN-CR-001 §6.3/§8）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class AgreementContentServiceFallbackFactory implements FallbackFactory<AgreementContentService> {

    @Override
    public AgreementContentService create(Throwable throwable) {
        return new AgreementContentService() {

            @Override
            public AgreementBundleResponse getCurrentBundle(String scene, String market, String locale,
                                                           String brandCode, String carLineCode) {
                log.error("读取当前协议发布包失败: scene={}, market={}, locale={}, brandCode={}, carLineCode={}, cause={}",
                        scene, market, locale, brandCode, carLineCode, throwable.getMessage());
                throw new IllegalStateException("读取当前协议发布包失败，CMS 未返回可用内容", throwable);
            }

            @Override
            public AgreementContentResponse getPublishedRevision(String revisionId) {
                log.error("读取协议历史修订失败: revisionId={}, cause={}", revisionId, throwable.getMessage());
                throw new IllegalStateException("读取协议历史修订失败，CMS 未返回可用内容", throwable);
            }
        };
    }
}
