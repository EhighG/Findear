package com.findear.batch.police.exception;

/** Lost112 서비스 키(LOST112_SERVICE_KEY)가 비어 있어 수집할 수 없을 때. 수동 실행 API는 503으로 응답한다 (D-49) */
public class Lost112NotConfiguredException extends RuntimeException {

    public Lost112NotConfiguredException() {
        super("Lost112 API 키가 설정되지 않았습니다.");
    }
}
