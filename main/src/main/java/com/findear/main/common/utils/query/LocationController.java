package com.findear.main.common.utils.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findear.main.common.exception.ExternalServiceNotConfiguredException;
import com.findear.main.common.exception.ExternalServiceUnavailableException;
import com.findear.main.common.response.FailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * VWorld(공간정보 오픈플랫폼) 프록시. 응답은 VWorld JSON 본문을 그대로 돌려준다.
 * 공식 문서: 검색 API 2.0 https://www.vworld.kr/dev/v4dv_search2_s001.do,
 * 주소→좌표 변환 API 2.0 https://www.vworld.kr/dev/v4dv_geocoderguide2_s001.do
 */
@Slf4j
@RequestMapping("/location")
@RestController
public class LocationController {

    private static final String SERVICE_NAME = "VWorld";
    private static final String API_KEY_ENV = "VWORLD_API_KEY";
    private static final MediaType JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    // 검색 API 2.0: size 1~1000(기본 10), page 기본 1
    static final long DEFAULT_SIZE = 10;
    static final long MAX_SIZE = 1000;
    static final long DEFAULT_PAGE = 1;

    private final String apiKey;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 공용 RestTemplate 빈은 타임아웃이 없고 matching·Naver도 함께 쓰므로, 이 프록시만 짧은 타임아웃을 갖도록 빌더에서 따로 만든다
    public LocationController(@Value("${vworld.api-key}") String apiKey,
                              @Value("${vworld.base-url}") String baseUrl,
                              RestTemplateBuilder restTemplateBuilder) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(LocationSearchReqDto dto) {
        requireApiKey();
        if (isBlank(dto.getQuery())) {
            return badRequest("query는 필수입니다");
        }
        long size = dto.getSize() == null ? DEFAULT_SIZE : dto.getSize();
        long page = dto.getPage() == null ? DEFAULT_PAGE : dto.getPage();
        if (size < 1 || size > MAX_SIZE) {
            return badRequest("size는 1~" + MAX_SIZE + " 범위여야 합니다");
        }
        if (page < 1) {
            return badRequest("page는 1 이상이어야 합니다");
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/req/search")
                .queryParam("key", "{key}")
                .queryParam("service", "search")
                .queryParam("request", "search")
                .queryParam("version", "2.0")
                .queryParam("crs", "epsg:4326")
                .queryParam("size", "{size}")
                .queryParam("page", "{page}")
                .queryParam("query", "{query}")
                .queryParam("type", "PLACE")
                .queryParam("format", "json")
                .queryParam("errorformat", "json")
                .encode()
                .build(Map.of("key", apiKey, "size", size, "page", page, "query", dto.getQuery()));
        return proxy(uri);
    }

    @GetMapping(value = "/address", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> address(LocationAddressReqDto dto) {
        requireApiKey();
        if (isBlank(dto.getAddress())) {
            return badRequest("address는 필수입니다");
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/req/address")
                .queryParam("key", "{key}")
                .queryParam("service", "address")
                .queryParam("request", "getcoord")
                .queryParam("version", "2.0")
                .queryParam("crs", "epsg:4326")
                .queryParam("address", "{address}")
                .queryParam("refine", "true")
                .queryParam("simple", "false")
                .queryParam("type", "road")
                .queryParam("format", "json")
                .queryParam("errorformat", "json")
                .encode()
                .build(Map.of("key", apiKey, "address", dto.getAddress()));
        return proxy(uri);
    }

    private void requireApiKey() {
        if (isBlank(apiKey)) {
            throw new ExternalServiceNotConfiguredException(SERVICE_NAME, API_KEY_ENV);
        }
    }

    // VWorld 본문을 바이트 그대로 돌려준다(문자 인코딩 변환 없음). 키가 들어 있는 URL은 로그·응답에 남기지 않는다.
    private ResponseEntity<?> proxy(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        } catch (RestClientException e) {
            throw new ExternalServiceUnavailableException(SERVICE_NAME, e);
        }
        byte[] body = response.getBody() == null ? new byte[0] : response.getBody();
        logVWorldError(body);
        // 문서상 VWorld는 오류(status=ERROR)도 JSON 본문으로 주므로 그대로 200으로 넘긴다 (기존 동작 유지)
        return ResponseEntity.ok().contentType(JSON_UTF8).body(body);
    }

    private void logVWorldError(byte[] body) {
        try {
            JsonNode res = objectMapper.readTree(body).path("response");
            if ("ERROR".equals(res.path("status").asText())) {
                log.warn("VWorld 오류 응답: code={}, level={}",
                        res.path("error").path("code").asText(), res.path("error").path("level").asText());
            }
        } catch (Exception ignored) {
            // JSON이 아니어도 본문은 그대로 전달한다
        }
    }

    private static ResponseEntity<FailResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new FailResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
