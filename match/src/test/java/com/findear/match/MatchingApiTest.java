package com.findear.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.findear.match.ApiTestSupport.MAPPER;
import static com.findear.match.ApiTestSupport.fixture;
import static com.findear.match.ApiTestSupport.json;
import static com.findear.match.ApiTestSupport.post;
import static com.findear.match.ApiTestSupport.postJson;
import static com.findear.match.ApiTestSupport.rand;
import static com.findear.match.ApiTestSupport.round5;
import static com.findear.match.ApiTestSupport.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@AutoConfigureMockMvc
class MatchingApiTest {

    static final long SEED = 42;

    @Autowired
    MockMvc mvc;

    // ---------- /matching/findear ----------

    @Test
    void findear_픽스처는_후보_수만큼_계약_모양으로_응답한다() throws Exception {
        var result = post(mvc, "/matching/findear", fixture("matching-findear-request.json"));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json(text(result));

        assertThat(body.get("message").asText()).isEqualTo("해당 분실물과 findear 데이터와의 매칭이 완료되었습니다");
        JsonNode list = body.get("result");
        assertThat(list).hasSize(5);
        double previous = Double.MAX_VALUE;
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : list) {
            assertThat(item.size()).isEqualTo(3);
            assertThat(item.get("lostBoardId").isIntegralNumber()).isTrue();
            assertThat(item.get("lostBoardId").asLong()).isEqualTo(101);
            assertThat(item.get("acquiredBoardId").isIntegralNumber()).isTrue();
            ids.add(item.get("acquiredBoardId").asLong());
            JsonNode rate = item.get("similarityRate");
            assertThat(rate.isFloatingPointNumber()).isTrue();
            assertThat(rate.asDouble()).isBetween(0.0, 1.0);
            assertThat(new BigDecimal(rate.toString()).scale()).isLessThanOrEqualTo(5);
            assertThat(rate.asDouble()).isLessThanOrEqualTo(previous);
            previous = rate.asDouble();
        }
        assertThat(ids).containsExactlyInAnyOrder(11L, 12L, 13L, 14L, 15L);
    }

    @Test
    void findear_같은_요청은_같은_응답() throws Exception {
        String first = text(post(mvc, "/matching/findear", fixture("matching-findear-request.json")));
        String second = text(post(mvc, "/matching/findear", fixture("matching-findear-request.json")));
        assertThat(second).isEqualTo(first);
    }

    @Test
    void findear_후보_점수는_스펙_공식과_같다() throws Exception {
        JsonNode list = postJson(mvc, "/matching/findear", fixture("matching-findear-request.json")).get("result");
        // 11: 카테고리(지갑)·색상(검정) 모두 일치, 13: 둘 다 불일치, 12: 카테고리만 일치, 14: 색상만 일치
        double[] expected = {
                round5(0.3 + 0.6 * rand(SEED, "101", "11") + 0.1 + 0.05),
                round5(0.3 + 0.6 * rand(SEED, "101", "12") + 0.1),
                round5(0.3 + 0.6 * rand(SEED, "101", "13")),
                round5(0.3 + 0.6 * rand(SEED, "101", "14") + 0.05),
        };
        long[] ids = {11, 12, 13, 14};
        for (int i = 0; i < ids.length; i++) {
            double actual = rateOf(list, ids[i]);
            assertThat(actual).as("acquiredBoardId=" + ids[i]).isCloseTo(expected[i], within(1e-9));
        }
    }

    @Test
    void findear_후보_150개는_100개로_자른다() throws Exception {
        ObjectNode request = findearRequest(150);
        JsonNode list = postJson(mvc, "/matching/findear", request.toString()).get("result");
        assertThat(list).hasSize(100);
        // 정렬 기준으로 앞 100개인지: 150개 전체 점수를 공식으로 계산해 상위 100개와 비교
        List<double[]> all = new ArrayList<>();
        for (int id = 1; id <= 150; id++) {
            all.add(new double[]{id, round5(0.3 + 0.6 * rand(SEED, "7", String.valueOf(id)))});
        }
        all.sort(Comparator.comparingDouble((double[] a) -> a[1]).reversed());
        for (int i = 0; i < 100; i++) {
            assertThat(list.get(i).get("acquiredBoardId").asLong()).isEqualTo((long) all.get(i)[0]);
            assertThat(list.get(i).get("similarityRate").asDouble()).isCloseTo(all.get(i)[1], within(1e-9));
        }
    }

    @Test
    void findear_빈_목록과_null_목록은_result가_null() throws Exception {
        String lost = "\"lostBoard\":{\"lostBoardId\":\"1\"}";
        for (String body : List.of("{" + lost + ",\"acquiredBoardList\":[]}", "{" + lost + ",\"acquiredBoardList\":null}",
                "{" + lost + "}")) {
            var result = post(mvc, "/matching/findear", body);
            assertThat(result.getResponse().getStatus()).as(body).isEqualTo(200);
            JsonNode node = json(text(result));
            assertThat(node.get("message").asText()).isEqualTo("해당 분실물과 매칭 가능한 findear 데이터가 없습니다.");
            assertThat(node.has("result")).as(body).isTrue();
            assertThat(node.get("result").isNull()).isTrue();
        }
    }

    @Test
    void findear_camelCase_좌표와_JSON_숫자_값도_받는다() throws Exception {
        String body = "{\"lostBoard\":{\"lostBoardId\":101,\"productName\":\"지갑\",\"color\":\"검정\",\"categoryName\":\"지갑\","
                + "\"description\":null,\"lostAt\":\"2026-09-20T10:30:00\",\"xPos\":126.978,\"yPos\":37.5665},"
                + "\"acquiredBoardList\":[{\"acquiredBoardId\":11,\"productName\":\"지갑\",\"color\":\"검정\","
                + "\"categoryName\":\"지갑\",\"description\":null,\"xPos\":\"126.9\",\"yPos\":\"37.5\","
                + "\"registeredAt\":\"2026-09-21T09:10:00\"}]}";
        var result = post(mvc, "/matching/findear", body);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode item = json(text(result)).get("result").get(0);
        assertThat(item.get("lostBoardId").asLong()).isEqualTo(101);
        assertThat(item.get("acquiredBoardId").asLong()).isEqualTo(11);
        assertThat(item.get("similarityRate").asDouble())
                .isCloseTo(round5(0.3 + 0.6 * rand(SEED, "101", "11") + 0.15), within(1e-9));
    }

    @Test
    void findear_잘못된_요청은_400() throws Exception {
        List<String> bodies = List.of(
                "{\"acquiredBoardList\":[{\"acquiredBoardId\":\"1\"}]}",
                "{\"lostBoard\":null,\"acquiredBoardList\":[{\"acquiredBoardId\":\"1\"}]}",
                "{\"lostBoard\":{},\"acquiredBoardList\":[{\"acquiredBoardId\":\"1\"}]}",
                "{\"lostBoard\":{\"lostBoardId\":\"abc\"},\"acquiredBoardList\":[{\"acquiredBoardId\":\"1\"}]}",
                "{\"lostBoard\":{\"lostBoardId\":\"1\"},\"acquiredBoardList\":[{\"acquiredBoardId\":\"abc\"}]}",
                "{\"lostBoard\":{\"lostBoardId\":\"1\"},\"acquiredBoardList\":[{\"productName\":\"지갑\"}]}",
                "{\"lostBoard\":{\"lostBoardId\":\"1\"},\"acquiredBoardList\":[{\"acquiredBoardId\":\"1\"},null]}",
                "{\"lostBoard\":{\"lostBoardId\":\"1\"},\"acquiredBoardList\":{\"a\":1}}",
                "[]",
                "");
        for (String body : bodies) {
            var result = post(mvc, "/matching/findear", body);
            assertThat(result.getResponse().getStatus()).as(body).isEqualTo(400);
            JsonNode error = json(text(result));
            assertThat(error.size()).as(body).isEqualTo(1);
            assertThat(error.get("message").asText()).as(body).isNotBlank();
        }
    }

    // ---------- /matching/lost ----------

    @Test
    void lost_픽스처는_입력_필드를_그대로_되돌린다() throws Exception {
        JsonNode request = json(fixture("matching-lost-request.json"));
        var result = post(mvc, "/matching/lost", fixture("matching-lost-request.json"));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json(text(result));
        assertThat(body.get("message").asText()).isEqualTo("해당 분실물과 lost112 데이터와의 매칭이 완료되었습니다");

        JsonNode list = body.get("result");
        assertThat(list).hasSize(4);
        double previous = Double.MAX_VALUE;
        for (JsonNode item : list) {
            assertThat(item.size()).isEqualTo(11);
            assertThat(item.get("lostBoardId").asLong()).isEqualTo(101);
            assertThat(item.get("acquiredBoardId").isIntegralNumber()).isTrue();
            assertThat(item.get("similarityRate").asDouble()).isLessThanOrEqualTo(previous);
            previous = item.get("similarityRate").asDouble();

            JsonNode input = findByAtcId(request.get("acquiredBoardList"), item.get("atcId").asText());
            assertThat(item.get("acquiredBoardId").asLong()).isEqualTo(input.get("id").asLong());
            for (String field : List.of("atcId", "depPlace", "fdFilePathImg", "fdPrdtNm", "fdSbjt", "clrNm", "fdYmd",
                    "mainPrdtClNm")) {
                assertThat(item.has(field)).as(field).isTrue();
                assertThat(item.get(field)).as(field).isEqualTo(input.get(field));
            }
        }
        // clrNm이 null인 후보는 "clrNm": null 로 키가 있다
        JsonNode phone = findByAtcId(list, "F2026092000002");
        assertThat(phone.has("clrNm")).isTrue();
        assertThat(phone.get("clrNm").isNull()).isTrue();
        assertThat(findByAtcId(list, "F2026092100003").get("fdFilePathImg").isNull()).isTrue();
    }

    @Test
    void lost_점수는_mainPrdtClNm_기준_카테고리_가산과_공식을_따른다() throws Exception {
        JsonNode list = postJson(mvc, "/matching/lost", fixture("matching-lost-request.json")).get("result");
        // 분실물 카테고리 "지갑", 색상 "검정". 5001: 카테고리만(clrNm "검정색"은 불일치), 5003: 둘 다, 5002·5004: 없음
        assertThat(rateOfAtc(list, "F2026092000001"))
                .isCloseTo(round5(0.3 + 0.6 * rand(SEED, "101", "F2026092000001") + 0.1), within(1e-9));
        assertThat(rateOfAtc(list, "F2026092100003"))
                .isCloseTo(round5(0.3 + 0.6 * rand(SEED, "101", "F2026092100003") + 0.15), within(1e-9));
        assertThat(rateOfAtc(list, "F2026092000002"))
                .isCloseTo(round5(0.3 + 0.6 * rand(SEED, "101", "F2026092000002")), within(1e-9));
        assertThat(rateOfAtc(list, "F2026092200004"))
                .isCloseTo(round5(0.3 + 0.6 * rand(SEED, "101", "F2026092200004")), within(1e-9));
    }

    @Test
    void lost_같은_요청은_같은_응답() throws Exception {
        String first = text(post(mvc, "/matching/lost", fixture("matching-lost-request.json")));
        String second = text(post(mvc, "/matching/lost", fixture("matching-lost-request.json")));
        assertThat(second).isEqualTo(first);
    }

    @Test
    void lost_acquiredBoardId는_숫자면_숫자_아니면_문자열_없으면_null() throws Exception {
        String body = "{\"lostBoard\":{\"lostBoardId\":\"5\"},\"acquiredBoardList\":["
                + "{\"id\":\"77\",\"atcId\":\"A1\"},{\"id\":\"abc\",\"atcId\":\"A2\"},{\"atcId\":\"A3\"},{\"id\":88,\"atcId\":\"A4\"}]}";
        JsonNode list = postJson(mvc, "/matching/lost", body).get("result");
        assertThat(list).hasSize(4);
        JsonNode a1 = findByAtcId(list, "A1").get("acquiredBoardId");
        JsonNode a2 = findByAtcId(list, "A2").get("acquiredBoardId");
        JsonNode a3 = findByAtcId(list, "A3").get("acquiredBoardId");
        JsonNode a4 = findByAtcId(list, "A4").get("acquiredBoardId");
        assertThat(a1.isIntegralNumber()).isTrue();
        assertThat(a1.asLong()).isEqualTo(77);
        assertThat(a2.isTextual()).isTrue();
        assertThat(a2.asText()).isEqualTo("abc");
        assertThat(a3.isNull()).isTrue();
        assertThat(a4.asLong()).isEqualTo(88);
    }

    @Test
    void lost_atcId가_비면_id를_점수_키로_쓴다() throws Exception {
        String body = "{\"lostBoard\":{\"lostBoardId\":\"5\"},\"acquiredBoardList\":[{\"id\":\"77\",\"atcId\":\" \"}]}";
        JsonNode item = postJson(mvc, "/matching/lost", body).get("result").get(0);
        assertThat(item.get("similarityRate").asDouble())
                .isCloseTo(round5(0.3 + 0.6 * rand(SEED, "5", "77")), within(1e-9));
    }

    @Test
    void lost_빈_목록과_null_목록은_result가_null() throws Exception {
        String lost = "\"lostBoard\":{\"lostBoardId\":\"1\"}";
        for (String body : List.of("{" + lost + ",\"acquiredBoardList\":[]}", "{" + lost + ",\"acquiredBoardList\":null}",
                "{" + lost + "}")) {
            var result = post(mvc, "/matching/lost", body);
            assertThat(result.getResponse().getStatus()).as(body).isEqualTo(200);
            JsonNode node = json(text(result));
            assertThat(node.get("message").asText()).isEqualTo("해당 분실물과 매칭 가능한 lost112 데이터가 없습니다.");
            assertThat(node.has("result")).isTrue();
            assertThat(node.get("result").isNull()).isTrue();
        }
    }

    @Test
    void lost_후보_150개는_100개로_자른다() throws Exception {
        ObjectNode request = MAPPER.createObjectNode();
        request.putObject("lostBoard").put("lostBoardId", "9");
        ArrayNode list = request.putArray("acquiredBoardList");
        for (int i = 1; i <= 150; i++) {
            list.addObject().put("id", String.valueOf(i)).put("atcId", "ATC" + i);
        }
        JsonNode result = postJson(mvc, "/matching/lost", request.toString()).get("result");
        assertThat(result).hasSize(100);
        double previous = Double.MAX_VALUE;
        for (JsonNode item : result) {
            assertThat(item.get("similarityRate").asDouble()).isLessThanOrEqualTo(previous);
            previous = item.get("similarityRate").asDouble();
        }
    }

    @Test
    void lost_잘못된_요청은_400() throws Exception {
        List<String> bodies = List.of(
                "{\"acquiredBoardList\":[{\"id\":\"1\"}]}",
                "{\"lostBoard\":{},\"acquiredBoardList\":[{\"id\":\"1\"}]}",
                "{\"lostBoard\":{\"lostBoardId\":\"abc\"},\"acquiredBoardList\":[{\"id\":\"1\"}]}",
                "{\"lostBoard\":{\"lostBoardId\":\"1\"},\"acquiredBoardList\":[null]}",
                "{\"lostBoard\":{\"lostBoardId\":\"1\"}, ");
        for (String body : bodies) {
            var result = post(mvc, "/matching/lost", body);
            assertThat(result.getResponse().getStatus()).as(body).isEqualTo(400);
            JsonNode error = json(text(result));
            assertThat(error.size()).as(body).isEqualTo(1);
            assertThat(error.get("message").asText()).isNotBlank();
        }
    }

    // ---------- 도우미 ----------

    static ObjectNode findearRequest(int count) {
        ObjectNode request = MAPPER.createObjectNode();
        request.putObject("lostBoard").put("lostBoardId", "7");
        ArrayNode list = request.putArray("acquiredBoardList");
        for (int i = 1; i <= count; i++) {
            list.addObject().put("acquiredBoardId", String.valueOf(i));
        }
        return request;
    }

    static double rateOf(JsonNode list, long acquiredBoardId) {
        for (JsonNode item : list) {
            if (item.get("acquiredBoardId").asLong() == acquiredBoardId) {
                return item.get("similarityRate").asDouble();
            }
        }
        throw new AssertionError("결과에 없음: " + acquiredBoardId);
    }

    static double rateOfAtc(JsonNode list, String atcId) {
        return findByAtcId(list, atcId).get("similarityRate").asDouble();
    }

    static JsonNode findByAtcId(JsonNode list, String atcId) {
        for (JsonNode item : list) {
            if (atcId.equals(item.get("atcId").asText())) {
                return item;
            }
        }
        throw new AssertionError("atcId 없음: " + atcId);
    }
}
