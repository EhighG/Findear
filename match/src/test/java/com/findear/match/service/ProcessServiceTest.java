package com.findear.match.service;

import com.findear.match.dto.ProcessRequest;
import com.findear.match.dto.ProcessResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessServiceTest {

    @Test
    void seed를_바꾸면_결과가_전부_같지는_않다() {
        ProcessService a = new ProcessService(42);
        ProcessService b = new ProcessService(43);
        int different = 0;
        for (int i = 0; i < 20; i++) {
            ProcessRequest request = new ProcessRequest("물건" + i, "http://x/" + i + ".jpg");
            ProcessResponse.Result ra = a.process(request).result();
            ProcessResponse.Result rb = b.process(request).result();
            if (!ra.equals(rb)) {
                different++;
            }
        }
        assertThat(different).isGreaterThan(10);
    }

    @Test
    void 모든_입력에서_키워드는_5개_서로_다르고_공백이_없다() {
        ProcessService service = new ProcessService(42);
        for (int i = 0; i < 200; i++) {
            List<String> tokens = new ArrayList<>();
            for (int t = 0; t < i % 8; t++) {
                tokens.add(t % 3 == 0 && t > 0 ? "소형" : "토큰" + i + "_" + t); // 풀 단어와 겹치는 토큰도 섞는다
            }
            List<String> description = service.process(
                    new ProcessRequest(String.join(" ", tokens) + " 지갑", "http://x/" + i)).result().description();
            assertThat(description).hasSize(5);
            assertThat(new HashSet<>(description)).hasSize(5);
            assertThat(description).allSatisfy(k -> assertThat(k).isNotEmpty().doesNotContainPattern("(?U)\\s"));
        }
    }

    @Test
    void 전각_공백도_토큰_구분자로_본다() {
        List<String> description = new ProcessService(1)
                .process(new ProcessRequest("검정　지갑", "u")).result().description();
        assertThat(description.subList(0, 2)).containsExactly("검정", "지갑");
    }
}
