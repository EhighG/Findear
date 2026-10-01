package com.findear.main.board.command.service;

import com.findear.main.board.command.dto.MatchingFindearDatasReqDto;
import com.findear.main.board.query.dto.BatchServerResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * batch 서버 `POST /findear/matching`(07 §2)을 호출해 매칭 결과를 알림 서비스에 넘긴다 (D-52).
 * WebClient는 Boot의 WebClient.Builder 빈으로 만든다 (HTTP client 지표, K-09).
 * 응답 대기에 상한을 두고(batch가 match를 두 번 부른다), 블로킹 JPA 작업은 reactor-netty 이벤트 루프 밖(boundedElastic)에서 한다.
 * 실패(연결 불가, 4xx·5xx, 시간 초과, 본문 오류)는 WARN 한 줄로 끝내고 예외를 밖으로 내지 않는다.
 */
@Slf4j
@Component
public class BatchMatchingClient {

    private static final int MAX_CAUSE_LENGTH = 200;
    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024;

    private final WebClient webClient;
    private final LostBoardMatchingAlertService alertService;
    private final Duration timeout;

    public BatchMatchingClient(WebClient.Builder webClientBuilder,
                               LostBoardMatchingAlertService alertService,
                               @Value("${servers.batch-server.url}") String batchServerUrl,
                               @Value("${servers.batch-server.matching-timeout}") Duration timeout) {
        this.webClient = webClientBuilder.baseUrl(batchServerUrl)
                .codecs(it -> it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
        this.alertService = alertService;
        this.timeout = timeout;
    }

    /** 요청을 보내고 바로 돌아온다 (비동기). */
    public void requestMatching(LostBoardMatchingRequestedEvent event) {
        matching(event).subscribe();
    }

    /** 요청·알림 전체. 어떤 실패도 WARN 로그로 삼키므로 완료 신호만 나온다 (테스트가 완료를 기다릴 수 있게 Mono로 노출). */
    Mono<Void> matching(LostBoardMatchingRequestedEvent event) {
        return webClient.post()
                .uri("/findear/matching")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestOf(event))
                .retrieve()
                .bodyToMono(BatchServerResponseDto.class)
                .timeout(timeout)
                .switchIfEmpty(Mono.error(new IllegalStateException("응답 본문 없음")))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(response -> alertService.alertIfMatched(event.lostBoardId(), response))
                .then()
                .onErrorResume(error -> {
                    log.warn("분실물 매칭 요청 실패: lostBoardId={}, {}", event.lostBoardId(), summarize(error));
                    return Mono.empty();
                });
    }

    private MatchingFindearDatasReqDto requestOf(LostBoardMatchingRequestedEvent event) {
        return MatchingFindearDatasReqDto.builder()
                .lostBoardId(event.lostBoardId())
                .productName(event.productName())
                .color(event.color())
                .categoryName(event.categoryName())
                .description(event.description())
                .lostAt(event.lostAt() == null ? null : event.lostAt().toString())
                .xPos(event.xPos())
                .yPos(event.yPos())
                .build();
    }

    /** 스택 없이 한 줄로 남길 원인 요약 */
    private String summarize(Throwable error) {
        String summary;
        if (error instanceof WebClientResponseException e) {
            summary = "HTTP " + e.getStatusCode().value();
        } else if (error instanceof TimeoutException) {
            summary = "시간 초과(" + timeout + ")";
        } else if (error instanceof WebClientRequestException e) {
            summary = "요청 실패: " + e.getMessage();
        } else {
            summary = error.getClass().getSimpleName() + ": " + error.getMessage();
        }
        summary = summary.replaceAll("\\s+", " ");
        return summary.length() > MAX_CAUSE_LENGTH ? summary.substring(0, MAX_CAUSE_LENGTH) + "..." : summary;
    }
}
