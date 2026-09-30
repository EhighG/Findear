package com.findear.match;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static com.findear.match.ApiTestSupport.fixture;
import static com.findear.match.ApiTestSupport.post;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "match.mock.latency-ms=300")
@AutoConfigureMockMvc
class LatencyTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 지연_설정만큼_응답이_늦어진다() throws Exception {
        long start = System.nanoTime();
        var result = post(mvc, "/process", fixture("process-request.json"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(elapsedMs).isGreaterThanOrEqualTo(300);
    }
}
