package com.findear.main.Alarm.service;

import com.findear.main.Alarm.push.PushRequestedEvent;
import com.findear.main.Alarm.push.PushResult;
import com.findear.main.Alarm.push.PushSender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 푸시 발송을 알림 저장 트랜잭션이 커밋된 뒤에 한다 (Spring "Transaction-bound Events").
 * - 바깥 트랜잭션이 롤백되면 이 리스너는 호출되지 않아 발송도 없다.
 * - FCM 네트워크 대기 동안 DB 트랜잭션을 잡고 있지 않는다.
 * - 트랜잭션 없이 발행된 이벤트(분실물 매칭 알림의 WebClient 콜백 등)는 fallbackExecution으로 바로 실행한다.
 * 어떤 예외도 발행한 쪽(요청 스레드)으로 나가지 않는다.
 *
 * 발송 결과는 발송 구현체와 관계없이 이곳 한 곳에서 `findear.fcm.send{result=sent|skipped|token_invalid|failed}`
 * 카운터(Prometheus: findear_fcm_send_total)로 센다. 결과를 얻지 못한 예외는 failed다. 기동 때 네 값을 0으로 미리 등록한다.
 */
@Slf4j
@Component
public class PushDispatchListener {

    static final String METRIC_NAME = "findear.fcm.send";

    private final PushSender pushSender;
    private final NotificationService notificationService;
    private final Map<PushResult, Counter> sendCounters = new EnumMap<>(PushResult.class);

    public PushDispatchListener(PushSender pushSender, NotificationService notificationService, MeterRegistry meterRegistry) {
        this.pushSender = pushSender;
        this.notificationService = notificationService;
        for (PushResult result : PushResult.values()) {
            sendCounters.put(result, Counter.builder(METRIC_NAME)
                    .description("FCM 푸시 발송 결과 수")
                    .tag("result", result.name().toLowerCase(Locale.ROOT))
                    .register(meterRegistry));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPushRequested(PushRequestedEvent event) {
        try {
            PushResult result;
            try {
                result = pushSender.send(event.message());
            } catch (RuntimeException e) {
                sendCounters.get(PushResult.FAILED).increment();
                throw e;
            }
            sendCounters.get(result == null ? PushResult.FAILED : result).increment();

            if (result == PushResult.TOKEN_INVALID) {
                // AFTER_COMMIT 콜백의 데이터 접근은 원래(이미 커밋된) 트랜잭션 자원에 참여하므로 별도 트랜잭션(REQUIRES_NEW)으로 지운다
                notificationService.deleteInvalidToken(event.message().memberId(), event.message().token());
            }
        } catch (Exception e) {
            log.warn("푸시 발송 후처리 실패: memberId={}, {}", event.message().memberId(), e.toString());
        }
    }
}
