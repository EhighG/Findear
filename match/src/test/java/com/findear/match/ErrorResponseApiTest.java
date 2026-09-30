package com.findear.match;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static com.findear.match.ApiTestSupport.json;
import static com.findear.match.ApiTestSupport.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class ErrorResponseApiTest {

    @Autowired
    MockMvc mvc;

    private void assertMessageShape(MvcResult result, int status) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        JsonNode error = json(text(result));
        assertThat(error.size()).isEqualTo(1);
        assertThat(error.get("message").asText()).isNotBlank();
    }

    @Test
    void 없는_경로는_404() throws Exception {
        assertMessageShape(mvc.perform(get("/nothing")).andReturn(), 404);
        assertMessageShape(mvc.perform(post("/matching/none").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn(), 404);
    }

    @Test
    void GET은_405() throws Exception {
        assertMessageShape(mvc.perform(get("/process")).andReturn(), 405);
        assertMessageShape(mvc.perform(get("/matching/findear")).andReturn(), 405);
        assertMessageShape(mvc.perform(get("/matching/lost")).andReturn(), 405);
    }

    @Test
    void text_plain은_415() throws Exception {
        assertMessageShape(mvc.perform(post("/process").contentType(MediaType.TEXT_PLAIN)
                .content("hello".getBytes(StandardCharsets.UTF_8))).andReturn(), 415);
        assertMessageShape(mvc.perform(post("/matching/lost").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andReturn(), 415);
    }

    @Test
    void Content_Type_없이_보내도_415() throws Exception {
        assertMessageShape(mvc.perform(post("/process").content("{}")).andReturn(), 415);
    }

    @Test
    void Accept가_json이_아니어도_오류는_message_모양() throws Exception {
        MvcResult result = mvc.perform(post("/process").accept(MediaType.TEXT_PLAIN)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(text(result)).get("message").asText()).isNotBlank();
    }
}
