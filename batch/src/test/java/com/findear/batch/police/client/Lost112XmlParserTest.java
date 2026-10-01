package com.findear.batch.police.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.findear.batch.support.Lost112Fixtures.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 응답 한 페이지 파싱 (공공데이터포털 표준 구조 픽스처, 05 §2). 외부 호출·DB 없음 */
class Lost112XmlParserTest {

    private final Lost112XmlParser parser = new Lost112XmlParser();

    @DisplayName("경찰청 서비스 모양: item 필드와 페이지 정보를 읽는다 (>는 &gt; 엔티티, 앞뒤 공백 제거)")
    @Test
    void policePage() {
        Lost112Page page = parser.parse(bytes("police-page1.xml"));

        assertThat(page.pageNo()).isEqualTo(1);
        assertThat(page.numOfRows()).isEqualTo(2);
        assertThat(page.totalCount()).isEqualTo(3L);
        assertThat(page.items()).hasSize(2);

        assertThat(page.items().get(0))
                .containsEntry("atcId", "F2099010100000001")
                .containsEntry("clrNm", "블랙(검정)")
                .containsEntry("depPlace", "서울은평경찰서")
                .containsEntry("fdFilePathImg", "https://example.test/img/1.jpg")
                .containsEntry("fdPrdtNm", "남성용 반지갑")
                .containsEntry("fdSbjt", "남성용 반지갑(블랙(검정)색)을 습득하여 보관하고 있습니다")
                .containsEntry("fdSn", "1")
                .containsEntry("fdYmd", "2026-09-30")
                .containsEntry("prdtClNm", "지갑 > 남성용 지갑");
        // clrNm이 없는 item에는 키가 없다
        assertThat(page.items().get(1)).doesNotContainKey("clrNm");
    }

    @DisplayName("포털기관 서비스 모양: fdYmd yyyyMMdd, prdtClNm 구분자 공백 없음")
    @Test
    void portalPage() {
        Lost112Page page = parser.parse(bytes("portal-page1.xml"));

        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0)).containsEntry("fdYmd", "20260927").containsEntry("prdtClNm", "지갑>여성지갑").containsEntry("clrNm", "은색");
        assertThat(page.totalCount()).isEqualTo(3L);
    }

    @DisplayName("resultCode 00이고 items가 비어 있으면 빈 페이지")
    @Test
    void emptyItems() {
        Lost112Page page = parser.parse(bytes("empty-items.xml"));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalCount()).isZero();
    }

    @DisplayName("resultCode 03(NODATA)은 빈 페이지로 본다")
    @Test
    void noData() {
        Lost112Page page = parser.parse(bytes("no-data.xml"));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalCount()).isZero();
    }

    @DisplayName("totalCount가 없는 응답은 totalCount null")
    @Test
    void missingTotalCount() {
        String xml = "<response><header><resultCode>00</resultCode></header><body><items><item><atcId>F1</atcId></item></items></body></response>";

        Lost112Page page = parser.parse(xml.getBytes(StandardCharsets.UTF_8));

        assertThat(page.totalCount()).isNull();
        assertThat(page.items()).hasSize(1);
    }

    @DisplayName("resultCode가 00·03이 아니면 코드와 메시지를 담아 실패")
    @Test
    void resultCodeError() {
        assertThatThrownBy(() -> parser.parse(bytes("result-error.xml")))
                .isInstanceOf(Lost112Exception.class)
                .hasMessage("resultCode 99 UNKNOWN ERROR");
    }

    @DisplayName("게이트웨이 오류(OpenAPI_ServiceResponse): 30 키 미등록, 22 일일 호출 초과")
    @Test
    void gatewayErrors() {
        assertThatThrownBy(() -> parser.parse(bytes("gateway-30.xml")))
                .isInstanceOf(Lost112Exception.class)
                .hasMessage("게이트웨이 오류 30 SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
        assertThatThrownBy(() -> parser.parse(bytes("gateway-22.xml")))
                .isInstanceOf(Lost112Exception.class)
                .hasMessage("게이트웨이 오류 22 LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR");
    }

    @DisplayName("XML이 아니거나 구조가 다르거나 비어 있으면 실패")
    @Test
    void notAnExpectedDocument() {
        assertThatThrownBy(() -> parser.parse(bytes("not-xml.txt"))).isInstanceOf(Lost112Exception.class).hasMessageContaining("XML로 읽을 수 없는");
        assertThatThrownBy(() -> parser.parse("{\"response\":{}}".getBytes(StandardCharsets.UTF_8))).isInstanceOf(Lost112Exception.class);
        assertThatThrownBy(() -> parser.parse("<other><a/></other>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(Lost112Exception.class).hasMessageContaining("루트 <other>");
        assertThatThrownBy(() -> parser.parse("<response><body/></response>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(Lost112Exception.class).hasMessageContaining("resultCode 없음");
        assertThatThrownBy(() -> parser.parse(new byte[0])).isInstanceOf(Lost112Exception.class).hasMessage("빈 응답");
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(Lost112Exception.class);
    }

    @DisplayName("XXE: DOCTYPE(외부 엔티티 선언)가 있는 응답은 거부한다")
    @Test
    void doctypeIsRejected() {
        assertThatThrownBy(() -> parser.parse(bytes("xxe.xml")))
                .isInstanceOf(Lost112Exception.class)
                .hasMessageContaining("XML로 읽을 수 없는");
    }
}
