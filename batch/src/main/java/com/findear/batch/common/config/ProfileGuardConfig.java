package com.findear.batch.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * local과 prod 프로필이 함께 켜지면 기동에 실패하게 한다 (D-60).
 * 개발용 컨트롤러(전체 조회·전체 삭제, D-56)는 local 프로필이 켜져 있으면 등록되므로,
 * 배포(prod)에서 실수로 local이 같이 켜지는 일을 조용히 넘기지 않고 막는다.
 */
@Configuration(proxyBeanMethods = false)
@Profile("local & prod")
public class ProfileGuardConfig {

    public ProfileGuardConfig() {
        throw new IllegalStateException(
                "local과 prod 프로필을 함께 켤 수 없습니다 (전체 조회·삭제 같은 개발용 기능이 배포에서 열림, D-60). "
                        + "SPRING_PROFILES_ACTIVE를 확인하세요 (배포는 prod만, 로컬 개발은 local만).");
    }
}
