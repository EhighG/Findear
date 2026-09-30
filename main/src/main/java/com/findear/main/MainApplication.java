package com.findear.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
// 사용자 이름·비밀번호 로그인을 쓰지 않으므로(JWT 필터만 사용) 기동 때 임시 비밀번호를 만드는 UserDetailsService 자동 설정을 끈다
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

}
