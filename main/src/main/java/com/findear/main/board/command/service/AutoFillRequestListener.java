package com.findear.main.board.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 습득물 자동채움 요청을 등록 트랜잭션이 커밋된 뒤에 match로 보낸다 (Spring "Transaction-bound Events", D-52).
 * - 등록이 롤백되면 이 리스너는 호출되지 않아 요청도 없다.
 * - 트랜잭션 없이 발행된 이벤트는 fallbackExecution으로 바로 실행한다.
 * - 요청은 비동기라 요청 스레드를 기다리게 하지 않고, 어떤 예외도 발행한 쪽(요청 스레드)으로 나가지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoFillRequestListener {

    private final MatchAutoFillClient matchAutoFillClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAutoFillRequested(AutoFillRequestedEvent event) {
        try {
            matchAutoFillClient.requestAutoFill(event);
        } catch (Exception e) {
            log.warn("습득물 자동채움 요청 실패: boardId={}, {}", event.boardId(), e.toString());
        }
    }
}
