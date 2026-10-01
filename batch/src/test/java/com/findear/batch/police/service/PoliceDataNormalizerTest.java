package com.findear.batch.police.service;

import com.findear.batch.police.client.Lost112ApiService;
import com.findear.batch.police.domain.PoliceAcquiredData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** item → 문서 정규화 규칙 (06 §3). 외부 호출·DB 없음 */
class PoliceDataNormalizerTest {

    private final PoliceDataNormalizer normalizer = new PoliceDataNormalizer();

    /** atcId·fdYmd가 있는 item에 key, value, key, value... 를 얹는다 */
    private Map<String, String> item(String... keyValues) {
        Map<String, String> item = new HashMap<>();
        item.put("atcId", "F2099010100000001");
        item.put("fdYmd", "2026-09-30");
        for (int i = 0; i < keyValues.length; i += 2) {
            item.put(keyValues[i], keyValues[i + 1]);
        }
        return item;
    }

    private PoliceAcquiredData normalize(Map<String, String> item) {
        return normalizer.normalize(item, Lost112ApiService.POLICE).orElseThrow();
    }

    @DisplayName("경찰청 모양 item: 필드 매핑, 문서 ID = atcId, 응답 clrNm 괄호 안 값, source")
    @Test
    void policeItem() {
        PoliceAcquiredData doc = normalize(item("clrNm", "블랙(검정)", "depPlace", "서울은평경찰서", "fdFilePathImg", "https://example.test/1.jpg",
                "fdPrdtNm", "남성용 반지갑", "fdSbjt", "남성용 반지갑(블랙(검정)색)을 습득하여 보관하고 있습니다", "fdSn", "1",
                "prdtClNm", "지갑 > 남성용 지갑"));

        assertThat(doc.getId()).isEqualTo("F2099010100000001");
        assertThat(doc.getAtcId()).isEqualTo("F2099010100000001");
        assertThat(doc.getDepPlace()).isEqualTo("서울은평경찰서");
        assertThat(doc.getFdFilePathImg()).isEqualTo("https://example.test/1.jpg");
        assertThat(doc.getFdPrdtNm()).isEqualTo("남성용 반지갑");
        assertThat(doc.getFdSbjt()).startsWith("남성용 반지갑(블랙");
        assertThat(doc.getClrNm()).isEqualTo("검정");
        assertThat(doc.getFdYmd()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(doc.getPrdtClNm()).isEqualTo("지갑 > 남성용 지갑");
        assertThat(doc.getMainPrdtClNm()).isEqualTo("지갑");
        assertThat(doc.getSubPrdtClNm()).isEqualTo("남성용 지갑");
        assertThat(doc.getFdSn()).isEqualTo("1");
        assertThat(doc.getSource()).isEqualTo("POLICE");
        assertThat(doc.getAddr()).isNull();
    }

    @DisplayName("포털기관 모양 item: fdYmd yyyyMMdd → yyyy-MM-dd, prdtClNm 공백 없는 구분자, source PORTAL")
    @Test
    void portalItem() {
        PoliceAcquiredData doc = normalizer.normalize(
                item("fdYmd", "20110223", "prdtClNm", "지갑>여성지갑", "clrNm", "은색"), Lost112ApiService.PORTAL).orElseThrow();

        assertThat(doc.getFdYmd()).isEqualTo(LocalDate.of(2011, 2, 23));
        assertThat(doc.getPrdtClNm()).isEqualTo("지갑>여성지갑");
        assertThat(doc.getMainPrdtClNm()).isEqualTo("지갑");
        assertThat(doc.getSubPrdtClNm()).isEqualTo("여성지갑");
        assertThat(doc.getClrNm()).isEqualTo("은색");
        assertThat(doc.getSource()).isEqualTo("PORTAL");
    }

    @DisplayName("fdYmd: yyyy-MM-dd와 yyyyMMdd만 받고 그 밖(형식·달력상 없는 날짜)은 건너뛴다")
    @Test
    void dateFormats() {
        assertThat(normalize(item("fdYmd", "2018-06-01")).getFdYmd()).isEqualTo(LocalDate.of(2018, 6, 1));
        assertThat(normalize(item("fdYmd", "20180601")).getFdYmd()).isEqualTo(LocalDate.of(2018, 6, 1));
        assertThat(normalize(item("fdYmd", " 20180601 ")).getFdYmd()).isEqualTo(LocalDate.of(2018, 6, 1));

        for (String bad : new String[]{"2018/06/01", "2018-6-1", "180601", "2018-13-01", "20180231", "abc", "", "  "}) {
            assertThat(normalizer.normalize(item("fdYmd", bad), Lost112ApiService.POLICE)).as("fdYmd=%s", bad).isEmpty();
        }
    }

    @DisplayName("atcId 또는 fdYmd가 없거나 비어 있으면 건너뛴다")
    @Test
    void missingKeys() {
        Map<String, String> noAtcId = item();
        noAtcId.remove("atcId");
        Map<String, String> noDate = item();
        noDate.remove("fdYmd");

        assertThat(normalizer.normalize(noAtcId, Lost112ApiService.POLICE)).isEmpty();
        assertThat(normalizer.normalize(noDate, Lost112ApiService.POLICE)).isEmpty();
        assertThat(normalizer.normalize(item("atcId", "  "), Lost112ApiService.POLICE)).isEmpty();
        assertThat(normalizer.normalize(new HashMap<>(), Lost112ApiService.POLICE)).isEmpty();
    }

    @DisplayName("prdtClNm: > 로 나눠 앞뒤 공백을 빼고, 소분류가 없으면 null, 분류가 없으면 둘 다 null")
    @Test
    void productClasses() {
        PoliceAcquiredData one = normalize(item("prdtClNm", "가방"));
        assertThat(one.getMainPrdtClNm()).isEqualTo("가방");
        assertThat(one.getSubPrdtClNm()).isNull();

        PoliceAcquiredData spaced = normalize(item("prdtClNm", "  전자기기  >  이어폰  "));
        assertThat(spaced.getPrdtClNm()).isEqualTo("전자기기  >  이어폰");
        assertThat(spaced.getMainPrdtClNm()).isEqualTo("전자기기");
        assertThat(spaced.getSubPrdtClNm()).isEqualTo("이어폰");

        PoliceAcquiredData none = normalize(item());
        assertThat(none.getPrdtClNm()).isNull();
        assertThat(none.getMainPrdtClNm()).isNull();
        assertThat(none.getSubPrdtClNm()).isNull();
    }

    @DisplayName("clrNm: 끝이 (…)면 괄호 안 값, 아니면 원문, 없으면 fdSbjt에서 추출, 그것도 없으면 null")
    @Test
    void colorName() {
        assertThat(normalize(item("clrNm", "블랙(검정)")).getClrNm()).isEqualTo("검정");
        assertThat(normalize(item("clrNm", " 레드(빨강) ")).getClrNm()).isEqualTo("빨강");
        assertThat(normalize(item("clrNm", "은색")).getClrNm()).isEqualTo("은색");
        assertThat(normalize(item("clrNm", "블랙()")).getClrNm()).isEqualTo("블랙()");

        // 응답에 clrNm이 없으면 fdSbjt(팀 코드의 추출 규칙)
        assertThat(normalize(item("fdSbjt", "남성용 반지갑(블랙(검정)색)을 습득하여 보관하고 있습니다")).getClrNm()).isEqualTo("검정");
        assertThat(normalize(item("fdSbjt", "여성용 지갑(레드(빨강)색)을 습득하여 보관하고 있습니다")).getClrNm()).isEqualTo("빨강");

        // 어디서도 못 찾으면 null
        assertThat(normalize(item("fdSbjt", "무선 이어폰을 습득하여 보관하고 있습니다")).getClrNm()).isNull();
        assertThat(normalize(item()).getClrNm()).isNull();
    }

    @DisplayName("fdSbjt 색상 추출: 팀 코드 규칙 (괄호가 둘 미만이거나 '색'이 없으면 null)")
    @Test
    void extractColorFromSubject() {
        assertThat(PoliceDataNormalizer.extractColorFromSubject("가방(그레이(회)색)을 습득")).isEqualTo("회");
        assertThat(PoliceDataNormalizer.extractColorFromSubject("지갑(검정)색을 습득")).isNull();   // 괄호가 하나
        assertThat(PoliceDataNormalizer.extractColorFromSubject("색깔 없음")).isNull();
        assertThat(PoliceDataNormalizer.extractColorFromSubject("색")).isNull();
        assertThat(PoliceDataNormalizer.extractColorFromSubject("(a(b)색")).isNull();               // 뒷 조각이 없음
        assertThat(PoliceDataNormalizer.extractColorFromSubject(null)).isNull();
    }

    @DisplayName("값이 비어 있는 필드는 null, 사진 URL이 없으면 null")
    @Test
    void blankValuesBecomeNull() {
        PoliceAcquiredData doc = normalize(item("depPlace", "  ", "fdFilePathImg", "", "fdPrdtNm", "물품"));

        assertThat(doc.getDepPlace()).isNull();
        assertThat(doc.getFdFilePathImg()).isNull();
        assertThat(doc.getFdPrdtNm()).isEqualTo("물품");
    }
}
