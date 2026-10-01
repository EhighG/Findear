package com.findear.batch.police.service;

import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.exception.PoliceException;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.PoliceDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lost112 습득물 검색(ES police_acquired_data)의 동작 확인. 문서를 직접 넣고 서비스 메서드를 호출한다.
 * 문서 ID·atcId는 같은 문자열이고, 인덱스 매핑은 PoliceAcquiredData 어노테이션이 정한다 (R-32).
 */
class PoliceAcquiredDataServiceTest extends IntegrationTestBase {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    PoliceAcquiredDataService service;

    /** atcId는 "F2099" + 4자리 번호 */
    private static String atcId(long n) {
        return String.format("F2099%04d", n);
    }

    private PoliceAcquiredData doc(long n, String category, String subject, LocalDate date) {
        return PoliceDocs.builder(atcId(n), category, date).depPlace("서울 파출소").fdFilePathImg("https://img/" + n)
                .fdPrdtNm("물품" + n).fdSbjt(subject).build();
    }

    /** 결과의 문서 ID(= atcId)를 번호로 바꿔 정렬해서 돌려준다 */
    private List<Long> ids(List<PoliceAcquiredData> result) {
        return result.stream().map(d -> Long.parseLong(d.getId().substring(5))).sorted().toList();
    }

    private void saveBasicDocs() {
        policeAcquiredDataRepository.saveAll(List.of(
                doc(1, "지갑", "검정색 지갑 습득", TODAY.minusDays(20)),
                doc(2, "지갑", "갈색 지갑", TODAY.minusDays(10)),
                doc(3, "지갑", "카드케이스 습득", TODAY.minusDays(5)),
                doc(4, "휴대폰", "삼성 휴대폰", TODAY.minusDays(10)),
                doc(5, "지갑", "미래 날짜 지갑", TODAY.plusDays(5))));
    }

    @DisplayName("카테고리만: mainPrdtClNm이 같은 문서, 기간이 없으면 오늘까지 (미래 날짜 문서 제외)")
    @Test
    void categoryOnly() {
        saveBasicDocs();

        assertThat(ids(service.search(1, 10, "지갑", null, null, null))).containsExactly(1L, 2L, 3L);
        assertThat(ids(service.search(1, 10, "휴대폰", null, null, null))).containsExactly(4L);
        assertThat(ids(service.search(1, 10, "없는카테고리", null, null, null))).isEmpty();
    }

    @DisplayName("카테고리 없음: 전체 카테고리")
    @Test
    void noCategory() {
        saveBasicDocs();

        assertThat(ids(service.search(1, 10, null, null, null, null))).containsExactly(1L, 2L, 3L, 4L);
        assertThat(ids(service.search(1, 10, "", null, null, null))).containsExactly(1L, 2L, 3L, 4L);
    }

    @DisplayName("기간: 시작만 / 끝만 / 둘 다 (양끝 날짜 포함) / 없음")
    @Test
    void dateRange() {
        saveBasicDocs();

        // 시작만: 그 날 이후 전부 (기간을 주면 오늘 상한이 없다 - 팀 코드와 같음)
        assertThat(ids(service.search(1, 10, "지갑", TODAY.minusDays(10).toString(), null, null))).containsExactly(2L, 3L, 5L);
        // 끝만: 그 날까지 (끝 날짜 당일 포함)
        assertThat(ids(service.search(1, 10, "지갑", null, TODAY.minusDays(10).toString(), null))).containsExactly(1L, 2L);
        // 둘 다: 양끝 날짜 당일 포함
        assertThat(ids(service.search(1, 10, "지갑", TODAY.minusDays(20).toString(), TODAY.minusDays(10).toString(), null))).containsExactly(1L, 2L);
        assertThat(ids(service.search(1, 10, "지갑", TODAY.minusDays(19).toString(), TODAY.minusDays(6).toString(), null))).containsExactly(2L);
        // 미래까지 끝을 주면 미래 문서도 나온다
        assertThat(ids(service.search(1, 10, "지갑", null, TODAY.plusDays(10).toString(), null))).containsExactly(1L, 2L, 3L, 5L);
        // 없는 기간
        assertThat(ids(service.search(1, 10, "지갑", TODAY.minusDays(100).toString(), TODAY.minusDays(50).toString(), null))).isEmpty();
    }

