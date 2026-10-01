package com.findear.batch.police.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.findear.batch.common.elasticsearch.ElasticsearchSourceReader;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.exception.PoliceException;
import com.findear.batch.police.repository.PoliceAcquiredDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PoliceAcquiredDataService {

    private static final String INDEX = PoliceAcquiredData.INDEX;

    private static final String[] SOURCE_FIELDS = {"id", "atcId", "depPlace", "fdFilePathImg", "fdPrdtNm",
            "fdSbjt", "clrNm", "fdYmd", "prdtClNm", "mainPrdtClNm", "subPrdtClNm"};

    private final PoliceAcquiredDataRepository policeAcquiredDataRepository;
    private final ElasticsearchSourceReader sourceReader;

    public void deleteDatas() {

        policeAcquiredDataRepository.deleteAll();
    }


    public List<PoliceAcquiredData> search(int page, int size, String category,
                                           String startDate, String endDate, String keyword) {

        log.info("page = " + page);
        log.info("size = " + size);
        log.info("category = " + category);
        log.info("startDate = " + startDate);
        log.info("endDate = " + endDate);
        log.info("keyword = " + keyword);

        try {
            List<PoliceAcquiredData> allDatas = new ArrayList<>();

            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // category가 제공되었을 경우: mainPrdtClNm(keyword)과 정확히 같은 문서
            if (category != null && !category.isEmpty()) {
                boolQuery.filter(f -> f.term(t -> t.field("mainPrdtClNm").value(category)));
            }

            // fdYmd는 ES가 날짜(date)로 매핑한 필드라 yyyy-MM-dd 문자열로 범위를 건다 (lte는 그 날 끝까지 포함)
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                // startDate와 endDate가 모두 제공되었을 경우
                String start = LocalDate.parse(startDate).toString();
                String end = LocalDate.parse(endDate).toString();
                boolQuery.filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").gte(start).lte(end))));
            } else if (startDate != null && !startDate.isEmpty()) {
                // startDate만 제공되는 경우
                String start = LocalDate.parse(startDate).toString();
                boolQuery.filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").gte(start))));
            } else if (endDate != null && !endDate.isEmpty()) {
                // endDate만 제공되는 경우
                String end = LocalDate.parse(endDate).toString();
                boolQuery.filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").lte(end))));
            } else {
                // startDate와 endDate가 모두 없는 경우 기본값으로 오늘까지
                String today = LocalDate.now().toString();
                boolQuery.filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").lte(today))));
            }

            // keyword가 제공되었을 경우
            if (keyword != null && !keyword.isEmpty()) {
                boolQuery.must(m -> m.match(mm -> mm.field("fdSbjt").query(keyword)));
            }

            // 습득일 최신순(같은 날은 atcId 내림차순), 페이지 번호와 사이즈에 따라 검색 시작 위치(from)와 건수(size)를 ES에서 자른다
            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolQuery.build())))
                    .withSort(s -> s.field(f -> f.field("fdYmd").order(SortOrder.Desc)))
                    .withSort(s -> s.field(f -> f.field("atcId").order(SortOrder.Desc)))
                    .withPageable(PageRequest.of(page - 1, size))
                    .withSourceFilter(new FetchSourceFilterBuilder().withIncludes(SOURCE_FIELDS).build())
                    .build();

            for (Map<String, Object> source : sourceReader.search(INDEX, query)) {
                allDatas.add(convertToPoliceData(source));
            }

            return allDatas;

        } catch (Exception e) {
            throw new PoliceException(e.getMessage());
        }
    }


    public List<PoliceAcquiredData> searchAllDatas() {
        try {
            List<PoliceAcquiredData> allDatas = new ArrayList<>();

            // 전체를 scroll로 500건씩 읽는다
            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.matchAll(m -> m)))
                    .withPageable(PageRequest.of(0, 500))
                    .withSourceFilter(new FetchSourceFilterBuilder().withIncludes(SOURCE_FIELDS).build())
                    .build();

            for (Map<String, Object> source : sourceReader.searchAll(INDEX, query)) {
                allDatas.add(convertToPoliceData(source));
            }

            return allDatas;

        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    /**
     * 문서 source(Map)를 엔티티로 바꾼다. 없는 필드는 null이다 (필드마다 따로 읽으므로 하나가 비어도 다른 필드가 밀리지 않는다).
     * 응답의 id는 문서 ID(= atcId)이고, source에 id가 없으면 atcId를 쓴다.
     */
    static PoliceAcquiredData convertToPoliceData(Map<String, Object> source) {

        String atcId = text(source, "atcId");
        String id = text(source, "id");

        return PoliceAcquiredData.builder()
                .id(id != null ? id : atcId)
                .atcId(atcId)
                .depPlace(text(source, "depPlace"))
                .fdFilePathImg(text(source, "fdFilePathImg"))
                .fdPrdtNm(text(source, "fdPrdtNm"))
                .fdSbjt(text(source, "fdSbjt"))
                .clrNm(text(source, "clrNm"))
                .fdYmd(date(source, "fdYmd"))
                .prdtClNm(text(source, "prdtClNm"))
                .mainPrdtClNm(text(source, "mainPrdtClNm"))
                .subPrdtClNm(text(source, "subPrdtClNm"))
                .build();
    }

    /** yyyy-MM-dd 문자열 → 날짜. 없거나 형식이 다르면 null (조회가 한 문서 때문에 실패하지 않게) */
    private static LocalDate date(Map<String, Object> source, String key) {
        String value = text(source, key);
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String text(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }

    public Page<PoliceAcquiredData> searchByPage(int page, int size) {

        return policeAcquiredDataRepository.findAll(PageRequest.of(page, size));
    }


    public long getTotalCount() {

        return policeAcquiredDataRepository.count();
    }

}
