package com.findear.main.Alarm.service;

import com.findear.main.Alarm.common.domain.Notification;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.push.PushMessage;
import com.findear.main.Alarm.push.PushResult;
import com.findear.main.Alarm.push.PushSender;
import com.findear.main.Alarm.repository.AlarmRepository;
import com.findear.main.Alarm.repository.NotificationRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.repository.MemberQueryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 푸시 발송이 알림을 저장한 트랜잭션의 커밋 뒤에 일어나는지 검증한다.
 * 진짜 스프링 컨텍스트(@EnableTransactionManagement, @TransactionalEventListener)에 DB 없는 가짜 트랜잭션 매니저를 쓴다.
 * 리포지토리·PushSender는 mock이라 DB와 Firebase는 쓰지 않는다.
 */
class PushDispatchTransactionTest {

    /** 커밋·롤백 이력만 남기는 가짜 매니저. 트랜잭션 동기화(afterCommit 콜백)는 AbstractPlatformTransactionManager가 처리한다. */
    // JpaTransactionManager처럼 커밋 뒤 콜백(afterCommit) 동안에도 기존 트랜잭션이 살아 있는 것으로 본다.
    // 그래야 REQUIRED로 바꾸면 기존 트랜잭션에 참여(begin 없음)하고, REQUIRES_NEW면 suspend → begin → resume이 기록된다.
    static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        final List<String> log = new ArrayList<>();
        private boolean active;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return active;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active = true;
            log.add("begin");
        }

        @Override
        protected Object doSuspend(Object transaction) {
            active = false;
            log.add("suspend");
            return Boolean.TRUE;
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            active = true;
            log.add("resume");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            log.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            log.add("rollback");
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            active = false;
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
        NotificationRepository notificationRepository() {
            return mock(NotificationRepository.class);
        }

        @Bean
        MemberQueryRepository memberQueryRepository() {
            return mock(MemberQueryRepository.class);
        }

        @Bean
        AlarmRepository alarmRepository() {
            return mock(AlarmRepository.class);
        }

        @Bean
        EntityManager entityManager() {
            return mock(EntityManager.class);
        }

        @Bean
        PushSender pushSender() {
            return mock(PushSender.class);
        }

        @Bean
        NotificationService notificationService(NotificationRepository n, MemberQueryRepository m, AlarmRepository a,
                                                ApplicationEventPublisher publisher, EntityManager em) {
            return new NotificationService(n, m, a, publisher, em);
        }

        @Bean
        PushDispatchListener pushDispatchListener(PushSender pushSender, NotificationService notificationService) {
            return new PushDispatchListener(pushSender, notificationService);
        }
    }

    private AnnotationConfigApplicationContext context;
    private RecordingTransactionManager txManager;
    private NotificationService notificationService;
    private PushSender pushSender;
    private NotificationRepository notificationRepository;
    private TransactionTemplate tx;
    private Notification stored;

    private final NotificationRequestDto request = NotificationRequestDto.builder()
            .title("쪽지 도착").message("쪽지가 도착했습니다.").type("message").memberId(1L).build();

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        txManager = context.getBean(RecordingTransactionManager.class);
        notificationService = context.getBean(NotificationService.class);
        pushSender = context.getBean(PushSender.class);
        notificationRepository = context.getBean(NotificationRepository.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        Member member = Member.builder().naverUid("uid").phoneNumber("010-0000-0000").role(Role.NORMAL).build();
        when(context.getBean(MemberQueryRepository.class).findById(1L)).thenReturn(Optional.of(member));
        stored = Notification.builder().token("tok").build();
        stored.confirmUser(member);
        when(notificationRepository.findByMember(member)).thenReturn(stored);
        when(notificationRepository.findByMemberId(1L)).thenReturn(stored);
        when(pushSender.send(any())).thenReturn(PushResult.SENT);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @DisplayName("(a) 트랜잭션 안에서 호출하면 커밋 전에는 발송하지 않고 커밋 뒤 1회 발송한다")
    @Test
    void sendsAfterCommit() {
        tx.executeWithoutResult(status -> {
            notificationService.sendNotification(request);
            verify(pushSender, never()).send(any());
        });

        verify(pushSender).send(new PushMessage(1L, "tok", "쪽지 도착", "쪽지가 도착했습니다.:message"));
        assertThat(txManager.log).containsExactly("begin", "commit");
    }

    @DisplayName("(b) 바깥 트랜잭션이 롤백되면 발송하지 않는다")
    @Test
    void noSendOnRollback() {
        tx.executeWithoutResult(status -> {
            notificationService.sendNotification(request);
            status.setRollbackOnly();
        });

        verify(pushSender, never()).send(any());
        assertThat(txManager.log).containsExactly("begin", "rollback");
    }

    @DisplayName("(b 보충) 바깥 트랜잭션에서 예외로 롤백돼도 발송하지 않는다")
    @Test
    void noSendOnException() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            notificationService.sendNotification(request);
            throw new IllegalStateException("이후 로직 실패");
        })).isInstanceOf(IllegalStateException.class);

        verify(pushSender, never()).send(any());
    }

    @DisplayName("(c) 트랜잭션 밖에서 호출하면 즉시 1회 발송한다")
    @Test
    void sendsImmediatelyWithoutTransaction() {
        notificationService.sendNotification(request);

        verify(pushSender).send(any());
        assertThat(txManager.log).isEmpty();
    }

    @DisplayName("(d) UNREGISTERED로 토큰 삭제가 예외를 던져도 바깥 트랜잭션은 정상 커밋된다")
    @Test
    void tokenDeleteFailureDoesNotBreakOuterTransaction() {
        when(pushSender.send(any())).thenReturn(PushResult.TOKEN_INVALID);
        doThrow(new RuntimeException("db down")).when(notificationRepository).delete(stored);

        tx.executeWithoutResult(status -> notificationService.sendNotification(request));

        verify(pushSender).send(any());
        verify(notificationRepository).delete(stored);
        // 바깥: begin, commit. 토큰 삭제는 커밋 뒤 콜백에서 바깥을 잠시 멈추고(suspend) 별도(REQUIRES_NEW) 트랜잭션으로 연다
        assertThat(txManager.log).containsExactly("begin", "commit", "suspend", "begin", "rollback", "resume");
    }

    @DisplayName("UNREGISTERED면 커밋 뒤 별도 트랜잭션에서 토큰을 삭제한다")
    @Test
    void invalidTokenDeletedInNewTransactionAfterCommit() {
        when(pushSender.send(any())).thenReturn(PushResult.TOKEN_INVALID);

        tx.executeWithoutResult(status -> {
            notificationService.sendNotification(request);
            verify(notificationRepository, never()).delete(any());
        });

        verify(notificationRepository).delete(stored);
        assertThat(txManager.log).containsExactly("begin", "commit", "suspend", "begin", "commit", "resume");
    }

    @DisplayName("FAILED(다른 오류)면 토큰을 유지하고, PushSender가 예외를 던져도 요청 흐름은 성공한다")
    @Test
    void failedKeepsTokenAndSenderExceptionIsSwallowed() {
        when(pushSender.send(any())).thenReturn(PushResult.FAILED);
        tx.executeWithoutResult(status -> notificationService.sendNotification(request));
        verify(notificationRepository, never()).delete(any());

        reset(pushSender);
        when(pushSender.send(any())).thenThrow(new RuntimeException("boom"));
        tx.executeWithoutResult(status -> notificationService.sendNotification(request));
        verify(notificationRepository, never()).delete(any());
    }
}
