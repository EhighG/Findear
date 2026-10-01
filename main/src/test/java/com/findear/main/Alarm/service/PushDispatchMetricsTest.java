package com.findear.main.Alarm.service;

import com.findear.main.Alarm.push.PushMessage;
import com.findear.main.Alarm.push.PushRequestedEvent;
import com.findear.main.Alarm.push.PushResult;
import com.findear.main.Alarm.push.PushSender;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FCM 발송 결과 카운터(findear.fcm.send, Prometheus: findear_fcm_send_total) 검증.
 * 결과는 PushSender 구현체와 관계없이 PushDispatchListener 한 곳에서 센다. Firebase는 쓰지 않는다.
 */
class PushDispatchMetricsTest {

    private MeterRegistry registry;
    private PushSender pushSender;
    private NotificationService notificationService;
    private PushDispatchListener listener;
    private final PushRequestedEvent event = new PushRequestedEvent(new PushMessage(1L, "tok", "제목", "본문"));

    @BeforeEach
    void setup() {
        registry = new SimpleMeterRegistry();
        pushSender = mock(PushSender.class);
        notificationService = mock(NotificationService.class);
        listener = new PushDispatchListener(pushSender, notificationService, registry);
    }

    private double count(String result) {
        return registry.get("findear.fcm.send").tag("result", result).counter().count();
    }

    @DisplayName("기동 직후 네 가지 결과가 모두 0으로 등록되어 있다")
    @Test
    void allResultsRegisteredAtZero() {
        assertThat(registry.find("findear.fcm.send").counters()).hasSize(4);
        for (String result : new String[]{"sent", "skipped", "token_invalid", "failed"}) {
            assertThat(count(result)).isZero();
        }
    }

    @DisplayName("결과마다 해당 태그의 카운터만 1 늘어난다")
    @Test
    void incrementsOnlyMatchingResult() {
        for (PushResult result : PushResult.values()) {
            when(pushSender.send(any())).thenReturn(result);
            listener.onPushRequested(event);
        }

        for (String result : new String[]{"sent", "skipped", "token_invalid", "failed"}) {
            assertThat(count(result)).as(result).isEqualTo(1.0);
        }

        when(pushSender.send(any())).thenReturn(PushResult.SKIPPED);
        listener.onPushRequested(event);
        assertThat(count("skipped")).isEqualTo(2.0);
        assertThat(count("sent")).isEqualTo(1.0);
    }

    @DisplayName("PushSender가 예외를 던져 결과를 못 얻으면 failed로 센다 (예외는 밖으로 나가지 않는다)")
    @Test
    void senderExceptionCountsAsFailed() {
        when(pushSender.send(any())).thenThrow(new RuntimeException("boom"));

        listener.onPushRequested(event);

        assertThat(count("failed")).isEqualTo(1.0);
        assertThat(count("sent")).isZero();
    }

    @DisplayName("토큰 삭제가 실패해도 발송 결과(token_invalid)는 그대로 센다")
    @Test
    void tokenDeleteFailureKeepsResultCount() {
        when(pushSender.send(any())).thenReturn(PushResult.TOKEN_INVALID);
        org.mockito.Mockito.doThrow(new RuntimeException("db down")).when(notificationService).deleteInvalidToken(any(), any());

        listener.onPushRequested(event);

        assertThat(count("token_invalid")).isEqualTo(1.0);
        assertThat(count("failed")).isZero();
    }

    @DisplayName("태그는 result 하나뿐이다 (회원·토큰 같은 값을 태그로 쓰지 않는다)")
    @Test
    void onlyResultTag() {
        assertThat(registry.find("findear.fcm.send").counters())
                .allSatisfy(counter -> assertThat(counter.getId().getTags()).hasSize(1));
    }
}
