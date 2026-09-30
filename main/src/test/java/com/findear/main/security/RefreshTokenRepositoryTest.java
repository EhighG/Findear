package com.findear.main.security;

import com.findear.main.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** refresh token은 redis-cli에서 읽을 수 있는 문자열 키 refresh:{memberId}로 저장된다 (TTL 1439분). 실제 Redis(Testcontainers) 사용. */
@IntegrationTest
class RefreshTokenRepositoryTest {

    private static final Long MEMBER_ID = 987_654L;

    @Autowired RefreshTokenRepository repository;
    @Autowired StringRedisTemplate redis;

    @AfterEach
    void clean() {
        repository.deleteRefreshToken(MEMBER_ID);
    }

    @Test
    @DisplayName("키는 refresh:{memberId}, 값은 토큰 문자열, TTL은 1439분")
    void savesWithReadableKeyAndTtl() {
        repository.save(MEMBER_ID, "token-value");

        assertThat(redis.opsForValue().get("refresh:987654")).isEqualTo("token-value");
        assertThat(redis.keys("refresh:*")).contains("refresh:987654");
        assertThat(redis.getExpire("refresh:987654", TimeUnit.SECONDS)).isBetween(86_300L, 86_340L);
        assertThat(repository.findByMemberId(MEMBER_ID)).contains("token-value");
    }

    @Test
    @DisplayName("삭제하면 조회되지 않는다")
    void deleteRemovesKey() {
        repository.save(MEMBER_ID, "token-value");
        repository.deleteRefreshToken(MEMBER_ID);

        assertThat(repository.findByMemberId(MEMBER_ID)).isEmpty();
        assertThat(redis.hasKey("refresh:987654")).isFalse();
    }
}
