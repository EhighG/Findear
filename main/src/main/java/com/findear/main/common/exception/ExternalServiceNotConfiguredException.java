package com.findear.main.common.exception;

import java.util.List;

/**
 * 외부 연동에 필요한 설정(키 등)이 비어 있어 그 연동을 쓸 수 없을 때 던진다 (D-49).
 * {@link ExternalServiceExceptionAdvice}가 HTTP 503 + 공통 실패 형식으로 응답한다.
 * 메시지: "{서비스 이름}(이/가) 설정되지 않았습니다: {환경변수 이름, ...}"
 */
public class ExternalServiceNotConfiguredException extends RuntimeException {

    private final String serviceName;
    private final List<String> envNames;

    public ExternalServiceNotConfiguredException(String serviceName, String... envNames) {
        super(serviceName + subjectParticle(serviceName) + " 설정되지 않았습니다: " + String.join(", ", envNames));
        this.serviceName = serviceName;
        this.envNames = List.of(envNames);
    }

    public String getServiceName() {
        return serviceName;
    }

    public List<String> getEnvNames() {
        return envNames;
    }

    // 마지막 글자가 받침 있는 한글이면 "이", 그 밖(받침 없는 한글, 영문 등)은 "가"
    private static String subjectParticle(String name) {
        if (name == null || name.isEmpty()) {
            return "가";
        }
        char last = name.charAt(name.length() - 1);
        if (last >= 0xAC00 && last <= 0xD7A3 && (last - 0xAC00) % 28 != 0) {
            return "이";
        }
        return "가";
    }
}
