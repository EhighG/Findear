package com.findear.main.common.profile;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * 개발용 기능(전화번호 로그인, 테스트 가입, 알림 테스트 발송, test-member-type 헤더 인증)은 local 프로필에서만 켠다 (D-26).
 * 컨트롤러는 {@code @Profile(LocalProfile.NAME)}, 필터·프로바이더처럼 빈 하나가 요청 때 분기하는 곳은 {@link #isActive}를 쓴다.
 * 어느 쪽이든 판단 기준은 이 프로필 이름 하나다.
 */
public final class LocalProfile {

    public static final String NAME = "local";

    private LocalProfile() {
    }

    public static boolean isActive(Environment environment) {
        return environment.acceptsProfiles(Profiles.of(NAME));
    }
}
