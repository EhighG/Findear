package com.findear.batch.support;

import mockwebserver3.MockResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** src/test/resources/lost112/ 의 응답 픽스처 (공공데이터포털 표준 구조 가정, 05 §2·§8). 실제 응답이 아니다 */
public final class Lost112Fixtures {

    private Lost112Fixtures() {
    }

    public static byte[] bytes(String name) {
        try (InputStream in = Lost112Fixtures.class.getResourceAsStream("/lost112/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("픽스처 없음: " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String text(String name) {
        return new String(bytes(name), StandardCharsets.UTF_8);
    }

    public static MockResponse xml(String fixtureName) {
        return xml(200, text(fixtureName));
    }

    public static MockResponse xml(int code, String body) {
        return new MockResponse.Builder().code(code).setHeader("Content-Type", "application/xml;charset=UTF-8").body(body).build();
    }
}
