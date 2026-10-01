package com.findear.batch.ours.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.findear.batch.common.elasticsearch.ElasticsearchSourceReader;
import com.findear.batch.common.exception.FindearException;
import com.findear.batch.ours.domain.LostBoard;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.ours.dto.*;
import com.findear.batch.ours.repository.LostBoardRepository;
import com.findear.batch.ours.repository.PoliceMatchingLogRepository;
import com.findear.batch.police.exception.PoliceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PoliceDataService {

    private final PoliceMatchingLogRepository policeMatchingLogRepository;
    private final LostBoardRepository lostBoardRepository;

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchSourceReader sourceReader;

    public Page<PoliceMatchingLog> testApi() {

        Page<PoliceMatchingLog> result = policeMatchingLogRepository.findAll(PageRequest.of(0, 100));

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

                // 각 "lostBoardId" 별로 가장 높은 similarityRate를 가진 1개의 문서만 가져오기 위해 size를 1로 설정하고 similarityRate 내림차순으로 정렬
                NativeQuery query = NativeQuery.builder()
                        .withQuery(Query.of(q -> q.bool(b -> b.must(m -> m.match(mm -> mm.field("lostBoardId").query(id))))))
                        .withSort(s -> s.field(f -> f.field("similarityRate").order(SortOrder.Desc)))
                        .withPageable(PageRequest.of(0, 1))
                        .build();

                SearchHits<PoliceMatchingLog> hits = null;

                try {
                    hits = elasticsearchOperations.search(query, PoliceMatchingLog.class);
                    log.info("가장 높은 similarityRate의 결과를 리스트에 추가");

                    // 이하 생략
                } catch (DataAccessException e) {

                    SearchPoliceBestMatchingListDto searchFindearBestMatchingListDto = SearchPoliceBestMatchingListDto.builder()
                            .matchingList(Collections.emptyList())
                            .totalCount(bestMatchesList.size()).build();
                    return searchFindearBestMatchingListDto;
                }

                log.info("가장 높은 similarityRate의 결과를 리스트에 추가");
                // 가장 높은 similarityRate의 결과를 리스트에 추가
                if (hits.hasSearchHits()) {
                    PoliceMatchingLog bestMatch = hits.getSearchHit(0).getContent(); // 가장 높은 similarityRate를 가진 문서

                    bestMatchesList.add(toPoliceMatchingDto(bestMatch, id.toString()));
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

            // similarityRate 내림차순으로 정렬하고, 페이지(from·size)는 ES에서 자른다. totalCount는 전체 일치 건수
            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(b -> b.must(m -> m.match(mm -> mm.field("lostBoardId").query(lostBoardId))))))
                    .withSort(s -> s.field(f -> f.field("similarityRate").order(SortOrder.Desc)))
                    .withPageable(PageRequest.of(page - 1, size))
                    .withTrackTotalHits(true)
                    .build();

            // 검색 실행
            SearchHits<PoliceMatchingLog> hits = elasticsearchOperations.search(query, PoliceMatchingLog.class);

            List<SearchPoliceMatchingListResDto> matchingList = new ArrayList<>();

            // 검색 결과를 리스트에 추가
            for (SearchHit<PoliceMatchingLog> hit : hits) {

                matchingList.add(toPoliceMatchingDto(hit.getContent(), String.valueOf(hit.getContent().getLostBoardId())));
            }

            SearchPoliceBoardMatchingListDto result = SearchPoliceBoardMatchingListDto.builder()
                    .matchingList(matchingList)
                    .totalCount((int) hits.getTotalHits()).build();

            return result;

        } catch (Exception e) {
            throw new PoliceException(e.getMessage());
        }
    }

    private SearchPoliceMatchingListResDto toPoliceMatchingDto(PoliceMatchingLog matchingLog, String lostBoardId) {

        return SearchPoliceMatchingListResDto.builder()
                .policeMatchingLogId(matchingLog.getPoliceMatchingLogId().toString())
                .lostBoardId(lostBoardId)
                .similarityRate(matchingLog.getSimilarityRate().toString())
                .matchedAt(matchingLog.getMatchingAt())
                .acquiredBoardId(matchingLog.getAcquiredBoardId())
                .atcId(matchingLog.getAtcId())
                .depPlace(matchingLog.getDepPlace())
                .fdFilePathImg(matchingLog.getFdFilePathImg())
                .fdPrdtNm(matchingLog.getFdPrdtNm())
                .fdSbjt(matchingLog.getFdSbjt())
                .clrNm(matchingLog.getClrNm())
                .fdYmd(matchingLog.getFdYmd())
                .mainPrdtClNm(matchingLog.getMainPrdtClNm())
                .build();
    }

    public void deletePoliceMatchingDatas() {

        policeMatchingLogRepository.deleteAll();
    }

    public List<SearchScrapBoardResDto> searchScrapBoard(SearchScrapBoardReqDto searchScrapBoardReqDto) {

        try {

            List<SearchScrapBoardResDto> result = new ArrayList<>();

            for(String key : searchScrapBoardReqDto.getAtcIdList()) {

                NativeQuery query = NativeQuery.builder()
                        .withQuery(Query.of(q -> q.bool(b -> b.must(m -> m.match(mm -> mm.field("atcId").query(key))))))
                        .withPageable(PageRequest.of(0, 1))
                        .build();

                List<Map<String, Object>> hits;

                try {
                    hits = sourceReader.search("police_acquired_data", query);

                } catch (DataAccessException e) {

                    result = Collections.emptyList();
                    return result;
                }

                if (!hits.isEmpty()) {
                    Map<String, Object> source = hits.get(0);
                    // id는 ES에 숫자(Long)로 저장되지만 응답은 문자열이다
                    String id = source.get("id") == null ? null : source.get("id").toString();
                    String atcId = (String) source.get("atcId");
                    String depPlace = (String) source.get("depPlace");
                    String fdFilePathImg = (String) source.get("fdFilePathImg");
                    String fdPrdtNm = (String) source.get("fdPrdtNm");
                    String fdSbjt = (String) source.get("fdSbjt");
                    String clrNm = (String) source.get("clrNm");
                    String fdYmd = (String) source.get("fdYmd");
                    String prdtClNm = (String) source.get("prdtClNm");
                    String mainPrdtClNm = (String) source.get("mainPrdtClNm");
                    String subPrdtClNm = (String) source.get("subPrdtClNm");

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
