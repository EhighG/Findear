package com.findear.main.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RefreshTokenRepository {

    private RedisTemplate<Long, String> redisTemplate;

    public RefreshTokenRepository(RedisTemplate<Long, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long memberId, String refreshToken) {
        ValueOperations<Long, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(memberId, refreshToken);
        redisTemplate.expire(memberId, (60 * 24) - 1, TimeUnit.MINUTES);
    }

    public void deleteRefreshToken(Long memberId) {
        redisTemplate.delete(memberId);
    }

    public Optional<String> findByMemberId(Long memberId) {
        ValueOperations<Long, String> valueOperations = redisTemplate.opsForValue();
        String refreshToken = valueOperations.get(memberId);

        if (refreshToken == null) {
            return Optional.empty();
        }

        return Optional.of(refreshToken);
    }
}
