package com.findear.batch.ours.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.findear.batch.common.elasticsearch.ElasticsearchSourceReader;
import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.dto.MatchingPoliceDatasToAiReqDto;
import com.findear.batch.ours.dto.MatchingPoliceDatasToAiResDto;
import com.findear.batch.ours.dto.PoliceAcquiredBoardMatchingDto;
import com.findear.batch.police.domain.PoliceAcquiredData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 분실물 1건의 Lost112 습득물 매칭. 분실물 등록 직후 매칭 API({@code POST /findear/matching})와
 * 정기 실행(policeJob의 policeMatchingStep, {@code POST /police/matching/batch})이 모두 이것을 쓴다.
 * <p>
 * 후보는 같은 카테고리(mainPrdtClNm 정확히 일치)이고 습득일(fdYmd)이 분실일 이후(당일 포함)인 Lost112 문서 전부다 (scroll로 500건씩 읽는다).
 * 결과는 {@link MatchingLogWriter}로 이 분실물의 로그를 교체해 저장한다. match 호출이 실패하면 예외가 나가고 기존 로그는 그대로다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoliceMatchingService {

    private final ElasticsearchSourceReader sourceReader;
    private final RestTemplate matchRestTemplate;
    private final MatchingLogWriter matchingLogWriter;

    /**
     * @return match가 돌려준 결과 (후보가 없거나 결과가 null이면 빈 목록)
     */
    public List<MatchingPoliceDatasToAiResDto> match(LostBoardMatchingDto lostBoardMatchingDto) {

        long lostBoardId = Long.parseLong(lostBoardMatchingDto.getLostBoardId());

        MatchingPoliceDatasToAiReqDto request = MatchingPoliceDatasToAiReqDto.builder()
                .lostBoard(lostBoardMatchingDto).acquiredBoardList(new ArrayList<>()).build();

        // 같은 카테고리이고 습득일(fdYmd)이 분실일 이후인 Lost112 습득물 전부 (scroll로 500건씩 읽는다)
        NativeQuery policeQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t.field("mainPrdtClNm").value(lostBoardMatchingDto.getCategoryName())))
                        .filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").gte(lostBoardMatchingDto.getLostAt())))))))
                .withPageable(PageRequest.of(0, 500))
                .withSourceFilter(new FetchSourceFilterBuilder()
                        .withIncludes("id", "atcId", "depPlace", "fdFilePathImg", "fdPrdtNm", "fdSbjt", "clrNm", "fdYmd", "mainPrdtClNm")
                        .build())
                .build();

        for (Map<String, Object> source : sourceReader.searchAll(PoliceAcquiredData.INDEX, policeQuery)) {
            request.getAcquiredBoardList().add(new PoliceAcquiredBoardMatchingDto(
                    text(source.get("id")), text(source.get("atcId")), text(source.get("depPlace")),
                    text(source.get("fdFilePathImg")), text(source.get("fdPrdtNm")), text(source.get("fdSbjt")),
                    text(source.get("clrNm")), text(source.get("fdYmd")), text(source.get("mainPrdtClNm"))));
        }
        log.debug("lost112 매칭 요청 (lostBoardId={}, 후보 {}건)", lostBoardId, request.getAcquiredBoardList().size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = matchRestTemplate.postForEntity("/matching/lost", new HttpEntity<>(request, headers), Map.class);
        if (response.getBody() == null) {
            throw new IllegalStateException("match /matching/lost 응답 본문이 없습니다");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.getBody().get("result");
        log.debug("lost112 매칭 결과 (lostBoardId={}, {}건)", lostBoardId, resultList == null ? 0 : resultList.size());

        // 로그는 이번 결과와 같아지도록 교체한다 (결과가 null이면 이 분실물의 로그를 모두 지운다)
        matchingLogWriter.replacePoliceLogs(lostBoardId, resultList);

        List<MatchingPoliceDatasToAiResDto> result = new ArrayList<>();
        if (resultList != null) {
            for (Map<String, Object> res : resultList) {
                result.add(MatchingPoliceDatasToAiResDto.builder()
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
                        .build());
            }
        }
        return result;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
