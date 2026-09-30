package com.findear.main.board.command.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * match 자동채움 요청이 등록 트랜잭션의 커밋 뒤에 나가는지 검증한다 (D-52).
 * 진짜 스프링 컨텍스트(@EnableTransactionManagement, @TransactionalEventListener)에 DB 없는 가짜 트랜잭션 매니저를 쓰고,
 * match 클라이언트는 mock이라 네트워크를 쓰지 않는다 (PushDispatchTransactionTest와 같은 방식).
 */
class AutoFillRequestListenerTest {

    /** 커밋·롤백 이력만 남기는 가짜 매니저. 트랜잭션 동기화(afterCommit 콜백)는 AbstractPlatformTransactionManager가 처리한다. */
    static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        final List<String> log = new ArrayList<>();

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            log.add("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            log.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            log.add("rollback");
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        RecordingTransactionManager transactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        MatchAutoFillClient matchAutoFillClient() {
            return mock(MatchAutoFillClient.class);
        }

        @Bean
        AutoFillRequestListener autoFillRequestListener(MatchAutoFillClient client) {
            return new AutoFillRequestListener(client);
        }
    }

    private static final AutoFillRequestedEvent EVENT = new AutoFillRequestedEvent(
            7L, "검정 가죽 지갑", "http://localhost:8333/findear-images/images/2026/10/a.png");

    private AnnotationConfigApplicationContext context;
    private RecordingTransactionManager txManager;
    private MatchAutoFillClient client;
    private TransactionTemplate tx;

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        txManager = context.getBean(RecordingTransactionManager.class);
        client = context.getBean(MatchAutoFillClient.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @DisplayName("트랜잭션 안에서 발행하면 커밋 전에는 요청하지 않고 커밋 뒤 정확히 1번 요청한다")
    @Test
    void requestsAfterCommit() {
        tx.executeWithoutResult(status -> {
            context.publishEvent(EVENT);
            verify(client, never()).requestAutoFill(EVENT);
        });

        verify(client, times(1)).requestAutoFill(EVENT);
        assertThat(txManager.log).containsExactly("begin", "commit");
    }

    @DisplayName("롤백되면 요청하지 않는다")
    @Test
    void noRequestOnRollback() {
        tx.executeWithoutResult(status -> {
            context.publishEvent(EVENT);
            status.setRollbackOnly();
        });

        verify(client, never()).requestAutoFill(EVENT);
        assertThat(txManager.log).containsExactly("begin", "rollback");
    }

    @DisplayName("예외로 롤백돼도 요청하지 않는다")
    @Test
    void noRequestOnException() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            context.publishEvent(EVENT);
            throw new IllegalStateException("이후 로직 실패");
        })).isInstanceOf(IllegalStateException.class);

        verify(client, never()).requestAutoFill(EVENT);
    }

    @DisplayName("트랜잭션 없이 발행하면 바로 1번 요청한다")
    @Test
    void requestsImmediatelyWithoutTransaction() {
        context.publishEvent(EVENT);

        verify(client, times(1)).requestAutoFill(EVENT);
        assertThat(txManager.log).isEmpty();
    }

    @DisplayName("클라이언트가 예외를 던져도 발행한 쪽(등록 흐름)으로 나가지 않고 커밋된다")
    @Test
    void clientFailureDoesNotPropagate() {
        doThrow(new IllegalStateException("boom")).when(client).requestAutoFill(EVENT);

        assertThatCode(() -> tx.executeWithoutResult(status -> context.publishEvent(EVENT))).doesNotThrowAnyException();
        assertThat(txManager.log).containsExactly("begin", "commit");
    }
}
