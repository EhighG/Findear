package com.findear.main.common.exception;

/**
 * 외부 서비스에 연결하지 못했거나(연결 실패·타임아웃) 외부가 5xx 등 비정상 응답을 준 경우 (HTTP 502).
 * 원인 예외의 메시지에는 요청 URL(키 포함)이 들어 있을 수 있으므로 응답·로그에 옮기지 않는다.
 */
public class ExternalServiceUnavailableException extends RuntimeException {

    private final String serviceName;

    public ExternalServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " 호출에 실패했습니다", cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
