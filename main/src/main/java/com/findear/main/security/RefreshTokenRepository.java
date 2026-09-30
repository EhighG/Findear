package com.findear.main.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * refresh token 저장소 (Redis). 키는 {@code refresh:{memberId}}, 값은 refresh token 문자열, TTL 1439분 (06-db-and-config §5).
 * 키·값 모두 문자열로 저장하므로 redis-cli에서 그대로 읽을 수 있다.
 */
@Repository
public class RefreshTokenRepository {

    static final String KEY_PREFIX = "refresh:";
    static final Duration TTL = Duration.ofMinutes((60 * 24) - 1);

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    static String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }

    public void save(Long memberId, String refreshToken) {
        redisTemplate.opsForValue().set(key(memberId), refreshToken, TTL);
    }

    public void deleteRefreshToken(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    public Optional<String> findByMemberId(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(memberId)));
    }
}
