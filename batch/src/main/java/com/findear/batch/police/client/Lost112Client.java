package com.findear.batch.police.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lost112 오픈API 한 페이지를 요청해 XML 본문(바이트)을 돌려준다. 파싱은 {@link Lost112XmlParser}.
 * <ul>
 *   <li>serviceKey는 정확히 한 번 인코딩한다 (Decoding 키의 {@code + / =}가 깨지지 않게). Encoding 키({@code %XX} 포함)를 넣었으면 한 번 풀고 같은 방식으로 인코딩한다.</li>
 *   <li>보내는 파라미터는 serviceKey·pageNo·numOfRows·START_YMD·END_YMD뿐이다 (비어 있는 옵션 파라미터는 보내지 않는다).</li>
 *   <li>키와 키가 든 URL은 로그·예외 메시지에 남기지 않는다.</li>
 * </ul>
 */
@Component
public class Lost112Client {

    private static final Pattern PERCENT_ENCODED = Pattern.compile("%[0-9A-Fa-f]{2}");

    private final RestTemplate restTemplate;
    private final Lost112Properties properties;

    public Lost112Client(@Qualifier("lost112RestTemplate") RestTemplate restTemplate, Lost112Properties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * @param startYmd yyyyMMdd
     * @param endYmd   yyyyMMdd
     * @throws Lost112Exception HTTP 오류·연결 실패·시간 초과 (메시지에 키 없음)
     */
    public byte[] fetchPage(Lost112ApiService service, int pageNo, int numOfRows, String startYmd, String endYmd) {

        URI uri = buildUri(service, pageNo, numOfRows, startYmd, endYmd);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.ALL));

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            byte[] body = response.getBody();
            return body == null ? new byte[0] : body;
        } catch (HttpStatusCodeException e) {
            throw new Lost112Exception("HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            throw new Lost112Exception("연결 실패 또는 시간 초과 (" + (cause == null ? e.getClass() : cause.getClass()).getSimpleName() + ")");
        } catch (RestClientException e) {
            throw new Lost112Exception("요청 실패 (" + e.getClass().getSimpleName() + ")");
        }
    }

    URI buildUri(Lost112ApiService service, int pageNo, int numOfRows, String startYmd, String endYmd) {

        // .encode()는 변수 값을 엄격하게(예약 문자 전부) 인코딩해서 키의 + / =가 %2B %2F %3D로 나간다
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(service.getPath())
                .queryParam("serviceKey", "{serviceKey}")
                .queryParam("pageNo", "{pageNo}")
                .queryParam("numOfRows", "{numOfRows}")
                .queryParam("START_YMD", "{startYmd}")
                .queryParam("END_YMD", "{endYmd}")
                .encode()
                .build(Map.of(
                        "serviceKey", decodingKey(properties.getServiceKey()),
                        "pageNo", pageNo,
                        "numOfRows", numOfRows,
                        "startYmd", startYmd,
                        "endYmd", endYmd));
    }

    /** Encoding 키(%XX 포함)면 한 번 풀어 Decoding 키로 만든다. '+'는 공백으로 바뀌지 않는다 (URLDecoder와 다름) */
    public static String decodingKey(String key) {
        String trimmed = key == null ? "" : key.trim();
        return PERCENT_ENCODED.matcher(trimmed).find() ? UriUtils.decode(trimmed, StandardCharsets.UTF_8) : trimmed;
    }
}