    @DisplayName("날짜 형식이 틀리면 PoliceException")
    @Test
    void invalidDate() {
        saveBasicDocs();

        assertThatThrownBy(() -> service.search(1, 10, "지갑", "2026/10/01", null, null)).isInstanceOf(PoliceException.class);
    }

    @DisplayName("키워드: 게시 제목(fdSbjt) match")
    @Test
    void keyword() {
        saveBasicDocs();

        assertThat(ids(service.search(1, 10, "지갑", null, null, "카드케이스"))).containsExactly(3L);
        assertThat(ids(service.search(1, 10, null, null, null, "휴대폰"))).containsExactly(4L);
        assertThat(ids(service.search(1, 10, "지갑", null, null, "존재하지않는단어"))).isEmpty();
    }

    @DisplayName("페이지: from=(page-1)*size, size를 ES에서 자르고 전체를 겹침 없이 나눈다. getTotalCount는 인덱스 전체 건수")
    @Test
    void paging() {
        List<PoliceAcquiredData> docs = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            docs.add(doc(i, "지갑", "지갑 습득 " + i, TODAY.minusDays(i)));
        }
        docs.add(doc(100, "휴대폰", "휴대폰", TODAY.minusDays(1)));
        policeAcquiredDataRepository.saveAll(docs);

        List<PoliceAcquiredData> page1 = service.search(1, 10, "지갑", null, null, null);
        List<PoliceAcquiredData> page2 = service.search(1 + 1, 10, "지갑", null, null, null);
        List<PoliceAcquiredData> page3 = service.search(3, 10, "지갑", null, null, null);
        List<PoliceAcquiredData> page4 = service.search(4, 10, "지갑", null, null, null);

        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(10);
        assertThat(page3).hasSize(5);
        assertThat(page4).isEmpty();

        List<PoliceAcquiredData> all = new ArrayList<>(page1);
        all.addAll(page2);
        all.addAll(page3);
        assertThat(ids(all)).hasSize(25).doesNotHaveDuplicates();

        // size가 200보다 커도 그 크기만큼 (예전에는 200건을 가져와 자르는 방식이었다)
        assertThat(service.search(1, 30, "지갑", null, null, null)).hasSize(25);

