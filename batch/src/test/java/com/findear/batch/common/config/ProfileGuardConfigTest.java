package com.findear.batch.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * local과 prod가 함께 켜지면 기동 실패 (D-60). 프로필 조건이 실제로 평가되는지 환경의 활성 프로필도 같이 확인한다.
 */
class ProfileGuardConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProfileGuardConfig.class);

    @DisplayName("local,prod를 함께 켜면 기동이 실패하고 원인에 안내 메시지가 나온다")
    @Test
    void localAndProdFails() {
        runner.withPropertyValues("spring.profiles.active=local,prod").run(context -> {
            assertThat(context).hasFailed();
            Throwable root = context.getStartupFailure();
            while (root.getCause() != null) {
                root = root.getCause();
            }
            assertThat(root).isInstanceOf(IllegalStateException.class);
            assertThat(root.getMessage())
                    .contains("local과 prod 프로필을 함께 켤 수 없습니다")
                    .contains("SPRING_PROFILES_ACTIVE");
        });
    }

    @DisplayName("prod만 켜면 기동에 성공하고 가드 빈은 없다")
    @Test
    void prodOnlyStarts() {
        runner.withPropertyValues("spring.profiles.active=prod").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("prod");
            assertThat(context).doesNotHaveBean(ProfileGuardConfig.class);
        });
    }

    @DisplayName("local만 켜면 기동에 성공하고 가드 빈은 없다")
    @Test
    void localOnlyStarts() {
        runner.withPropertyValues("spring.profiles.active=local").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("local");
            assertThat(context).doesNotHaveBean(ProfileGuardConfig.class);
        });
    }
}
