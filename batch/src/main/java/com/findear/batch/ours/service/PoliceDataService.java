package com.findear.batch.ours.service;

import com.findear.batch.common.exception.FindearException;
import com.findear.batch.ours.domain.LostBoard;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.ours.dto.*;
import com.findear.batch.ours.repository.LostBoardRepository;
import com.findear.batch.ours.repository.PoliceMatchingLogRepository;
import com.findear.batch.police.exception.PoliceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
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
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PoliceDataService {

    private final PoliceMatchingLogRepository policeMatchingLogRepository;
    private final LostBoardRepository lostBoardRepository;

    private final RestHighLevelClient restHighLevelClient;

    public Page<PoliceMatchingLog> testApi() {

        Page<PoliceMatchingLog> result = (Page<PoliceMatchingLog>) policeMatchingLogRepository.findAll();

        return result;
    }

    public SearchPoliceBestMatchingListDto searchPoliceBestMatchingList(int page, int size, Long memberId) {

        try {

            List<LostBoard> lostBoardList = lostBoardRepository.findAllWithBoardByMemberId(memberId);
            List<Long> lostBoardMatchingIds = new ArrayList<>();

            for (LostBoard lb : lostBoardList) {
                lostBoardMatchingIds.add(lb.getId());
            }

            List<SearchPoliceMatchingListResDto> bestMatchesList = new ArrayList<>();

            for (Long id : lostBoardMatchingIds) {
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                boolQueryBuilder.must(QueryBuilders.matchQuery("lostBoardId", id));

                // 검색 요청 생성
                SearchRequest searchRequest = new SearchRequest("police_matching_log");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(boolQueryBuilder);
                searchSourceBuilder.size(1); // 각 "lostBoardId" 별로 가장 높은 similarityRate를 가진 1개의 문서만 가져오기 위해 size를 1로 설정
                searchSourceBuilder.sort(SortBuilders.fieldSort("similarityRate").order(SortOrder.DESC)); // similarityRate 내림차순으로 정렬
                searchRequest.source(searchSourceBuilder);

                SearchHits hits = null;

                try {
                    hits = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT).getHits();
                    log.info("가장 높은 similarityRate의 결과를 리스트에 추가");

                    // 이하 생략
                } catch (ElasticsearchException | IOException e) {

                    SearchPoliceBestMatchingListDto searchFindearBestMatchingListDto = SearchPoliceBestMatchingListDto.builder()
                            .matchingList(Collections.emptyList())
                            .totalCount(bestMatchesList.size()).build();
                    return searchFindearBestMatchingListDto;
                }

                log.info("가장 높은 similarityRate의 결과를 리스트에 추가");
                // 가장 높은 similarityRate의 결과를 리스트에 추가
                if (hits.getTotalHits().value > 0) {
                    SearchHit bestMatchHit = hits.getAt(0); // 가장 높은 similarityRate를 가진 문서
                    Long policeMatchingLogId = Long.parseLong(bestMatchHit.getSourceAsMap().get("policeMatchingLogId").toString());
                    Float similarityRate = Float.parseFloat(bestMatchHit.getSourceAsMap().get("similarityRate").toString());
                    String matchingAt = (String) bestMatchHit.getSourceAsMap().get("matchingAt");
                    String acquiredBoardId = (String) bestMatchHit.getSourceAsMap().get("acquiredBoardId");
                    String atcId = (String) bestMatchHit.getSourceAsMap().get("atcId");
                    String depPlace = (String) bestMatchHit.getSourceAsMap().get("depPlace");
                    String fdFilePathImg = (String) bestMatchHit.getSourceAsMap().get("fdFilePathImg");
                    String fdPrdtNm = (String) bestMatchHit.getSourceAsMap().get("fdPrdtNm");
                    String fdSbjt = (String) bestMatchHit.getSourceAsMap().get("fdSbjt");
                    String clrNm = (String) bestMatchHit.getSourceAsMap().get("clrNm");
                    String fdYmd = (String) bestMatchHit.getSourceAsMap().get("fdYmd");
                    String mainPrdtClNm = (String) bestMatchHit.getSourceAsMap().get("mainPrdtClNm");

                    SearchPoliceMatchingListResDto dto = SearchPoliceMatchingListResDto.builder()
                            .policeMatchingLogId(policeMatchingLogId.toString())
                            .lostBoardId(id.toString())
                            .similarityRate(similarityRate.toString())
                            .matchedAt(matchingAt)
                            .acquiredBoardId(acquiredBoardId)
                            .atcId(atcId)
                            .depPlace(depPlace)
                            .fdFilePathImg(fdFilePathImg)
                            .fdPrdtNm(fdPrdtNm)
                            .fdSbjt(fdSbjt)
                            .clrNm(clrNm)
                            .fdYmd(fdYmd)
                            .mainPrdtClNm(mainPrdtClNm)
                            .build();

                    bestMatchesList.add(dto);
                }
            }
            log.info("bestMatchesList에 데이터 담김");

            int from = (page - 1) * size;
            int to = page * size;

            if(to > bestMatchesList.size()) {
                to = bestMatchesList.size();
            }

            List<SearchPoliceMatchingListResDto> result = new ArrayList<>();
            for(int i=from; i<to; i++) {
                result.add(bestMatchesList.get(i));
            }

            SearchPoliceBestMatchingListDto searchFindearBestMatchingListDto = SearchPoliceBestMatchingListDto.builder()
                    .matchingList(result)
                    .totalCount(bestMatchesList.size()).build();

            return searchFindearBestMatchingListDto;


        } catch (Exception e) {
            throw new FindearException(e.getMessage());
        }
    }

    public SearchPoliceBoardMatchingListDto searchPoliceBoardMatchingList(int page, int size, Long lostBoardId) {

        try {

            //////////////////////
            System.out.println("lostBoardId : " + lostBoardId);
            LostBoard findLostBoard = lostBoardRepository.findById(lostBoardId)
                    .orElseThrow(() -> new FindearException("해당 분실물이 존재하지 않습니다."));

            List<SearchPoliceMatchingListResDto> boardMatchingList = new ArrayList<>();

            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("lostBoardId", lostBoardId));

            // 검색 요청 생성
            SearchRequest searchRequest = new SearchRequest("police_matching_log");
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

                String policeMatchingLogId = hit.getSourceAsMap().get("policeMatchingLogId").toString();
                String findLostBoardId = hit.getSourceAsMap().get("lostBoardId").toString();
                Float similarityRate = Float.parseFloat(hit.getSourceAsMap().get("similarityRate").toString());
                String matchedAt = (String) hit.getSourceAsMap().get("matchingAt");
                String acquiredBoardId = (String) hit.getSourceAsMap().get("acquiredBoardId");
                String atcId = (String) hit.getSourceAsMap().get("atcId");
                String depPlace = (String) hit.getSourceAsMap().get("depPlace");
                String fdFilePathImg = (String) hit.getSourceAsMap().get("fdFilePathImg");
                String fdPrdtNm = (String) hit.getSourceAsMap().get("fdPrdtNm");
                String fdSbjt = (String) hit.getSourceAsMap().get("fdSbjt");
                String clrNm = (String) hit.getSourceAsMap().get("clrNm");
                String fdYmd = (String) hit.getSourceAsMap().get("fdYmd");
                String mainPrdtClNm = (String) hit.getSourceAsMap().get("mainPrdtClNm");

                SearchPoliceMatchingListResDto dto = SearchPoliceMatchingListResDto.builder()
                        .policeMatchingLogId(policeMatchingLogId)
                        .lostBoardId(findLostBoardId)
                        .similarityRate(similarityRate.toString())
                        .matchedAt(matchedAt)
                        .acquiredBoardId(acquiredBoardId)
                        .atcId(atcId)
                        .depPlace(depPlace)
                        .fdFilePathImg(fdFilePathImg)
                        .fdPrdtNm(fdPrdtNm)
                        .fdSbjt(fdSbjt)
                        .clrNm(clrNm)
                        .fdYmd(fdYmd)
                        .mainPrdtClNm(mainPrdtClNm)
                        .build();

                boardMatchingList.add(dto);
            }

            int from = (page - 1) * size;
            int to = page * size;

            if(to > boardMatchingList.size()) {
                to = boardMatchingList.size();
            }

            List<SearchPoliceMatchingListResDto> matchingList = new ArrayList<>();
            for(int i=from; i<to; i++) {
                matchingList.add(boardMatchingList.get(i));
            }

            SearchPoliceBoardMatchingListDto result = SearchPoliceBoardMatchingListDto.builder()
                    .matchingList(matchingList)
                    .totalCount(boardMatchingList.size()).build();

            return result;

        } catch (Exception e) {
            throw new PoliceException(e.getMessage());
        }
    }

    public void deletePoliceMatchingDatas() {

        policeMatchingLogRepository.deleteAll();
    }

    public List<SearchScrapBoardResDto> searchScrapBoard(SearchScrapBoardReqDto searchScrapBoardReqDto) {

        try {

            List<SearchScrapBoardResDto> result = new ArrayList<>();

            for(String key : searchScrapBoardReqDto.getAtcIdList()) {

                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                boolQueryBuilder.must(QueryBuilders.matchQuery("atcId", key));

                // 검색 요청 생성
                SearchRequest searchRequest = new SearchRequest("police_acquired_data");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(boolQueryBuilder);
                searchRequest.source(searchSourceBuilder);

                SearchHits hits = null;

                try {
                    hits = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT).getHits();

                } catch (ElasticsearchException | IOException e) {

                    result = Collections.emptyList();
                    return result;
                }

                if (hits.getTotalHits().value > 0) {
                    SearchHit bestMatchHit = hits.getAt(0); // 가장 높은 similarityRate를 가진 문서
                    String id = (String) bestMatchHit.getSourceAsMap().get("id");
                    String atcId = (String) bestMatchHit.getSourceAsMap().get("atcId");
                    String depPlace = (String) bestMatchHit.getSourceAsMap().get("depPlace");
                    String fdFilePathImg = (String) bestMatchHit.getSourceAsMap().get("fdFilePathImg");
                    String fdPrdtNm = (String) bestMatchHit.getSourceAsMap().get("fdPrdtNm");
                    String fdSbjt = (String) bestMatchHit.getSourceAsMap().get("fdSbjt");
                    String clrNm = (String) bestMatchHit.getSourceAsMap().get("clrNm");
                    String fdYmd = (String) bestMatchHit.getSourceAsMap().get("fdYmd");
                    String prdtClNm = (String) bestMatchHit.getSourceAsMap().get("prdtClNm");
                    String mainPrdtClNm = (String) bestMatchHit.getSourceAsMap().get("mainPrdtClNm");
                    String subPrdtClNm = (String) bestMatchHit.getSourceAsMap().get("subPrdtClNm");

                    SearchScrapBoardResDto dto = SearchScrapBoardResDto.builder()
                            .id(id)
                            .atcId(atcId)
                            .depPlace(depPlace)
                            .fdFilePathImg(fdFilePathImg)
                            .fdPrdtNm(fdPrdtNm)
                            .fdSbjt(fdSbjt)
                            .clrNm(clrNm)
                            .fdYmd(fdYmd)
                            .prdtClNm(prdtClNm)
                            .mainPrdtClNm(mainPrdtClNm)
                            .subPrdtClNm(subPrdtClNm)
                            .build();

                    result.add(dto);
                }
            }

            return result;

        } catch (Exception e) {
            throw new PoliceException(e.getMessage());
        }
    }
}
