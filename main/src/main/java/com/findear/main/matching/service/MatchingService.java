package com.findear.main.matching.service;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.BatchServerResponseDto;

import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.matching.model.dto.FindearMatchingListResDto;
import com.findear.main.matching.model.dto.FindearMatchingDto;
import com.findear.main.matching.model.dto.Lost112MatchingDto;
import com.findear.main.matching.model.dto.Lost112MatchingListResDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;


@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MatchingService {

    private final RestTemplate restTemplate;
    private final LostBoardQueryRepository lostBoardQueryRepository;
    private final AcquiredBoardQueryRepository acquiredBoardQueryRepository;

    @Value("${servers.batch-server.url}")
    private String BATCH_SERVER_URL;

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

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BATCH_SERVER_URL + "/" + src + "/"+ param + "/" + id)
                    .queryParam("page", pageNo)
                    .queryParam("size", size);
            log.info("builder.toUriString() = " + builder.toUriString());
            BatchServerResponseDto response = restTemplate.getForObject(builder.toUriString(), BatchServerResponseDto.class);
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
