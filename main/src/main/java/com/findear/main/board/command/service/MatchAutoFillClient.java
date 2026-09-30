package com.findear.main.board.command.service;

import com.findear.main.board.command.dto.AiGeneratedColumnDto;
import com.findear.main.board.command.dto.ModelServerResponseDto;
import com.findear.main.board.command.dto.NotFilledBoardDto;
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
 * match 서버 `POST /process`(07 §5.1)를 호출해 자동채움 결과를 반영 서비스에 넘긴다 (D-52).
 * WebClient는 Boot의 WebClient.Builder 빈으로 만든다 (HTTP client 지표, K-09).
 * 응답 대기에 상한을 두고, 블로킹 JPA 작업은 reactor-netty 이벤트 루프 밖(boundedElastic)에서 한다.
 * 실패(연결 불가, 4xx·5xx, 시간 초과, 본문 오류)는 WARN 한 줄로 끝내고 예외를 밖으로 내지 않는다.
 */
@Slf4j
@Component
public class MatchAutoFillClient {

    private static final int MAX_CAUSE_LENGTH = 200;

    private final WebClient webClient;
    private final AcquiredBoardAutoFillService autoFillService;
    private final Duration timeout;

    public MatchAutoFillClient(WebClient.Builder webClientBuilder,
                               AcquiredBoardAutoFillService autoFillService,
                               @Value("${servers.match-server.url}") String matchServerUrl,
                               @Value("${servers.match-server.autofill-timeout}") Duration timeout) {
        this.webClient = webClientBuilder.baseUrl(matchServerUrl).build();
        this.autoFillService = autoFillService;
        this.timeout = timeout;
    }

    /** 요청을 보내고 바로 돌아온다 (비동기). */
    public void requestAutoFill(AutoFillRequestedEvent event) {
        autoFill(event).subscribe();
    }

    /** 요청·반영 전체. 어떤 실패도 WARN 로그로 삼키므로 완료 신호만 나온다 (테스트가 완료를 기다릴 수 있게 Mono로 노출). */
    Mono<Void> autoFill(AutoFillRequestedEvent event) {
        return webClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NotFilledBoardDto(event.productName(), event.imgUrl()))
                .retrieve()
                .bodyToMono(ModelServerResponseDto.class)
                .timeout(timeout)
                .switchIfEmpty(Mono.error(new IllegalStateException("응답 본문 없음")))
                .publishOn(Schedulers.boundedElastic())
                .map(this::resultOf)
                .doOnNext(result -> {
                    autoFillService.apply(event.boardId(), result);
                    log.debug("습득물 자동채움 반영: boardId={}", event.boardId());
                })
                .then()
                .onErrorResume(error -> {
                    log.warn("습득물 자동채움 실패: boardId={}, {}", event.boardId(), summarize(error));
                    return Mono.empty();
                });
    }

    private AiGeneratedColumnDto resultOf(ModelServerResponseDto response) {
        if (response.getResult() == null) {
            throw new IllegalStateException("응답에 result 없음(message=" + response.getMessage() + ")");
        }
        return response.getResult();
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
