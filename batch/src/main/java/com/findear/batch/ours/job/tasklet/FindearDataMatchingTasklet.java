package com.findear.batch.ours.job.tasklet;

import com.findear.batch.ours.domain.AcquiredBoard;
import com.findear.batch.ours.domain.LostBoard;
import com.findear.batch.ours.dto.AcquiredBoardMatchingDto;
import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.dto.MatchingFindearDatasToAiReqDto;
import com.findear.batch.ours.repository.AcquiredBoardRepository;
import com.findear.batch.ours.repository.LostBoardRepository;
import com.findear.batch.ours.service.MatchingLogWriter;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class FindearDataMatchingTasklet implements Tasklet, StepExecutionListener {

    private final LostBoardRepository lostBoardRepository;
    private final AcquiredBoardRepository acquiredBoardRepository;
    private final RestTemplate matchRestTemplate;
    private final MatchingLogWriter matchingLogWriter;

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

            ResponseEntity<Map> response = matchRestTemplate.postForEntity("/matching/findear", requestEntity, Map.class);

            System.out.println("response : " + response.getBody());

            List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.getBody().get("result");

            // 로그는 이번 결과와 같아지도록 교체한다 (결과가 null이면 이 분실물의 로그를 모두 지운다)
            matchingLogWriter.replaceFindearLogs(l.getId(), resultList);
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
