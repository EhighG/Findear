package com.findear.match;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.findear.match.ApiTestSupport.fixture;
import static com.findear.match.ApiTestSupport.json;
import static com.findear.match.ApiTestSupport.post;
import static com.findear.match.ApiTestSupport.postJson;
import static com.findear.match.ApiTestSupport.text;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class ProcessApiTest {

    static final List<String> CATEGORIES = List.of("카드", "지갑", "현금", "의류", "전자기기", "가방", "휴대폰", "증명서", "쇼핑백",
            "귀금속", "유가증권", "자동차", "서류", "도서용품", "스포츠용품", "컴퓨터", "산업용품", "악기", "기타");
    static final List<String> COLORS = List.of("검정", "흰", "빨강", "오렌지", "노랑", "초록", "파랑", "갈", "보라", "회", "기타");

    @Autowired
    MockMvc mvc;

    @Test
    void 픽스처_요청은_계약_모양으로_응답한다() throws Exception {
        var result = post(mvc, "/process", fixture("process-request.json"));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        JsonNode body = json(text(result));
        assertThat(body.get("message").asText()).isEqualTo("success");
        JsonNode r = body.get("result");
        assertThat(CATEGORIES).contains(r.get("category").asText());
        assertThat(COLORS).contains(r.get("color").asText());

        JsonNode description = r.get("description");
        assertThat(description.isArray()).isTrue();
        assertThat(description).hasSize(5);
        Set<String> keywords = new HashSet<>();
        description.forEach(n -> {
            assertThat(n.asText()).isNotEmpty().doesNotContainPattern("(?U)\\s");
            keywords.add(n.asText());
        });
        assertThat(keywords).hasSize(5);
        // productName "검정 가죽 지갑"의 토큰이 앞에 들어간다
        assertThat(description.get(0).asText()).isEqualTo("검정");
        assertThat(description.get(1).asText()).isEqualTo("가죽");
        assertThat(description.get(2).asText()).isEqualTo("지갑");
    }

    @Test
    void 긴_상품명은_토큰_5개까지만_쓴다() throws Exception {
        String body = "{\"productName\":\" a  b c d e f g \",\"imgUrl\":\"http://x/y.jpg\"}";
        JsonNode description = postJson(mvc, "/process", body).get("result").get("description");
        assertThat(description).hasSize(5);
        assertThat(description.get(0).asText()).isEqualTo("a");
        assertThat(description.get(4).asText()).isEqualTo("e");
    }

    @Test
    void 같은_요청은_같은_응답() throws Exception {
        String first = text(post(mvc, "/process", fixture("process-request.json")));
        String second = text(post(mvc, "/process", fixture("process-request.json")));
        assertThat(second).isEqualTo(first);
    }

    @Test
    void 모르는_필드는_무시하고_숫자도_문자열로_받는다() throws Exception {
        var result = post(mvc, "/process", "{\"productName\":123,\"imgUrl\":\"http://x/y.jpg\",\"extra\":true}");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(text(result)).get("result").get("description").get(0).asText()).isEqualTo("123");
    }

    @Test
    void 잘못된_요청은_400_message_한_가지_모양() throws Exception {
        List<String> bodies = List.of(
                "{\"imgUrl\":\"http://x/y.jpg\"}",
                "{\"productName\":\"지갑\",\"imgUrl\":\"   \"}",
                "{\"productName\":\"  \",\"imgUrl\":\"http://x/y.jpg\"}",
                "{\"productName\":\"지갑\",\"imgUrl\":null}",
                "{\"productName\": \"지갑\", ",
                "[{\"productName\":\"지갑\",\"imgUrl\":\"http://x/y.jpg\"}]",
                "\"문자열\"",
                "{\"productName\":{\"a\":1},\"imgUrl\":\"http://x/y.jpg\"}",
                "");
        for (String body : bodies) {
            var result = post(mvc, "/process", body);
            assertThat(result.getResponse().getStatus()).as(body).isEqualTo(400);
            JsonNode error = json(text(result));
            assertThat(error.size()).as(body).isEqualTo(1);
            assertThat(error.get("message").asText()).as(body).isNotBlank();
        }
    }
}
