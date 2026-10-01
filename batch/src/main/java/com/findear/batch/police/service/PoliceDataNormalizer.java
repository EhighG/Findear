package com.findear.batch.police.service;

import com.findear.batch.police.client.Lost112ApiService;
import com.findear.batch.police.domain.PoliceAcquiredData;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lost112 응답 item을 인덱스 문서로 바꾼다 (06 §3).
 * <ul>
 *   <li>atcId·fdYmd가 없거나 fdYmd 형식이 틀린 item은 건너뛴다 (빈 결과)</li>
 *   <li>fdYmd: {@code yyyy-MM-dd}(경찰청), {@code yyyyMMdd}(포털기관) → {@code yyyy-MM-dd}</li>
 *   <li>prdtClNm: 원문 그대로 저장, {@code >}로 나눠 첫 값이 대분류, 둘째 값이 소분류 ({@code 지갑 > 남성용 지갑}, {@code 지갑>여성지갑})</li>
 *   <li>clrNm: 응답 값({@code 블랙(검정)} → {@code 검정}, 괄호가 없으면 원문), 없으면 fdSbjt에서 추출(팀 코드 규칙), 그것도 없으면 null</li>
 * </ul>
 */
@Component
public class PoliceDataNormalizer {

    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

    /** 끝이 (…)로 끝나는 색상 이름: 마지막 괄호 안 값 */
    private static final Pattern TRAILING_PARENTHESES = Pattern.compile("^.*\\(([^()]*)\\)$");

    public Optional<PoliceAcquiredData> normalize(Map<String, String> item, Lost112ApiService source) {

        String atcId = blankToNull(item.get("atcId"));
        LocalDate fdYmd = parseDate(item.get("fdYmd"));
        if (atcId == null || fdYmd == null) {
            return Optional.empty();
        }

        String prdtClNm = blankToNull(item.get("prdtClNm"));
        String[] classes = splitClasses(prdtClNm);
        String fdSbjt = blankToNull(item.get("fdSbjt"));

        return Optional.of(PoliceAcquiredData.builder()
                .id(atcId)
                .atcId(atcId)
                .depPlace(blankToNull(item.get("depPlace")))
                .fdFilePathImg(blankToNull(item.get("fdFilePathImg")))
                .fdPrdtNm(blankToNull(item.get("fdPrdtNm")))
                .fdSbjt(fdSbjt)
                .clrNm(colorName(blankToNull(item.get("clrNm")), fdSbjt))
                .fdYmd(fdYmd)
                .prdtClNm(prdtClNm)
                .mainPrdtClNm(classes[0])
                .subPrdtClNm(classes[1])
                .fdSn(blankToNull(item.get("fdSn")))
                .source(source.name())
                .build());
    }

    /** yyyy-MM-dd 또는 yyyyMMdd → 날짜 (인덱스에는 yyyy-MM-dd로 저장된다). 그 밖은 null */
    static LocalDate parseDate(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            if (text.length() == 10) {
                return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (text.length() == 8) {
                return LocalDate.parse(text, BASIC);
            }
        } catch (DateTimeParseException e) {
            return null;
        }
        return null;
    }

    /** {대분류, 소분류}. 구분자 {@code >}, 앞뒤 공백 제거. 분류가 없으면 둘 다 null, 소분류가 없으면 null */
    static String[] splitClasses(String prdtClNm) {
        String[] result = new String[2];
        if (prdtClNm == null) {
            return result;
        }
        String[] parts = prdtClNm.split(">");
        result[0] = parts.length > 0 ? blankToNull(parts[0]) : null;
        result[1] = parts.length > 1 ? blankToNull(parts[1]) : null;
        return result;
    }

    static String colorName(String clrNm, String fdSbjt) {
        if (clrNm != null) {
            Matcher matcher = TRAILING_PARENTHESES.matcher(clrNm);
            if (matcher.matches()) {
                String inner = blankToNull(matcher.group(1));
                if (inner != null) {
                    return inner;
                }
            }
            return clrNm;
        }
        return extractColorFromSubject(fdSbjt);
    }

    /**
     * 팀 코드의 색상 추출 규칙: 제목을 '색'으로 나눠 뒤에서 둘째 조각의 마지막 괄호 안 값을 쓴다.
     * 예: {@code 남성용 반지갑(블랙(검정)색)을 습득하여 보관하고 있습니다} → {@code 검정}.
     * 괄호가 둘 미만이거나 조각이 ')'로 끝나지 않으면 null.
     */
    static String extractColorFromSubject(String fdSbjt) {

        if (fdSbjt == null) {
            return null;
        }
        String[] parts = fdSbjt.split("색");
        if (parts.length < 2) {
            return null;
        }

        String lastPart = parts[parts.length - 2];
        List<Integer> openIndexes = new ArrayList<>();
        for (int i = 0; i < lastPart.length(); i++) {
            if (lastPart.charAt(i) == '(') {
                openIndexes.add(i);
            }
        }
        if (openIndexes.size() < 2 || !lastPart.endsWith(")")) {
            return null;
        }

        int start = openIndexes.get(openIndexes.size() - 1) + 1;
        int end = lastPart.length() - 1;
        return start < end ? blankToNull(lastPart.substring(start, end)) : null;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
