package com.findear.batch.common.job;

/**
 * 매칭 스텝에서 시도한 분실물이 모두 실패했을 때 스텝을 FAILED로 끝내는 예외 (match가 내려간 경우 등).
 * 원인은 분실물별 WARN 로그에 이미 있으므로 스택 트레이스를 만들지 않는다 (Spring Batch가 스텝 실패를 ERROR로 남길 때 긴 스택이 나오지 않게).
 */
public class MatchingStepFailedException extends RuntimeException {

    public MatchingStepFailedException(String message) {
        super(message, null, false, false);
    }
}
