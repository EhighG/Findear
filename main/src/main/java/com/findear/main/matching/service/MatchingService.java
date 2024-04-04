package com.findear.main.matching.service;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.BatchServerResponseDto;

import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.matching.model.dto.FindearMatchingResDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MatchingService {

    private final RestTemplate restTemplate;
    private final LostBoardQueryRepository lostBoardQueryRepository;
    private final AcquiredBoardQueryRepository acquiredBoardQueryRepository;

    private final String BATCH_SERVER_URL = "https://j10a706.p.ssafy.io/batch";

    public Map<String, Object> getFindearBestsResponse(Long memberId, int pageNo, int size) {
        Map<String, Object> response = sendRequest("member", "findear", memberId, pageNo, size);
        return parseFindearBoardInfo(response);
    }

    public Map<String, Object> getFindearMatchingsResponse(Long lostBoardId, int pageNo, int size) {
        Map<String, Object> response = sendRequest("board", "findear", lostBoardId, pageNo, size);
        return parseFindearBoardInfo(response);
    }

    public Map<String, Object> getLost112BestsResponse(Long memberId, int pageNo, int size) {
        return sendRequest("member", "police", memberId, pageNo, size);
    }

    public Map<String, Object> getLost112MatchingsResponse(Long lostBoardId, int pageNo, int size) {
        return sendRequest("board", "police", lostBoardId, pageNo, size);
    }

    private Map<String, Object> parseFindearBoardInfo(Map<String, Object> response) {
        List<Map<String, Object>> matchingList = (List<Map<String, Object>>) response.get("matchingList");
        List<FindearMatchingResDto> parsedMatchingList = new ArrayList<>(matchingList.size());
        for (Map<String, Object> matchingInfo : matchingList) {
            Long lostBoardId = Long.parseLong(matchingInfo.get("lostBoardId").toString());
            Long acquiredBoardsboardId = Long.parseLong(matchingInfo.get("acquiredBoardId").toString());
            Optional<LostBoard> optionalLostBoard = lostBoardQueryRepository.findById(lostBoardId);
            LostBoard lostBoard = optionalLostBoard.isPresent() ? optionalLostBoard.get() : null;
            Optional<AcquiredBoard> optionalAcquiredBoard = acquiredBoardQueryRepository.findByBoardId(acquiredBoardsboardId);
            AcquiredBoard acquiredBoard = optionalAcquiredBoard.isPresent() ? optionalAcquiredBoard.get() : null;
            if (acquiredBoard != null && lostBoard != null) {
                FindearMatchingResDto findearMatchingResDto = new FindearMatchingResDto(lostBoard, acquiredBoard, Float.parseFloat(matchingInfo.get("similarityRate").toString()),
                        (String) matchingInfo.get("matchedAt"));
                parsedMatchingList.add(findearMatchingResDto);
            }
        }
        response.put("matchingList", parsedMatchingList);
        return response;
    }

    private Map<String, Object> sendRequest(String param, String src, Long id, int pageNo, int size) {
        try {

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BATCH_SERVER_URL + "/" + src + "/"+ param + "/" + id)
//                .uriVariables(Collections.singletonMap(param + "Id", id))
                    .queryParam("page", pageNo)
                    .queryParam("size", size);
            System.out.println("builder.toUriString() = " + builder.toUriString());
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
