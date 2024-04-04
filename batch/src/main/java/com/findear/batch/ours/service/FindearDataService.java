package com.findear.batch.ours.service;

import com.findear.batch.common.exception.FindearException;
import com.findear.batch.ours.domain.AcquiredBoard;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.LostBoard;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.ours.dto.*;
import com.findear.batch.ours.repository.AcquiredBoardRepository;
import com.findear.batch.ours.repository.FindearMatchingLogRepository;
import com.findear.batch.ours.repository.LostBoardRepository;
import com.findear.batch.ours.repository.PoliceMatchingLogRepository;
import com.findear.batch.police.domain.PoliceAcquiredData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class FindearDataService {

    private final FindearMatchingLogRepository findearMatchingLogRepository;
    private final LostBoardRepository lostBoardRepository;
    private final AcquiredBoardRepository acquiredBoardRepository;
    private final PoliceMatchingLogRepository policeMatchingLogRepository;

    private final RestHighLevelClient restHighLevelClient;

    public List<MatchingFindearDatasToAiResDto> matchingFindearDatasBatch() {

        try {

            List<MatchingFindearDatasToAiResDto> result = new ArrayList<>();

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

                if (resultList == null) {

                    return Collections.emptyList();
                } else {
                    List<FindearMatchingLog> findearMatchingLogList = new ArrayList<>();

                    Long findearMatchingId = findearMatchingLogRepository.count() + 1;

                    // findear 매칭 로직
                    for (Map<String, Object> res : resultList) {

                        MatchingFindearDatasToAiResDto matchingFindearDatasToAiResDto = MatchingFindearDatasToAiResDto.builder()
                                .lostBoardId(res.get("lostBoardId"))
                                .acquiredBoardId(res.get("acquiredBoardId"))
                                .similarityRate(res.get("similarityRate")).build();

                        result.add(matchingFindearDatasToAiResDto);

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

            return result;

        } catch (Exception e) {
            throw new FindearException(e.getMessage());
        }
    }

    public MatchingAllDatasToAiResDto matchingFindearDatas(LostBoardMatchingDto lostBoardMatchingDto) {

        try {

            MatchingAllDatasToAiResDto result = new MatchingAllDatasToAiResDto(new ArrayList<>(), new ArrayList<>());

            log.info("분실물 매칭 service");
            // request dto 생성
            MatchingFindearDatasToAiReqDto matchingFindearDatasToAiReqDto = MatchingFindearDatasToAiReqDto
                    .builder().lostBoard(lostBoardMatchingDto).acquiredBoardList(new ArrayList<>()).build();

            log.info("request dto 생성 완료");

            LocalDate dateTime = LocalDate.parse(lostBoardMatchingDto.getLostAt(), DateTimeFormatter.ISO_DATE);

            // 카테고리가 같고, 분실 일자 이후에 등록된 게시글 전송
            List<AcquiredBoard> acquiredBoardList = acquiredBoardRepository
                    .findAllWithBoardByCategoryAndAfterLostAt(lostBoardMatchingDto.getCategoryName(), dateTime.atStartOfDay());

            log.info("카테고리가 같고, 분실 일자 이후에 등록된 게시글 전송");
            for(AcquiredBoard ab : acquiredBoardList) {

                matchingFindearDatasToAiReqDto.getAcquiredBoardList()
                        .add(AcquiredBoardMatchingDto.builder()
                                .acquiredBoardId(ab.getBoard().getId().toString())
                                .productName(ab.getBoard().getProductName())
                                .color(ab.getBoard().getColor())
                                .categoryName(ab.getBoard().getCategoryName())
                                .description(ab.getBoard().getAiDescription())
                                .xPos(ab.getXPos().toString())
                                .yPos(ab.getYPos().toString())
                                .registeredAt(ab.getBoard().getRegisteredAt().toString())
                                .build());
            }

            log.info("들어온 데이터 : " + matchingFindearDatasToAiReqDto.toString());

            log.info("ai 서버로 요청");
            // ai 서버로 요청
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

            HttpEntity<?> requestEntity = new HttpEntity<>(matchingFindearDatasToAiReqDto, headers);

            String serverURL = "https://j10a706.p.ssafy.io/match/matching/findear";

            RestTemplate restTemplate = new RestTemplate();

            log.info("findear 매칭 요청된 데이터 : " + requestEntity.getBody());
            ResponseEntity<Map> response = restTemplate.postForEntity(serverURL, requestEntity, Map.class);

            log.info("findear 매칭 결과 : " + response.getBody());
            List<Map<String, Object>> resultList = (List<Map<String, Object>> ) response.getBody().get("result");

            if(resultList == null) {

                result.setFindearDatas(Collections.emptyList());
            }
            else {
                List<FindearMatchingLog> findearMatchingLogList = new ArrayList<>();

                Long findearMatchingId = findearMatchingLogRepository.count() + 1;

                // findear 매칭 로직
                for(Map<String, Object> res : resultList) {

                    MatchingFindearDatasToAiResDto matchingFindearDatasToAiResDto = MatchingFindearDatasToAiResDto.builder()
                            .lostBoardId(res.get("lostBoardId"))
                            .acquiredBoardId(res.get("acquiredBoardId"))
                            .similarityRate(res.get("similarityRate")).build();

                    result.getFindearDatas().add(matchingFindearDatasToAiResDto);

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

            // lost112 매칭 로직

            log.info("lost112 매칭 start");
            Long policeMatchingId = policeMatchingLogRepository.count() + 1;

            MatchingPoliceDatasToAiReqDto matchingPoliceDatasToAiReqDto = MatchingPoliceDatasToAiReqDto
                    .builder().lostBoard(lostBoardMatchingDto).acquiredBoardList(new ArrayList<>()).build();

            List<PoliceAcquiredData> allDatas = new ArrayList<>();
            String searchAfter = null;
            int pageSize = 200; // 페이지당 가져올 문서 수

            while (true) {
                SearchRequest searchRequest = new SearchRequest("police_acquired_data");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                boolQueryBuilder.must(QueryBuilders.matchQuery("mainPrdtClNm", lostBoardMatchingDto.getCategoryName()));
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("fdYmd").gte(lostBoardMatchingDto.getLostAt()));

                searchSourceBuilder.query(boolQueryBuilder);
                searchSourceBuilder.size(pageSize);
                searchSourceBuilder.fetchSource(new String[]{"id", "atcId", "depPlace", "fdFilePathImg", "fdPrdtNm", "fdSbjt", "clrNm", "fdYmd", "mainPrdtClNm"}, null);

                if (searchAfter != null) {
                    searchSourceBuilder.sort("_doc");
                    searchSourceBuilder.searchAfter(new Object[]{searchAfter});
                }

                searchRequest.source(searchSourceBuilder);
                SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

                SearchHit[] hits = searchResponse.getHits().getHits();

                if (hits.length == 0) {
                    break;
                }

                PoliceAcquiredBoardMatchingDto policeAcquiredBoardMatchingDto = null;

                for (SearchHit hit : hits) {
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();

//                    log.info("id : " + sourceAsMap.get("id").toString());
////                    log.info("atcId : " + sourceAsMap.get("atcId").toString());
//                    log.info("depPlace : " + sourceAsMap.get("depPlace").toString());
//                    log.info("fdFilePathImg : " + sourceAsMap.get("fdFilePathImg").toString());
//                    log.info("fdPrdtNm : " + sourceAsMap.get("fdPrdtNm").toString());
//                    log.info("fdSbjt : " + sourceAsMap.get("fdSbjt").toString());
////                    log.info("clrNm : " + sourceAsMap.get("clrNm").toString());
//                    log.info("fdYmd : " + sourceAsMap.get("fdYmd").toString());
//                    log.info("mainPrdtClNm : " + sourceAsMap.get("mainPrdtClNm").toString());

                    policeAcquiredBoardMatchingDto = new PoliceAcquiredBoardMatchingDto(
                            sourceAsMap.get("id") == null ? null : sourceAsMap.get("id").toString(),
                            sourceAsMap.get("atcId") == null ? null : sourceAsMap.get("atcId").toString(),
                            sourceAsMap.get("depPlace") == null ? null : sourceAsMap.get("depPlace").toString(),
                            sourceAsMap.get("fdFilePathImg") == null ? null : sourceAsMap.get("fdFilePathImg").toString(),
                            sourceAsMap.get("fdPrdtNm") == null ? null : sourceAsMap.get("fdPrdtNm").toString(),
                            sourceAsMap.get("fdSbjt") == null ? null : sourceAsMap.get("fdSbjt").toString(),
                            sourceAsMap.get("clrNm") == null ? null : sourceAsMap.get("clrNm").toString(),
                            sourceAsMap.get("fdYmd") == null ? null : sourceAsMap.get("fdYmd").toString(),
                            sourceAsMap.get("mainPrdtClNm") == null ? null : sourceAsMap.get("mainPrdtClNm").toString()
                    );

                    matchingPoliceDatasToAiReqDto.getAcquiredBoardList().add(policeAcquiredBoardMatchingDto);
                }

                searchAfter = getLastSortValue(hits);
            }


            List<PoliceMatchingLog> policeMatchingLogList = new ArrayList<>();

            HttpEntity<?> requestEntity2 = new HttpEntity<>(matchingPoliceDatasToAiReqDto, headers);

            String serverURL2 = "https://j10a706.p.ssafy.io/match/matching/lost";

            log.info("lost112 요청된 데이터 : " + requestEntity2.getBody());
            ResponseEntity<Map> response2 = restTemplate.postForEntity(serverURL2, requestEntity2, Map.class);

            log.info("lost112 매칭 결과 : " + response2.getBody());
            List<Map<String, Object>> resultList2 = (List<Map<String, Object>> ) response2.getBody().get("result");

            if(resultList2 == null) {

                result.setPoliceDatas(Collections.emptyList());
            }
            else {
                for (Map<String, Object> res : resultList2) {

                    MatchingPoliceDatasToAiResDto matchingPoliceDatasToAiResDto = MatchingPoliceDatasToAiResDto.builder()
                            .lostBoardId(res.get("lostBoardId"))
                            .acquiredBoardId(res.get("acquiredBoardId"))
                            .similarityRate(res.get("similarityRate"))
                            .atcId(res.get("atcId"))
                            .depPlace(res.get("depPlace"))
                            .fdFilePathImg(res.get("fdFilePathImg"))
                            .fdPrdtNm(res.get("fdPrdtNm"))
                            .fdSbjt(res.get("fdSbjt"))
                            .clrNm(res.get("clrNm"))
                            .fdYmd(res.get("fdYmd"))
                            .mainPrdtClNm(res.get("mainPrdtClNm"))
                            .build();

                    result.getPoliceDatas().add(matchingPoliceDatasToAiResDto);

                    PoliceMatchingLog newPoliceMatchingLog = PoliceMatchingLog.builder()
                            .policeMatchingLogId(policeMatchingId++)
                            .lostBoardId(Long.parseLong(String.valueOf(matchingPoliceDatasToAiResDto.getLostBoardId())))
                            .acquiredBoardId(String.valueOf(matchingPoliceDatasToAiResDto.getAcquiredBoardId()))
                            .similarityRate(Float.parseFloat(String.valueOf(matchingPoliceDatasToAiResDto.getSimilarityRate())))
                            .matchingAt(LocalDateTime.now().toString())
                            .atcId(matchingPoliceDatasToAiResDto.getAtcId().toString())
                            .depPlace(matchingPoliceDatasToAiResDto.getDepPlace().toString())
                            .fdFilePathImg(matchingPoliceDatasToAiResDto.getFdFilePathImg().toString())
                            .fdPrdtNm(matchingPoliceDatasToAiResDto.getFdPrdtNm().toString())
                            .fdSbjt(matchingPoliceDatasToAiResDto.getFdSbjt().toString())
                            .clrNm(matchingPoliceDatasToAiResDto.getClrNm() == null ? null : matchingPoliceDatasToAiResDto.getClrNm().toString())
                            .fdYmd(matchingPoliceDatasToAiResDto.getFdYmd().toString())
                            .mainPrdtClNm(matchingPoliceDatasToAiResDto.getMainPrdtClNm().toString())
                            .build();

                    policeMatchingLogList.add(newPoliceMatchingLog);
                }
                policeMatchingLogRepository.saveAll(policeMatchingLogList);
            }
//
//            policeMatchingLogRepository.saveAll(policeMatchingLogList);

            return result;

        } catch (Exception e) {

            throw new FindearException(e.getMessage());
        }
    }

    public SearchFindearBoardMatchingListDto searchBoardMatchingList(int page, int size, Long lostBoardId) {

        try {
            System.out.println("lostBoardId : " + lostBoardId);
            LostBoard findLostBoard = lostBoardRepository.findById(lostBoardId)
                    .orElseThrow(() -> new FindearException("해당 분실물이 존재하지 않습니다."));

            List<SearchFindearMatchingListResDto> boardMatchingList = new ArrayList<>();

            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("lostBoardId", lostBoardId));

            // 검색 요청 생성
            SearchRequest searchRequest = new SearchRequest("findear_matching_log");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.sort(SortBuilders.fieldSort("similarityRate").order(SortOrder.DESC)); // similarityRate 내림차순으로 정렬
            searchRequest.source(searchSourceBuilder);

            SearchHits hits;
            try {
                // 검색 실행
                hits = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT).getHits();
            } catch (IOException e) {
                throw new FindearException(e.getMessage());
            }

            // 검색 결과를 리스트에 추가
            for (SearchHit hit : hits) {
                Long findearMatchingLogId = Long.parseLong(hit.getSourceAsMap().get("findearMatchingLogId").toString());
                Long acquiredBoardId = Long.parseLong(hit.getSourceAsMap().get("acquiredBoardId").toString());
                Float similarityRate = Float.parseFloat(hit.getSourceAsMap().get("similarityRate").toString());
                String matchingAt = (String) hit.getSourceAsMap().get("matchingAt");

                SearchFindearMatchingListResDto dto = new SearchFindearMatchingListResDto(
                        findearMatchingLogId, lostBoardId, acquiredBoardId, similarityRate, matchingAt
                );

                boardMatchingList.add(dto);
            }

            int from = (page - 1) * size;
            int to = page * size;

            if(to > boardMatchingList.size()) {
                to = boardMatchingList.size();
            }

            List<SearchFindearMatchingListResDto> result = new ArrayList<>();
            for(int i=from; i<to; i++) {
                result.add(boardMatchingList.get(i));
            }

            SearchFindearBoardMatchingListDto searchFindearBoardMatchingListDto = SearchFindearBoardMatchingListDto.builder()
                    .matchingList(result)
                    .totalCount(boardMatchingList.size()).build();

            return searchFindearBoardMatchingListDto;

        } catch (Exception e) {

            throw new FindearException(e.getMessage());
        }
    }

    public SearchFindearBestMatchingListDto searchBestMatchingList(int page, int size, Long memberId) {

        try {

            List<LostBoard> lostBoardList = lostBoardRepository.findAllWithBoardByMemberId(memberId);
            List<Long> lostBoardMatchingIds = new ArrayList<>();

            for (LostBoard lb : lostBoardList) {
                lostBoardMatchingIds.add(lb.getId());
            }

            List<SearchFindearMatchingListResDto> bestMatchesList = new ArrayList<>();

            for (Long id : lostBoardMatchingIds) {
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                boolQueryBuilder.must(QueryBuilders.matchQuery("lostBoardId", id));

                // 검색 요청 생성
                SearchRequest searchRequest = new SearchRequest("findear_matching_log");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(boolQueryBuilder);
                searchSourceBuilder.size(1); // 각 "lostBoardId" 별로 가장 높은 similarityRate를 가진 1개의 문서만 가져오기 위해 size를 1로 설정
                searchSourceBuilder.sort(SortBuilders.fieldSort("similarityRate").order(SortOrder.DESC)); // similarityRate 내림차순으로 정렬
                searchRequest.source(searchSourceBuilder);

                // 검색 실행
                SearchHits hits = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT).getHits();

                // 가장 높은 similarityRate의 결과를 리스트에 추가
                if (hits.getTotalHits().value > 0) {
                    SearchHit bestMatchHit = hits.getAt(0); // 가장 높은 similarityRate를 가진 문서
                    Long findearMatchingLogId = Long.parseLong(bestMatchHit.getSourceAsMap().get("findearMatchingLogId").toString());
                    Long acquiredBoardId = Long.parseLong(bestMatchHit.getSourceAsMap().get("acquiredBoardId").toString());
                    Float similarityRate = Float.parseFloat(bestMatchHit.getSourceAsMap().get("similarityRate").toString());
                    String matchingAt = (String) bestMatchHit.getSourceAsMap().get("matchingAt");

                    SearchFindearMatchingListResDto dto = new SearchFindearMatchingListResDto(
                            findearMatchingLogId, id, acquiredBoardId, similarityRate, matchingAt
                    );

                    bestMatchesList.add(dto);
                }
            }

            int from = (page - 1) * size;
            int to = page * size;

            if(to > bestMatchesList.size()) {
                to = bestMatchesList.size();
            }

            List<SearchFindearMatchingListResDto> result = new ArrayList<>();
            for(int i=from; i<to; i++) {
                result.add(bestMatchesList.get(i));
            }

            SearchFindearBestMatchingListDto searchFindearBestMatchingListDto = SearchFindearBestMatchingListDto.builder()
                    .matchingList(result)
                    .totalCount(bestMatchesList.size()).build();

            return searchFindearBestMatchingListDto;

        } catch (IOException e) {
            throw new FindearException(e.getMessage());
        }
    }

    public List<SearchFindearMatchingListResDto> searchAllFindearMatchingList() {

        List<SearchFindearMatchingListResDto> allDatas = new ArrayList<>();
        String searchAfter = null;
        int pageSize = 200; // 페이지당 가져올 문서 수

        try {

            while (true) {
                SearchRequest searchRequest = new SearchRequest("findear_matching_log");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                searchSourceBuilder.size(pageSize);
                searchSourceBuilder.fetchSource(new String[]{"findearMatchingLogId", "lostBoardId", "acquiredBoardId", "similarityRate", "matchingAt"}, null);

                if (searchAfter != null) {
                    searchSourceBuilder.sort("_doc");
                    searchSourceBuilder.searchAfter(new Object[]{searchAfter});
                }

                searchRequest.source(searchSourceBuilder);
                SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

                SearchHit[] hits = searchResponse.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }

                for (SearchHit hit : hits) {
                    allDatas.add(convertToFindearMatchingLog(hit));
                }

                searchAfter = getLastSortValue(hits);
                System.out.println("searchAfter = " + searchAfter);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return allDatas;
    }

    private String getLastSortValue(SearchHit[] hits) {
        SearchHit lastHit = hits[hits.length - 1];
        Object[] sortValues = lastHit.getSortValues();
        if (sortValues != null && sortValues.length > 0) {
            return sortValues[0].toString();
        } else {
            return lastHit.getId();
        }
    }


    private SearchFindearMatchingListResDto convertToFindearMatchingLog(SearchHit hit) {

        Map<String, Object> sourceAsMap = hit.getSourceAsMap();

        return new SearchFindearMatchingListResDto(

                Long.parseLong(sourceAsMap.get("findearMatchingLogId").toString()),
                Long.parseLong(sourceAsMap.get("lostBoardId").toString()),
                Long.parseLong(sourceAsMap.get("acquiredBoardId").toString()),
                Float.parseFloat(sourceAsMap.get("similarityRate").toString()),
                sourceAsMap.get("matchingAt").toString()
        );

    }

    public void deleteFindearMatchingDatas() {

        findearMatchingLogRepository.deleteAll();
    }

    public Page<FindearMatchingLog> testApi() {

        Page<FindearMatchingLog> result = (Page<FindearMatchingLog>) findearMatchingLogRepository.findAll();

        return result;
    }


}