        assertThat(service.getTotalCount()).isEqualTo(26);
    }

    @DisplayName("응답 필드: 저장한 문서의 필드가 그대로 나오고 id는 atcId 문자열이다")
    @Test
    void responseFields() {
        policeAcquiredDataRepository.save(doc(7, "지갑", "검정색 지갑 습득", TODAY.minusDays(3)));

        PoliceAcquiredData found = service.search(1, 10, "지갑", null, null, null).get(0);

        assertThat(found.getId()).isEqualTo("F20990007");
        assertThat(found.getAtcId()).isEqualTo("F20990007");
        assertThat(found.getDepPlace()).isEqualTo("서울 파출소");
        assertThat(found.getFdFilePathImg()).isEqualTo("https://img/7");
        assertThat(found.getFdPrdtNm()).isEqualTo("물품7");
        assertThat(found.getFdSbjt()).isEqualTo("검정색 지갑 습득");
        assertThat(found.getClrNm()).isEqualTo("검정");
        assertThat(found.getFdYmd()).isEqualTo(TODAY.minusDays(3));
        assertThat(found.getPrdtClNm()).isEqualTo("지갑 > 소분류");
        assertThat(found.getMainPrdtClNm()).isEqualTo("지갑");
        assertThat(found.getSubPrdtClNm()).isEqualTo("소분류");
    }

    @DisplayName("값이 없는 필드(clrNm·fdSbjt·depPlace 등)가 있어도 다른 필드가 밀리거나 예외가 나지 않는다")
    @Test
    void nullFieldsDoNotShiftOthers() {
        policeAcquiredDataRepository.saveAll(List.of(
                PoliceDocs.builder(atcId(1), "지갑", TODAY.minusDays(1)).clrNm(null).build(),
                PoliceDocs.builder(atcId(2), "지갑", TODAY.minusDays(2)).fdSbjt(null).build(),
                PoliceDocs.builder(atcId(3), "지갑", TODAY.minusDays(3)).depPlace(null).fdFilePathImg(null).subPrdtClNm(null).build()));

        List<PoliceAcquiredData> result = service.search(1, 10, "지갑", null, null, null);

        assertThat(result).hasSize(3);
        PoliceAcquiredData noColor = result.get(0);
        assertThat(noColor.getAtcId()).isEqualTo(atcId(1));
        assertThat(noColor.getClrNm()).isNull();
        assertThat(noColor.getFdSbjt()).isEqualTo("제목 " + atcId(1));
        assertThat(noColor.getFdYmd()).isEqualTo(TODAY.minusDays(1));
        assertThat(noColor.getMainPrdtClNm()).isEqualTo("지갑");

        PoliceAcquiredData noSubject = result.get(1);
        assertThat(noSubject.getFdSbjt()).isNull();
        assertThat(noSubject.getClrNm()).isEqualTo("검정");
        assertThat(noSubject.getDepPlace()).isEqualTo("종로경찰서");

        PoliceAcquiredData sparse = result.get(2);
        assertThat(sparse.getDepPlace()).isNull();
        assertThat(sparse.getFdFilePathImg()).isNull();
        assertThat(sparse.getSubPrdtClNm()).isNull();
        assertThat(sparse.getFdPrdtNm()).isEqualTo("물품 " + atcId(3));
        assertThat(sparse.getPrdtClNm()).isEqualTo("지갑 > 소분류");

        // 전체 조회(scroll)도 같은 변환을 쓴다
        assertThat(service.searchAllDatas()).hasSize(3);
    }

    @DisplayName("정렬: 습득일(fdYmd) 내림차순, 같은 날은 atcId 내림차순")
    @Test
    void sortedByDateThenAtcIdDescending() {
        LocalDate day = TODAY.minusDays(2);
        policeAcquiredDataRepository.saveAll(List.of(
                doc(3, "지갑", "셋", day),
                doc(1, "지갑", "하나", TODAY.minusDays(1)),
                doc(2, "지갑", "둘", day),
                doc(5, "지갑", "다섯", TODAY.minusDays(4)),
                doc(4, "지갑", "넷", day)));

        List<String> order = service.search(1, 10, "지갑", null, null, null).stream().map(PoliceAcquiredData::getAtcId).toList();

        assertThat(order).containsExactly(atcId(1), atcId(4), atcId(3), atcId(2), atcId(5));
        // 페이지를 나눠도 같은 순서가 이어진다
        assertThat(service.search(2, 2, "지갑", null, null, null)).extracting(PoliceAcquiredData::getAtcId)
                .containsExactly(atcId(3), atcId(2));
    }

    @DisplayName("카테고리는 mainPrdtClNm과 정확히 같은 문서만 (부분 일치 아님)")
    @Test
    void categoryIsExactMatch() {
        policeAcquiredDataRepository.saveAll(List.of(
                doc(1, "지갑", "지갑", TODAY.minusDays(1)),
                doc(2, "지갑류", "지갑류", TODAY.minusDays(1)),
                doc(3, "전자기기", "전자기기", TODAY.minusDays(1))));

        assertThat(ids(service.search(1, 10, "지갑", null, null, null))).containsExactly(1L);
        assertThat(ids(service.search(1, 10, "전자기기", null, null, null))).containsExactly(3L);
    }

    @DisplayName("searchAllDatas: 500건씩 scroll로 전부 읽는다 (예전 search_after는 깨져 있었다)")
    @Test
    void searchAllReadsEverything() {
        List<PoliceAcquiredData> docs = new ArrayList<>();
        for (long i = 1; i <= 1200; i++) {
            docs.add(doc(i, "지갑", "지갑 " + i, TODAY.minusDays(1)));
        }
        policeAcquiredDataRepository.saveAll(docs);

        List<PoliceAcquiredData> all = service.searchAllDatas();

        assertThat(all).hasSize(1200);
        assertThat(ids(all)).doesNotHaveDuplicates();
    }
}
