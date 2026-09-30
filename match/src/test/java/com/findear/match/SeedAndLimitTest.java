package com.findear.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.findear.match.ApiTestSupport.MAPPER;
import static com.findear.match.ApiTestSupport.fixture;
import static com.findear.match.ApiTestSupport.postJson;
import static com.findear.match.ApiTestSupport.rand;
import static com.findear.match.ApiTestSupport.round5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** seed=7, max-results=3 설정에서: seed가 점수에 반영되고, 상한이 전체 정렬의 상위 N개와 같다. */
@SpringBootTest(properties = {"match.mock.seed=7", "match.mock.max-results=3"})
@AutoConfigureMockMvc
class SeedAndLimitTest {

    @Autowired
    MockMvc mvc;

    @Test
    void seed가_다르면_점수가_달라지고_새_seed의_공식을_따른다() throws Exception {
        JsonNode list = postJson(mvc, "/matching/findear", fixture("matching-findear-request.json")).get("result");
        // 상한 3개라 결과에는 상위 3개만 있다. 각 항목이 seed=7 공식과 같고 seed=42 공식과는 다르다
        assertThat(list).hasSize(3);
        for (JsonNode item : list) {
            String id = item.get("acquiredBoardId").asText();
            double bonus = switch (id) {
                case "11" -> 0.15;
                case "12" -> 0.1;
                case "14" -> 0.05;
                default -> 0.0;
            };
            double withSeed7 = round5(0.3 + 0.6 * rand(7, "101", id) + bonus);
            double withSeed42 = round5(0.3 + 0.6 * rand(42, "101", id) + bonus);
            assertThat(item.get("similarityRate").asDouble()).isCloseTo(withSeed7, within(1e-9));
            assertThat(withSeed7).isNotEqualTo(withSeed42);
        }
    }

    @Test
    void 상한_3개는_전체_정렬의_상위_3개와_같다() throws Exception {
        ObjectNode request = MAPPER.createObjectNode();
        request.putObject("lostBoard").put("lostBoardId", "7");
        ArrayNode candidates = request.putArray("acquiredBoardList");
        for (int i = 1; i <= 40; i++) {
            candidates.addObject().put("acquiredBoardId", String.valueOf(i));
        }
        JsonNode list = postJson(mvc, "/matching/findear", request.toString()).get("result");

        List<double[]> all = new ArrayList<>();
        for (int id = 1; id <= 40; id++) {
            all.add(new double[]{id, round5(0.3 + 0.6 * rand(7, "7", String.valueOf(id)))});
        }
        all.sort(Comparator.comparingDouble((double[] a) -> a[1]).reversed());

        assertThat(list).hasSize(3);
        for (int i = 0; i < 3; i++) {
            assertThat(list.get(i).get("acquiredBoardId").asLong()).isEqualTo((long) all.get(i)[0]);
            assertThat(list.get(i).get("similarityRate").asDouble()).isCloseTo(all.get(i)[1], within(1e-9));
        }
    }

    @Test
    void lost도_상한을_따른다() throws Exception {
        JsonNode list = postJson(mvc, "/matching/lost", fixture("matching-lost-request.json")).get("result");
        assertThat(list).hasSize(3);
    }
}
