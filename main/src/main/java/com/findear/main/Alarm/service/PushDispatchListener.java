package com.findear.main.Alarm.service;

import com.findear.main.Alarm.push.PushRequestedEvent;
import com.findear.main.Alarm.push.PushResult;
import com.findear.main.Alarm.push.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 푸시 발송을 알림 저장 트랜잭션이 커밋된 뒤에 한다 (Spring "Transaction-bound Events").
 * - 바깥 트랜잭션이 롤백되면 이 리스너는 호출되지 않아 발송도 없다.
 * - FCM 네트워크 대기 동안 DB 트랜잭션을 잡고 있지 않는다.
 * - 트랜잭션 없이 발행된 이벤트(분실물 매칭 알림의 WebClient 콜백 등)는 fallbackExecution으로 바로 실행한다.
 * 어떤 예외도 발행한 쪽(요청 스레드)으로 나가지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDispatchListener {

    private final PushSender pushSender;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPushRequested(PushRequestedEvent event) {
        try {
            PushResult result = pushSender.send(event.message());

            if (result == PushResult.TOKEN_INVALID) {
                // AFTER_COMMIT 콜백의 데이터 접근은 원래(이미 커밋된) 트랜잭션 자원에 참여하므로 별도 트랜잭션(REQUIRES_NEW)으로 지운다
                notificationService.deleteInvalidToken(event.message().memberId(), event.message().token());
            }
        } catch (Exception e) {
            log.warn("푸시 발송 후처리 실패: memberId={}, {}", event.message().memberId(), e.toString());
        }
    }
}
