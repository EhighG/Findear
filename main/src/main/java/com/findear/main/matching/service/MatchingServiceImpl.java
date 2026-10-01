package com.findear.main.matching.service;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.BatchServerResponseDto;

import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.common.config.WebConfig;
import com.findear.main.matching.model.dto.FindearMatchingListResDto;
import com.findear.main.matching.model.dto.FindearMatchingDto;
import com.findear.main.matching.model.dto.Lost112MatchingDto;
import com.findear.main.matching.model.dto.Lost112MatchingListResDto;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;


@Slf4j
@Transactional
@Service
public class MatchingServiceImpl implements MatchingService {

    private final RestTemplate batchRestTemplate;
    private final LostBoardQueryRepository lostBoardQueryRepository;
    private final AcquiredBoardQueryRepository acquiredBoardQueryRepository;

    // RestTemplate 빈이 둘(공용 @Primary, batch 전용)이라 batch 전용을 이름으로 고른다
    public MatchingServiceImpl(@Qualifier(WebConfig.BATCH_REST_TEMPLATE) RestTemplate batchRestTemplate,
                               LostBoardQueryRepository lostBoardQueryRepository,
                               AcquiredBoardQueryRepository acquiredBoardQueryRepository) {
        this.batchRestTemplate = batchRestTemplate;
        this.lostBoardQueryRepository = lostBoardQueryRepository;
        this.acquiredBoardQueryRepository = acquiredBoardQueryRepository;
    }

    public FindearMatchingListResDto getFindearBestMatchings(Long memberId, int pageNo, int size) {
        Map<String, Object> response = sendRequest("member", "findear", memberId, pageNo, size);
        return parseFindearBoardInfo(response);
    }

    public FindearMatchingListResDto getFindearMatchingList(Long lostBoardId, int pageNo, int size) {
        Map<String, Object> response = sendRequest("board", "findear", lostBoardId, pageNo, size);
        return parseFindearBoardInfo(response);
    }

    public Lost112MatchingListResDto getLost112BestMatchings(Long memberId, int pageNo, int size) {
        return parseLost112MatchingData(sendRequest("member", "police",
                memberId, pageNo, size));
    }

    public Lost112MatchingListResDto getLost112MatchingList(Long lostBoardId, int pageNo, int size) {
        return parseLost112MatchingData(sendRequest("board", "police",
                lostBoardId, pageNo, size));
    }

    private FindearMatchingListResDto parseFindearBoardInfo(Map<String, Object> response) {
        List<Map<String, Object>> matchingList = (List<Map<String, Object>>) response.get("matchingList");
        List<FindearMatchingDto> parsedMatchingList = new ArrayList<>(matchingList.size());
        for (Map<String, Object> matchingInfo : matchingList) {
            Long lostBoardId = Long.parseLong(matchingInfo.get("lostBoardId").toString());
            Long acquiredBoardsboardId = Long.parseLong(matchingInfo.get("acquiredBoardId").toString());
            Optional<LostBoard> optionalLostBoard = lostBoardQueryRepository.findById(lostBoardId);
            LostBoard lostBoard = optionalLostBoard.isPresent() ? optionalLostBoard.get() : null;
            Optional<AcquiredBoard> optionalAcquiredBoard = acquiredBoardQueryRepository.findByBoardId(acquiredBoardsboardId);
            AcquiredBoard acquiredBoard = optionalAcquiredBoard.isPresent() ? optionalAcquiredBoard.get() : null;
            if (acquiredBoard != null && lostBoard != null) {
                FindearMatchingDto findearMatchingDto = new FindearMatchingDto(lostBoard, acquiredBoard, Float.parseFloat(matchingInfo.get("similarityRate").toString()),
                        (String) matchingInfo.get("matchedAt"));
                parsedMatchingList.add(findearMatchingDto);
            }
        }
        // TODO: convertCountToPageNum() 개선하면서 같이 변경하기(아마도 해당 메소드를 이 때 호출하도록)
        return new FindearMatchingListResDto(parsedMatchingList, (int) response.get("totalPageNum"));
    }

    private Lost112MatchingListResDto parseLost112MatchingData(Map<String, Object> response) {
        List<Lost112MatchingDto> matchingList = (List<Lost112MatchingDto>) response.get("matchingList");
        int totalPageNum = (int) response.get("totalPageNum");

        return new Lost112MatchingListResDto(matchingList, totalPageNum);
    }

    private Map<String, Object> sendRequest(String param, String src, Long id, int pageNo, int size) {
        try {

            // src·param·id도 모두 변수라 지표의 uri 태그는 고정 템플릿 "/{src}/{param}/{id}?page={page}&size={size}"가 된다
            BatchServerResponseDto response = batchRestTemplate.getForObject(
                    "/{src}/{param}/{id}?page={page}&size={size}", BatchServerResponseDto.class,
                    src, param, id, pageNo, size);
            Map<String, Object> result = (Map<String, Object>) response.getResult();
            return convertCountToPageNum(result, size);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("배치서버 요청 중 에러");
        }
    }

    private Map<String, Object> convertCountToPageNum(Map<String, Object> result, int pageSize) {
        int totalCount = (int) result.get("totalCount");
        result.remove("totalCount");
        if (totalCount == 0) {
            result.put("totalPageNum", 1);
        } else {
            result.put("totalPageNum", totalCount / pageSize + (totalCount % pageSize != 0 ? 1 : 0));
        }
        return result;
    }
}
