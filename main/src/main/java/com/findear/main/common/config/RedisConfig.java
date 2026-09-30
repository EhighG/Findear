package com.findear.main.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

// 연결 팩토리와 StringRedisTemplate은 Boot 자동 설정(spring.data.redis.host/port/password)이 만든다
@Configuration
@EnableRedisRepositories
public class RedisConfig {
}
