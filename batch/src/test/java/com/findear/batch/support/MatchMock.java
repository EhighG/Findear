package com.findear.batch.support;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * match 서버를 흉내 내는 mock HTTP 서버 (D-38: 실제 match·외부 API는 호출하지 않는다).
 * 요청을 모두 기록하고, 테스트가 정한 핸들러로 응답한다. 테스트마다 reset()으로 비운다.
 */
public class MatchMock {

    private static final Function<RecordedRequest, MockResponse> NO_HANDLER =
            request -> json(404, "{\"message\":\"핸들러 없음\"}");

    private final MockWebServer server = new MockWebServer();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private volatile Function<RecordedRequest, MockResponse> handler = NO_HANDLER;

    public MatchMock() {
        server.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                requests.add(request);
                return handler.apply(request);
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
        }));
    }

    public String url() {
        String url = server.url("/").toString();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public void reset() {
        requests.clear();
        handler = NO_HANDLER;
    }

    public void respondWith(Function<RecordedRequest, MockResponse> handler) {
        this.handler = handler;
    }

    public List<RecordedRequest> requests() {
        return new ArrayList<>(requests);
    }

    public static MockResponse json(int code, String body) {
        return new MockResponse.Builder().code(code).setHeader("Content-Type", "application/json").body(body).build();
    }
}
