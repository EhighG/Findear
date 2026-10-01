package com.findear.batch.common.job;

/** 같은 잡이 이미 실행 중이라 새로 시작하지 않았을 때. 수동 실행 API는 409로 응답하고 스케줄러는 WARN 한 줄로 건너뛴다 */
public class BatchJobBusyException extends RuntimeException {

    private final String jobName;

    public BatchJobBusyException(String jobName) {
        super(jobName + "이(가) 이미 실행 중입니다.");
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }
}
