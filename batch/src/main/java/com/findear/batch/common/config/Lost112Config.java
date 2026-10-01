package com.findear.batch.common.config;

import com.findear.batch.police.client.Lost112Properties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Lost112(공공데이터포털) 호출 전용 RestTemplate. 루트 URI 없이 Lost112Client가 전체 URI를 만든다.
 * Boot가 제공하는 RestTemplateBuilder로 만들어야 http_client_requests 메트릭이 수집된다 (K-09).
 * 타임아웃은 설정 lost112.connect-timeout(5s)·read-timeout(60s).
 */
@Configuration
public class Lost112Config {

    @Bean
    public RestTemplate lost112RestTemplate(RestTemplateBuilder builder, Lost112Properties properties) {

        return builder
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .build();
    }
}
