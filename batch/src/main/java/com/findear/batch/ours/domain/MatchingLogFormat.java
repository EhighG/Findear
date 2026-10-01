package com.findear.batch.ours.domain;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 매칭 로그의 시각 형식. 인덱스 매핑·저장·응답이 모두 Asia/Seoul 기준 초 단위 {@code yyyy-MM-dd'T'HH:mm:ss}를 쓴다.
 */
public final class MatchingLogFormat {

    /** 매핑({@code @Field(pattern)})과 응답 문자열이 같이 쓰는 형식 */
    public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    private MatchingLogFormat() {
    }

    /** 지금 시각 (Asia/Seoul, 초 단위) */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    /** 응답용 문자열. 값이 없으면 null */
    public static String format(LocalDateTime time) {
        return time == null ? null : FORMATTER.format(time);
    }
}
