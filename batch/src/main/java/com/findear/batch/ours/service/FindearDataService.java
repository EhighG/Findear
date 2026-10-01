package com.findear.batch.ours.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.findear.batch.common.exception.NotFoundException;
import com.findear.batch.common.request.RequestChecks;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.LostBoard;
import com.findear.batch.ours.domain.MatchingLogFormat;
import com.findear.batch.ours.dto.*;
import com.findear.batch.ours.repository.FindearMatchingLogRepository;
import com.findear.batch.ours.repository.LostBoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class FindearDataService {

    private final FindearMatchingLogRepository findearMatchingLogRepository;
    private final LostBoardRepository lostBoardRepository;
    private final FindearMatchingService findearMatchingService;
    private final PoliceMatchingService policeMatchingService;

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 분실물 등록 직후 매칭: 요청 본문의 분실물 하나를 Findear 습득물과 Lost112 습득물에 매칭한다.
     * 매칭과 로그 저장은 {@link FindearMatchingService}·{@link PoliceMatchingService}가 한다 (정기 잡과 같은 코드).
     */
    public MatchingAllDatasToAiResDto matchingFindearDatas(LostBoardMatchingDto lostBoardMatchingDto) {

        // 입력 검사: 잘못된 값은 match를 부르기 전에 400으로 끝낸다 (match 호출 실패는 MatchServerException, 502)
        RequestChecks.longValue(lostBoardMatchingDto.getLostBoardId(), "lostBoardId");
        RequestChecks.isoDate(lostBoardMatchingDto.getLostAt(), "lostAt");
        RequestChecks.required(lostBoardMatchingDto.getCategoryName(), "categoryName");

        log.info("분실물 매칭 (lostBoardId={})", lostBoardMatchingDto.getLostBoardId());

        List<MatchingFindearDatasToAiResDto> findearDatas = findearMatchingService.match(lostBoardMatchingDto);
        List<MatchingPoliceDatasToAiResDto> policeDatas = policeMatchingService.match(lostBoardMatchingDto);

        return new MatchingAllDatasToAiResDto(findearDatas, policeDatas);
    }

    public SearchFindearBoardMatchingListDto searchBoardMatchingList(int page, int size, Long lostBoardId) {

        RequestChecks.pageAndSize(page, size);

        // 없는 분실물은 404 (로그가 없는 분실물은 200 빈 목록)
        lostBoardRepository.findById(lostBoardId)
                .orElseThrow(() -> new NotFoundException("해당 분실물이 존재하지 않습니다."));

        // similarityRate 내림차순으로 정렬하고, 페이지(from·size)는 ES에서 자른다. totalCount는 전체 일치 건수
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("lostBoardId").value(lostBoardId))))))
                .withSort(s -> s.field(f -> f.field("similarityRate").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(page - 1, size))
                .withTrackTotalHits(true)
                .build();

        // 검색 실행
        SearchHits<FindearMatchingLog> hits = elasticsearchOperations.search(query, FindearMatchingLog.class);

        List<SearchFindearMatchingListResDto> result = new ArrayList<>();

        // 검색 결과를 리스트에 추가
        for (SearchHit<FindearMatchingLog> hit : hits) {
            result.add(toMatchingListDto(hit.getContent(), lostBoardId));
        }

        return SearchFindearBoardMatchingListDto.builder()
                .matchingList(result)
                .totalCount((int) hits.getTotalHits()).build();
    }

    public SearchFindearBestMatchingListDto searchBestMatchingList(int page, int size, Long memberId) {

        RequestChecks.pageAndSize(page, size);

        List<LostBoard> lostBoardList = lostBoardRepository.findAllWithBoardByMemberId(memberId);
        List<Long> lostBoardMatchingIds = new ArrayList<>();

        for (LostBoard lb : lostBoardList) {
            lostBoardMatchingIds.add(lb.getId());
        }

        List<SearchFindearMatchingListResDto> bestMatchesList = new ArrayList<>();

        for (Long id : lostBoardMatchingIds) {

            // 각 "lostBoardId" 별로 가장 높은 similarityRate를 가진 1개의 문서만 가져오기 위해 size를 1로 설정하고 similarityRate 내림차순으로 정렬
            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("lostBoardId").value(id))))))
                    .withSort(s -> s.field(f -> f.field("similarityRate").order(SortOrder.Desc)))
                    .withPageable(PageRequest.of(0, 1))
                    .build();

            // 검색 실행
            SearchHits<FindearMatchingLog> hits = elasticsearchOperations.search(query, FindearMatchingLog.class);

            // 가장 높은 similarityRate의 결과를 리스트에 추가
            if (hits.hasSearchHits()) {
                FindearMatchingLog bestMatch = hits.getSearchHit(0).getContent(); // 가장 높은 similarityRate를 가진 문서

                bestMatchesList.add(toMatchingListDto(bestMatch, id));
            }
        }

        // page·size는 위에서 1 이상으로 검사했다. 범위를 넘는 페이지는 빈 목록이다
        long from = (long) (page - 1) * size;
        long to = Math.min((long) page * size, bestMatchesList.size());

        List<SearchFindearMatchingListResDto> result = new ArrayList<>();
        for (long i = from; i < to; i++) {
            result.add(bestMatchesList.get((int) i));
        }

        return SearchFindearBestMatchingListDto.builder()
                .matchingList(result)
                .totalCount(bestMatchesList.size()).build();
    }

    public List<SearchFindearMatchingListResDto> searchAllFindearMatchingList() {

        List<SearchFindearMatchingListResDto> allDatas = new ArrayList<>();

        // 전체를 scroll로 500건씩 읽는다
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.matchAll(m -> m)))
                .withPageable(PageRequest.of(0, 500))
                .build();

        try (SearchHitsIterator<FindearMatchingLog> iterator =
                     elasticsearchOperations.searchForStream(query, FindearMatchingLog.class)) {

            while (iterator.hasNext()) {
                FindearMatchingLog matchingLog = iterator.next().getContent();
                allDatas.add(toMatchingListDto(matchingLog, matchingLog.getLostBoardId()));
            }

        }

        return allDatas;
    }

    private SearchFindearMatchingListResDto toMatchingListDto(FindearMatchingLog matchingLog, Long lostBoardId) {

        return new SearchFindearMatchingListResDto(
                matchingLog.getFindearMatchingLogId(),
                lostBoardId,
                matchingLog.getAcquiredBoardId(),
                matchingLog.getSimilarityRate(),
                MatchingLogFormat.format(matchingLog.getMatchingAt())
        );
    }

    public void deleteFindearMatchingDatas() {

        findearMatchingLogRepository.deleteAll();
    }

}
