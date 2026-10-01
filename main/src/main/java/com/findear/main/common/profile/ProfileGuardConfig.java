package com.findear.main.common.profile;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * local과 prod 프로필이 함께 켜지면 기동에 실패하게 한다 (D-60).
 * 개발용 기능(전화번호 로그인, 테스트 가입, 알림 테스트 발송, test-member-type 헤더 인증, D-26)은 local 프로필이 켜져 있으면 열리므로,
 * 배포(prod)에서 실수로 local이 같이 켜지는 일을 조용히 넘기지 않고 막는다.
 */
@Configuration(proxyBeanMethods = false)
@Profile(LocalProfile.NAME + " & " + ProfileGuardConfig.PROD)
public class ProfileGuardConfig {

    static final String PROD = "prod";

    public ProfileGuardConfig() {
        throw new IllegalStateException(
                "local과 prod 프로필을 함께 켤 수 없습니다 (개발용 기능이 배포에서 열림, D-60). "
                        + "SPRING_PROFILES_ACTIVE를 확인하세요 (배포는 prod만, 로컬 개발은 local만).");
    }
}
