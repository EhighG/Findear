package com.findear.batch.ours.job.tasklet;

import com.findear.batch.ours.domain.AcquiredBoard;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.LostBoard;
import com.findear.batch.ours.dto.AcquiredBoardMatchingDto;
import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.dto.MatchingFindearDatasToAiReqDto;
import com.findear.batch.ours.dto.MatchingFindearDatasToAiResDto;
import com.findear.batch.ours.repository.AcquiredBoardRepository;
import com.findear.batch.ours.repository.FindearMatchingLogRepository;
import com.findear.batch.ours.repository.LostBoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class FindearDataMatchingTasklet implements Tasklet, StepExecutionListener {

    private final FindearMatchingLogRepository findearMatchingLogRepository;
    private final LostBoardRepository lostBoardRepository;
    private final AcquiredBoardRepository acquiredBoardRepository;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        // 찾아지지 않은 분실물 게시글 모두 조회
        List<LostBoard> lostBoardList = lostBoardRepository.findAllWithBoardByStatusOngoing();

        for(LostBoard l : lostBoardList) {

            // 분실물 게시글 정보
            LostBoardMatchingDto lostBoardMatchingDto = LostBoardMatchingDto.builder()
                    .lostBoardId(l.getId().toString())
                    .productName(l.getBoard().getProductName())
                    .color(l.getBoard().getColor())
                    .categoryName(l.getBoard().getCategoryName())
                    .description(l.getBoard().getAiDescription())
                    .lostAt(l.getLostAt().toString())
                    .xPos(l.getXPos().toString())
                    .yPos(l.getYPos().toString()).build();

            LocalDate dateTime = LocalDate.parse(lostBoardMatchingDto.getLostAt(), DateTimeFormatter.ISO_DATE);

            // 카테고리가 같고, 분실 일자 이후에 등록된 게시글 전송
            List<AcquiredBoard> acquiredBoardList = acquiredBoardRepository
                    .findAllWithBoardByCategoryAndAfterLostAt(lostBoardMatchingDto.getCategoryName(),
                            dateTime.atStartOfDay());

            // request dto 생성
            MatchingFindearDatasToAiReqDto matchingFindearDatasToAiReqDto = MatchingFindearDatasToAiReqDto
                    .builder().lostBoard(lostBoardMatchingDto).acquiredBoardList(new ArrayList<>()).build();

            for (AcquiredBoard ab : acquiredBoardList) {
                matchingFindearDatasToAiReqDto.getAcquiredBoardList()
                        .add(AcquiredBoardMatchingDto.builder()
                                .acquiredBoardId(ab.getId().toString())
                                .productName(ab.getBoard().getProductName())
                                .color(ab.getBoard().getColor())
                                .categoryName(ab.getBoard().getCategoryName())
                                .description(ab.getBoard().getAiDescription())
                                .xPos(ab.getXPos().toString())
                                .yPos(ab.getYPos().toString())
                                .registeredAt(ab.getBoard().getRegisteredAt().toString())
                                .build());
            }

            // ai 서버로 요청
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

            HttpEntity<?> requestEntity = new HttpEntity<>(matchingFindearDatasToAiReqDto, headers);

            String serverURL = "https://j10a706.p.ssafy.io/match/matching/findear";

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response = restTemplate.postForEntity(serverURL, requestEntity, Map.class);

            System.out.println("response : " + response.getBody());

            List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.getBody().get("result");

            if (resultList != null) {
                List<FindearMatchingLog> findearMatchingLogList = new ArrayList<>();

                Long findearMatchingId = findearMatchingLogRepository.count() + 1;

                // findear 매칭 로직
                for (Map<String, Object> res : resultList) {

                    MatchingFindearDatasToAiResDto matchingFindearDatasToAiResDto = MatchingFindearDatasToAiResDto.builder()
                            .lostBoardId(res.get("lostBoardId"))
                            .acquiredBoardId(res.get("acquiredBoardId"))
                            .similarityRate(res.get("similarityRate")).build();

                    FindearMatchingLog newFindearMatchingLog = FindearMatchingLog.builder()
                            .findearMatchingLogId(findearMatchingId++)
                            .lostBoardId(Long.parseLong(String.valueOf(matchingFindearDatasToAiResDto.getLostBoardId())))
                            .acquiredBoardId(Long.parseLong(String.valueOf(matchingFindearDatasToAiResDto.getAcquiredBoardId())))
                            .similarityRate(Float.parseFloat(String.valueOf(matchingFindearDatasToAiResDto.getSimilarityRate())))
                            .matchingAt(LocalDateTime.now().toString())
                            .build();

                    findearMatchingLogList.add(newFindearMatchingLog);
                }

                findearMatchingLogRepository.saveAll(findearMatchingLogList);
                log.info("findear 로그 저장 완료");
            }
        }

        return RepeatStatus.FINISHED;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {

        log.info("매칭 이전 start");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        log.info("매칭 이전 start");

        return ExitStatus.COMPLETED;
    }
}
