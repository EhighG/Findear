package com.findear.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 테스트 공통 도우미: 픽스처 읽기, POST 호출, 기대 점수 계산(구현과 별도로 작성한 공식). */
public final class ApiTestSupport {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiTestSupport() {
    }

    public static String fixture(String name) {
        try (InputStream in = ApiTestSupport.class.getResourceAsStream("/contracts/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("픽스처가 없습니다: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static JsonNode json(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static MvcResult post(MockMvc mvc, String path, String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andReturn();
    }

    public static String text(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    public static JsonNode postJson(MockMvc mvc, String path, String body) throws Exception {
        return json(text(post(mvc, path, body)));
    }

    /** 스펙(07 §5.4)의 rand(seed, lostBoardId, key). */
    public static double rand(long seed, String lostBoardId, String key) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256")
                .digest((seed + "|" + lostBoardId + "|" + key).getBytes(StandardCharsets.UTF_8));
        long v = ByteBuffer.wrap(h).getLong();
        return (v >>> 11) * 0x1.0p-53;
    }

    /** [0,1]로 자르고 소수 5자리 HALF_UP 반올림. */
    public static double round5(double raw) {
        double clamped = Math.max(0.0, Math.min(1.0, raw));
        return new BigDecimal(Double.toString(clamped)).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }
}
