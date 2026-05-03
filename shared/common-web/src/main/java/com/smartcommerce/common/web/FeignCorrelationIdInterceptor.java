package com.smartcommerce.common.web;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Forwards the current request's correlation-id (CorrelationIdFilter) to any
 * downstream service called via Feign. Without this, distributed log queries
 * on Loki "filter by correlationId" return only the first hop — events from
 * the downstream service never link back.
 *
 * Auto-applies wherever Feign is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignCorrelationIdInterceptor {

    @Bean
    public RequestInterceptor correlationIdRequestInterceptor() {
        return template -> {
            var cid = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
            if (cid != null && !cid.isBlank()
                && template.headers().get(CorrelationIdFilter.CORRELATION_ID_HEADER) == null) {
                template.header(CorrelationIdFilter.CORRELATION_ID_HEADER, cid);
            }
        };
    }
}
