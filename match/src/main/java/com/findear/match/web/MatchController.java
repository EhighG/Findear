package com.findear.match.web;

import com.findear.match.config.MockProperties;
import com.findear.match.dto.FindearMatchItem;
import com.findear.match.dto.FindearMatchRequest;
import com.findear.match.dto.LostMatchItem;
import com.findear.match.dto.LostMatchRequest;
import com.findear.match.dto.MatchResponse;
import com.findear.match.dto.ProcessRequest;
import com.findear.match.dto.ProcessResponse;
import com.findear.match.service.MatchingService;
import com.findear.match.service.ProcessService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 팀 시절 Django match 서버와 같은 경로(접두사 없음)·JSON 형태 (07 §5). */
@RestController
public class MatchController {

    private final ProcessService processService;
    private final MatchingService matchingService;
    private final long latencyMs;

    public MatchController(ProcessService processService, MatchingService matchingService, MockProperties properties) {
        this.processService = processService;
        this.matchingService = matchingService;
        this.latencyMs = properties.latencyMs();
    }

    @PostMapping("/process")
    public ProcessResponse process(@RequestBody ProcessRequest request) {
        delay();
        return processService.process(request);
    }

    @PostMapping("/matching/findear")
    public MatchResponse<FindearMatchItem> matchFindear(@RequestBody FindearMatchRequest request) {
        delay();
        return matchingService.matchFindear(request);
    }

    @PostMapping("/matching/lost")
    public MatchResponse<LostMatchItem> matchLost(@RequestBody LostMatchRequest request) {
        delay();
        return matchingService.matchLost(request);
    }

    /** MATCH_MOCK_LATENCY_MS만큼 응답을 늦춘다 (모니터링 확인용). 0이면 대기 없음. */
    private void delay() {
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
