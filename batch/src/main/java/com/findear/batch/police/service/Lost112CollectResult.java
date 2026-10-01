package com.findear.batch.police.service;

import com.findear.batch.police.client.Lost112ApiService;

import java.util.List;

/**
 * 수집 한 번의 결과 요약 (수동 실행 API 응답에도 그대로 쓴다).
 *
 * @param startYmd 수집 시작일(yyyyMMdd)
 * @param endYmd   수집 종료일(yyyyMMdd)
 * @param services 서비스별 결과 (경찰청, 포털기관 순)
 */
public record Lost112CollectResult(String startYmd, String endYmd, List<ServiceResult> services) {

    /**
     * @param pages    요청해 받은 페이지 수
     * @param fetched  받은 item 수
     * @param indexed  인덱스에 넣은 문서 수
     * @param skipped  atcId·fdYmd가 없거나 형식이 틀려 건너뛴 item 수
     * @param truncated max-pages에 닿아 중간에 멈췄으면 true
     * @param error    실패 사유 (키 없음). 성공이면 null
     */
    public record ServiceResult(Lost112ApiService service, int pages, int fetched, int indexed, int skipped,
                                boolean truncated, String error) {

        public boolean failed() {
            return error != null;
        }
    }

    public int totalIndexed() {
        return services.stream().mapToInt(ServiceResult::indexed).sum();
    }

    public boolean hasFailure() {
        return services.stream().anyMatch(ServiceResult::failed);
    }

    /** 실패가 있고 하나도 못 넣었으면 true (수동 실행 API는 이때 502) */
    public boolean totalFailure() {
        return hasFailure() && totalIndexed() == 0;
    }

    /** 실패한 서비스의 사유를 한 줄로. 예: {@code POLICE: HTTP 500; PORTAL: 게이트웨이 오류 30 …} */
    public String failureSummary() {
        return String.join("; ", services.stream()
                .filter(ServiceResult::failed)
                .map(r -> r.service() + ": " + r.error())
                .toList());
    }
}
