package com.findear.batch.police.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 설정 {@code lost112.*}. 값의 출처는 application.yml (환경변수 LOST112_SERVICE_KEY·LOST112_COLLECT_ENABLED·LOST112_COLLECT_DAYS·LOST112_PAGE_SIZE).
 * base-url·타임아웃·max-pages는 환경변수 없이 고정값이고 테스트에서만 바꾼다 (D-38).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lost112")
public class Lost112Properties {

    /** 공공데이터포털 Decoding 키 (Encoding 키를 넣어도 받아 준다). 비어 있으면 수집하지 않는다 */
    private String serviceKey = "";

    /** policeJob이 Lost112 수집 스텝을 실행하는지. false(기본)면 수집하지 않는다 (수동 수집 {@code POST /search/save}는 이 값과 무관) */
    private boolean collectEnabled = false;

    private String baseUrl = "https://apis.data.go.kr/1320000";

    /** 오늘 - N일 ~ 오늘 범위를 수집한다 */
    private int collectDays = 30;

    /** 요청 한 번의 numOfRows */
    private int pageSize = 1000;

    /** 서비스당 페이지 수 안전 상한. 닿으면 멈추고 WARN */
    private int maxPages = 1000;

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(60);

    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
