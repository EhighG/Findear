package com.findear.match;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Findear match mock 서버 (P1, D-28).
 * 팀 시절 Django match 서버(OpenAI·fastText)를 복구하지 않고, 같은 API 경로·JSON 형태를 결정적 점수로 흉내 낸다.
 */
@SpringBootApplication
public class MatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchApplication.class, args);
    }
}
