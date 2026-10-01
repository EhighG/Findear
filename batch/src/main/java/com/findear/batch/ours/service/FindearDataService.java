package com.findear.batch.ours.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.findear.batch.common.elasticsearch.ElasticsearchSourceReader;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
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

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchSourceReader sourceReader;
    private final RestTemplate matchRestTemplate;

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

                ResponseEntity<Map> response = matchRestTemplate.postForEntity("/matching/findear", requestEntity, Map.class);

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

            log.info("findear 매칭 요청된 데이터 : " + requestEntity.getBody());
            ResponseEntity<Map> response = matchRestTemplate.postForEntity("/matching/findear", requestEntity, Map.class);

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

            // 같은 카테고리이고 습득일(fdYmd)이 분실일 이후인 Lost112 습득물 전부 (scroll로 500건씩 읽는다)
            NativeQuery policeQuery = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(b -> b
                            .must(m -> m.match(mm -> mm.field("mainPrdtClNm").query(lostBoardMatchingDto.getCategoryName())))
                            .filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").gte(lostBoardMatchingDto.getLostAt())))))))
                    .withPageable(PageRequest.of(0, 500))
                    .withSourceFilter(new FetchSourceFilterBuilder()
                            .withIncludes("id", "atcId", "depPlace", "fdFilePathImg", "fdPrdtNm", "fdSbjt", "clrNm", "fdYmd", "mainPrdtClNm")
                            .build())
                    .build();

            for (Map<String, Object> sourceAsMap : sourceReader.searchAll("police_acquired_data", policeQuery)) {

                PoliceAcquiredBoardMatchingDto policeAcquiredBoardMatchingDto = new PoliceAcquiredBoardMatchingDto(
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


            List<PoliceMatchingLog> policeMatchingLogList = new ArrayList<>();

            HttpEntity<?> requestEntity2 = new HttpEntity<>(matchingPoliceDatasToAiReqDto, headers);

            log.info("lost112 요청된 데이터 : " + requestEntity2.getBody());
            ResponseEntity<Map> response2 = matchRestTemplate.postForEntity("/matching/lost", requestEntity2, Map.class);

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

            // similarityRate 내림차순으로 정렬하고, 페이지(from·size)는 ES에서 자른다. totalCount는 전체 일치 건수
            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(b -> b.must(m -> m.match(mm -> mm.field("lostBoardId").query(lostBoardId))))))
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

            SearchFindearBoardMatchingListDto searchFindearBoardMatchingListDto = SearchFindearBoardMatchingListDto.builder()
                    .matchingList(result)
                    .totalCount((int) hits.getTotalHits()).build();

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

                // 각 "lostBoardId" 별로 가장 높은 similarityRate를 가진 1개의 문서만 가져오기 위해 size를 1로 설정하고 similarityRate 내림차순으로 정렬
                NativeQuery query = NativeQuery.builder()
                        .withQuery(Query.of(q -> q.bool(b -> b.must(m -> m.match(mm -> mm.field("lostBoardId").query(id))))))
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

        } catch (Exception e) {
            throw new FindearException(e.getMessage());
        }
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

        } catch (Exception e) {

            e.printStackTrace();
        }

        return allDatas;
    }

    private SearchFindearMatchingListResDto toMatchingListDto(FindearMatchingLog matchingLog, Long lostBoardId) {

        return new SearchFindearMatchingListResDto(
                matchingLog.getFindearMatchingLogId(),
                lostBoardId,
                matchingLog.getAcquiredBoardId(),
                matchingLog.getSimilarityRate(),
                matchingLog.getMatchingAt()
        );
    }

    public void deleteFindearMatchingDatas() {

        findearMatchingLogRepository.deleteAll();
    }

    public Page<FindearMatchingLog> testApi() {

        Page<FindearMatchingLog> result = findearMatchingLogRepository.findAll(PageRequest.of(0, 100));

        return result;
    }


}
