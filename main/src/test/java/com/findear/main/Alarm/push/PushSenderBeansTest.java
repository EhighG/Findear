package com.findear.main.Alarm.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * fcm.enabled에 따른 빈 구성 검증. 서비스계정 파일은 만들지 않는다: 없는 경로로 fail fast만 확인한다.
 */
class PushSenderBeansTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FcmConfig.class, NoopPushSender.class, FcmPushSender.class);

    @DisplayName("fcm.enabled가 없으면 NoopPushSender만 있고 FirebaseApp 빈은 없다")
    @Test
    void defaultIsNoop() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PushSender.class);
            assertThat(context.getBean(PushSender.class)).isInstanceOf(NoopPushSender.class);
            assertThat(context).doesNotHaveBean(FirebaseApp.class);
            assertThat(context).doesNotHaveBean(FirebaseMessaging.class);
        });
    }

    @DisplayName("fcm.enabled=false도 같다")
    @Test
    void falseIsNoop() {
        runner.withPropertyValues("fcm.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PushSender.class)).isInstanceOf(NoopPushSender.class);
            assertThat(context).doesNotHaveBean(FirebaseApp.class);
        });
    }

    @DisplayName("fcm.enabled=no/off/0처럼 스프링이 false로 읽는 값도 Noop이다")
    @Test
    void falseLikeValuesAreNoop() {
        for (String value : new String[]{"no", "off", "0", "FALSE"}) {
            runner.withPropertyValues("fcm.enabled=" + value).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(PushSender.class);
                assertThat(context.getBean(PushSender.class)).isInstanceOf(NoopPushSender.class);
                assertThat(context).doesNotHaveBean(FirebaseApp.class);
            });
        }
    }

    @DisplayName("fcm.enabled=yes/on/1처럼 true로 읽는 값이면 Fcm 쪽이 선택되어 FcmConfig가 동작한다 (없는 경로면 기동 실패)")
    @Test
    void trueLikeValuesSelectFcm() {
        for (String value : new String[]{"yes", "on", "1", "TRUE"}) {
            runner.withPropertyValues("fcm.enabled=" + value, "fcm.credentials-path=/no/such/dir/firebase-adminsdk.json")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        Throwable root = context.getStartupFailure();
                        while (root.getCause() != null) {
                            root = root.getCause();
                        }
                        assertThat(root).isInstanceOf(IllegalStateException.class);
                        assertThat(root.getMessage()).contains("/no/such/dir/firebase-adminsdk.json");
                    });

            new ApplicationContextRunner()
                    .withUserConfiguration(MockFirebaseMessagingConfig.class, NoopPushSender.class, FcmPushSender.class)
                    .withPropertyValues("fcm.enabled=" + value)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(PushSender.class);
                        assertThat(context.getBean(PushSender.class)).isInstanceOf(FcmPushSender.class);
                    });
        }
    }

    @DisplayName("boolean이 아닌 값(maybe)이면 기동이 실패하고 원인에 fcm.enabled가 나온다")
    @Test
    void nonBooleanValueFailsWithClearCause() {
        runner.withPropertyValues("fcm.enabled=maybe").run(context -> {
            assertThat(context).hasFailed();
            StringBuilder chain = new StringBuilder();
            for (Throwable t = context.getStartupFailure(); t != null; t = t.getCause()) {
                chain.append(t).append('\n');
            }
            assertThat(chain.toString()).contains("fcm.enabled").contains("maybe");
        });
    }

    @DisplayName("fcm.enabled=true + 없는 경로면 기동 실패하고 메시지에 경로가 나온다")
    @Test
    void trueWithMissingFileFailsFast() {
        runner.withPropertyValues("fcm.enabled=true", "fcm.credentials-path=/no/such/dir/firebase-adminsdk.json")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable root = context.getStartupFailure();
                    while (root.getCause() != null) {
                        root = root.getCause();
                    }
                    assertThat(root).isInstanceOf(IllegalStateException.class);
                    assertThat(root.getMessage())
                            .contains("/no/such/dir/firebase-adminsdk.json")
                            .contains("FCM_CREDENTIALS_PATH");
                });
    }

    @DisplayName("fcm.enabled=true + 경로가 비어 있으면 기동 실패")
    @Test
    void trueWithBlankPathFailsFast() {
        runner.withPropertyValues("fcm.enabled=true", "fcm.credentials-path=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
        });
    }

    @DisplayName("fcm.enabled=true면 FcmPushSender가 PushSender가 되고 Noop은 빠진다")
    @Test
    void trueSelectsFcmSender() {
        // Firebase 초기화(FcmConfig)는 빼고 FirebaseMessaging만 mock으로 제공
        new ApplicationContextRunner()
                .withUserConfiguration(MockFirebaseMessagingConfig.class, NoopPushSender.class, FcmPushSender.class)
                .withPropertyValues("fcm.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(PushSender.class);
                    assertThat(context.getBean(PushSender.class)).isInstanceOf(FcmPushSender.class);
                });
    }

    @Configuration
    static class MockFirebaseMessagingConfig {
        @Bean
        FirebaseMessaging firebaseMessaging() {
            return mock(FirebaseMessaging.class);
        }
    }
}
