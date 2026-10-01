package com.findear.batch.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * match 서버 호출용 RestTemplate. 루트 URI는 servers.match-server.url이고, 호출은 "/matching/findear", "/matching/lost" 상대 경로로 한다.
 * Boot가 제공하는 RestTemplateBuilder로 만들어야 http_client_requests 메트릭이 수집된다 (K-09).
 */
@Configuration
public class MatchServerConfig {

    @Bean
    public RestTemplate matchRestTemplate(RestTemplateBuilder builder,
                                          @Value("${servers.match-server.url}") String matchServerUrl,
                                          @Value("${servers.match-server.connect-timeout}") Duration connectTimeout,
                                          @Value("${servers.match-server.read-timeout}") Duration readTimeout) {

        return builder
                .rootUri(matchServerUrl)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .build();
    }
}
