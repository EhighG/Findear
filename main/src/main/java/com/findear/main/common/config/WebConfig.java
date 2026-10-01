package com.findear.main.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    /** main → batch 호출용 RestTemplate 빈 이름. 주입할 때 @Qualifier로 고른다 */
    public static final String BATCH_REST_TEMPLATE = "batchRestTemplate";

    private final String[] allowedOrigins;

    public WebConfig(@Value("${cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    // Accept 헤더가 없거나 */*이면 JSON으로 응답한다 (main API 계약은 JSON).
    // firebase-admin이 끌어오는 jackson-dataformat-xml 때문에 @EnableWebMvc 아래에서는 XML 변환기가 먼저 선택되기 때문이다.
    // https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/content-negotiation.html
    // ContentNegotiationConfigurer#defaultContentType: https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/servlet/config/annotation/ContentNegotiationConfigurer.html
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedHeaders("*")
                .allowedMethods("GET", "POST", "DELETE", "PATCH", "OPTIONS")
                .allowCredentials(true);
    }

    /** Naver 로그인 등 batch 외의 호출용 공용 RestTemplate. 시간 제한은 spring.http.client.*(application.yml)를 따른다. */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    /**
     * main → batch 호출 전용 RestTemplate. rootUri는 servers.batch-server.url이고, 호출은 "/search?page={page}" 같은
     * URI 템플릿 + 변수로 한다. 그래서 HTTP 클라이언트 지표(http_client_requests_seconds)의 uri 태그가 id·검색어가 아니라
     * 고정 템플릿이 된다 (완성된 문자열이나 URI 객체를 넘기면 템플릿을 알 수 없어 시계열이 호출마다 늘어난다).
     * 변수 값은 TEMPLATE_AND_VALUES로 엄격하게 인코딩한다: + & = % 같은 예약 문자와 한글을 한 번만 퍼센트 인코딩한다 (K-01).
     * https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-uri
     * 시간 제한은 spring.http.client.*(application.yml)를 따른다.
     */
    @Bean(BATCH_REST_TEMPLATE)
    public RestTemplate batchRestTemplate(RestTemplateBuilder restTemplateBuilder,
                                          @Value("${servers.batch-server.url}") String batchServerUrl) {
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);
        return restTemplateBuilder
                .uriTemplateHandler(uriBuilderFactory)
                .rootUri(batchServerUrl)
                .build();
    }

}