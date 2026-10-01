package com.findear.batch.ours.service;

import com.findear.batch.ours.domain.AcquiredBoard;
import com.findear.batch.ours.dto.AcquiredBoardMatchingDto;
import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.dto.MatchingFindearDatasToAiReqDto;
import com.findear.batch.ours.dto.MatchingFindearDatasToAiResDto;
import com.findear.batch.ours.repository.AcquiredBoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 분실물 1건의 Findear 습득물 매칭. 분실물 등록 직후 매칭 API({@code POST /findear/matching})와
 * 정기 실행(findearJob, {@code POST /findear/matching/batch})이 모두 이것을 쓴다.
 * <p>
 * 후보는 같은 카테고리이고 분실일 0시 이후 등록된, 삭제되지 않은 진행 중(ONGOING) 습득물이다.
 * match에는 습득물 게시글의 <b>board_id</b>를 {@code acquiredBoardId}로 보낸다 (main이 매칭 로그의 acquiredBoardId를 board_id로 읽는다).
 * 결과는 {@link MatchingLogWriter}로 이 분실물의 로그를 교체해 저장한다. match 호출이 실패하면 예외가 나가고 기존 로그는 그대로다.
 * 트랜잭션은 호출하는 쪽(요청·스텝)의 것을 쓴다 (여기서 열지 않는다. 예외가 바깥 트랜잭션을 롤백 표시하지 않게).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindearMatchingService {

    private final AcquiredBoardRepository acquiredBoardRepository;
    private final RestTemplate matchRestTemplate;
    private final MatchingLogWriter matchingLogWriter;

    /**
     * @return match가 돌려준 결과 (후보가 없거나 결과가 null이면 빈 목록)
     */
    public List<MatchingFindearDatasToAiResDto> match(LostBoardMatchingDto lostBoardMatchingDto) {

        long lostBoardId = Long.parseLong(lostBoardMatchingDto.getLostBoardId());
        LocalDate lostAt = LocalDate.parse(lostBoardMatchingDto.getLostAt(), DateTimeFormatter.ISO_DATE);

        // 카테고리가 같고, 분실 일자 이후에 등록된 습득물 (삭제·반환 완료는 제외)
        List<AcquiredBoard> acquiredBoardList = acquiredBoardRepository
                .findAllWithBoardByCategoryAndAfterLostAt(lostBoardMatchingDto.getCategoryName(), lostAt.atStartOfDay());

        MatchingFindearDatasToAiReqDto request = MatchingFindearDatasToAiReqDto.builder()
                .lostBoard(lostBoardMatchingDto).acquiredBoardList(new ArrayList<>()).build();

        for (AcquiredBoard ab : acquiredBoardList) {
            request.getAcquiredBoardList().add(AcquiredBoardMatchingDto.builder()
                    .acquiredBoardId(ab.getBoard().getId().toString())
                    .productName(ab.getBoard().getProductName())
                    .color(ab.getBoard().getColor())
                    .categoryName(ab.getBoard().getCategoryName())
                    .description(ab.getBoard().getAiDescription())
                    .xPos(ab.getXPos() == null ? null : ab.getXPos().toString())
                    .yPos(ab.getYPos() == null ? null : ab.getYPos().toString())
                    .registeredAt(ab.getBoard().getRegisteredAt().toString())
                    .build());
        }
        log.debug("findear 매칭 요청 (lostBoardId={}, 후보 {}건)", lostBoardId, request.getAcquiredBoardList().size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = matchRestTemplate.postForEntity("/matching/findear", new HttpEntity<>(request, headers), Map.class);
        if (response.getBody() == null) {
            throw new IllegalStateException("match /matching/findear 응답 본문이 없습니다");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.getBody().get("result");
        log.debug("findear 매칭 결과 (lostBoardId={}, {}건)", lostBoardId, resultList == null ? 0 : resultList.size());

        // 로그는 이번 결과와 같아지도록 교체한다 (결과가 null이면 이 분실물의 로그를 모두 지운다)
        matchingLogWriter.replaceFindearLogs(lostBoardId, resultList);

        List<MatchingFindearDatasToAiResDto> result = new ArrayList<>();
        if (resultList != null) {
            for (Map<String, Object> res : resultList) {
                result.add(MatchingFindearDatasToAiResDto.builder()
                        .lostBoardId(res.get("lostBoardId"))
                        .acquiredBoardId(res.get("acquiredBoardId"))
                        .similarityRate(res.get("similarityRate")).build());
            }
        }
        return result;
    }
}
