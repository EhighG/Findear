package com.findear.batch.ours.service;

import com.findear.batch.common.exception.FindearException;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.ours.dto.SearchFindearBestMatchingListDto;
import com.findear.batch.ours.dto.SearchFindearBoardMatchingListDto;
import com.findear.batch.ours.dto.SearchFindearMatchingListResDto;
import com.findear.batch.ours.dto.SearchPoliceBestMatchingListDto;
import com.findear.batch.ours.dto.SearchPoliceBoardMatchingListDto;
import com.findear.batch.ours.dto.SearchPoliceMatchingListResDto;
import com.findear.batch.ours.dto.SearchScrapBoardReqDto;
import com.findear.batch.ours.dto.SearchScrapBoardResDto;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.exception.PoliceException;
import com.findear.batch.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 매칭 로그(findear_matching_log, police_matching_log) 조회와 스크랩 조회를 Spring Data Elasticsearch로 옮긴 뒤의 동작 확인.
 * 핵심: 한 분실물에 로그가 10건을 넘어도 similarityRate 내림차순·페이지·totalCount(전체 건수)가 맞아야 한다
 * (팀 코드는 ES 기본값 10건만 보고 그 안에서 페이지를 잘랐다).
 */
class MatchingLogSearchTest extends IntegrationTestBase {

    private static final LocalDateTime MATCHED_AT = LocalDateTime.of(2026, 10, 1, 10, 0, 0);

    @Autowired
    FindearDataService findearDataService;
    @Autowired
    PoliceDataService policeDataService;

    @BeforeEach
    void insertBoards() {
        insertMember(1);
        insertMember(2);
        // 회원 1: 분실물 1, 2 / 회원 2: 분실물 3, 4(로그 없음)
        insertBoard(101, 1, true, "지갑", "검정", "지갑", "d", LocalDateTime.now());
        insertBoard(102, 1, true, "전자기기", "흰", "이어폰", "d", LocalDateTime.now());
        insertBoard(103, 2, true, "가방", "검정", "가방", "d", LocalDateTime.now());
        insertBoard(104, 2, true, "가방", "빨강", "가방2", "d", LocalDateTime.now());
        insertLostBoard(1, 101, LocalDate.now(), 1f, 1f);
        insertLostBoard(2, 102, LocalDate.now(), 1f, 1f);
        insertLostBoard(3, 103, LocalDate.now(), 1f, 1f);
        insertLostBoard(4, 104, LocalDate.now(), 1f, 1f);
    }

    private FindearMatchingLog findearLog(long id, long lostBoardId, long acquiredBoardId, float rate) {
        return FindearMatchingLog.builder().findearMatchingLogId(String.valueOf(id)).lostBoardId(lostBoardId)
                .acquiredBoardId(acquiredBoardId).similarityRate(rate).matchingAt(MATCHED_AT).build();
    }

    private PoliceMatchingLog policeLog(long id, long lostBoardId, String atcId, float rate) {
        return PoliceMatchingLog.builder().policeMatchingLogId(String.valueOf(id)).lostBoardId(lostBoardId).similarityRate(rate)
                .matchingAt(MATCHED_AT).acquiredBoardId("5" + id).atcId(atcId).depPlace("종로경찰서")
                .fdFilePathImg("https://img/" + atcId).fdPrdtNm("물품").fdSbjt("제목 " + atcId).clrNm("검정")
                .fdYmd(LocalDate.of(2026, 9, 30)).mainPrdtClNm("지갑").build();
    }

    /** 분실물 1에 로그 12건(점수 0.10~0.90 사이 섞어서 저장), 분실물 2에 3건, 분실물 3에 1건 */
    private void saveFindearLogs() {
        List<FindearMatchingLog> logs = new ArrayList<>();
        float[] shuffled = {0.55f, 0.15f, 0.95f, 0.35f, 0.75f, 0.25f, 0.85f, 0.05f, 0.65f, 0.45f, 0.99f, 0.11f};
        for (int i = 0; i < shuffled.length; i++) {
            logs.add(findearLog(i + 1, 1, 200 + i, shuffled[i]));
        }
        logs.add(findearLog(101, 2, 301, 0.3f));
        logs.add(findearLog(102, 2, 302, 0.9f));
        logs.add(findearLog(103, 2, 303, 0.6f));
        logs.add(findearLog(201, 3, 401, 0.7f));
        findearMatchingLogRepository.saveAll(logs);
    }

    private List<Float> rates(List<SearchFindearMatchingListResDto> list) {
        return list.stream().map(SearchFindearMatchingListResDto::getSimilarityRate).toList();
    }

    @DisplayName("findear 분실물별 목록: 11건 이상에서 similarityRate 내림차순, 페이지 자르기, totalCount는 전체 건수")
    @Test
    void findearBoardMatchingList() {
        saveFindearLogs();

        SearchFindearBoardMatchingListDto page1 = findearDataService.searchBoardMatchingList(1, 5, 1L);
        SearchFindearBoardMatchingListDto page2 = findearDataService.searchBoardMatchingList(2, 5, 1L);
        SearchFindearBoardMatchingListDto page3 = findearDataService.searchBoardMatchingList(3, 5, 1L);
        SearchFindearBoardMatchingListDto page4 = findearDataService.searchBoardMatchingList(4, 5, 1L);

        assertThat(rates(page1.getMatchingList())).containsExactly(0.99f, 0.95f, 0.85f, 0.75f, 0.65f);
        assertThat(rates(page2.getMatchingList())).containsExactly(0.55f, 0.45f, 0.35f, 0.25f, 0.15f);
        assertThat(rates(page3.getMatchingList())).containsExactly(0.11f, 0.05f);
        assertThat(page4.getMatchingList()).isEmpty();
        // 10건이 아니라 전체 12건
        assertThat(page1.getTotalCount()).isEqualTo(12);
        assertThat(page2.getTotalCount()).isEqualTo(12);
        assertThat(page4.getTotalCount()).isEqualTo(12);

        // 항목 필드: 분실물 ID는 요청한 ID, 나머지는 로그 그대로
        SearchFindearMatchingListResDto first = page1.getMatchingList().get(0);
        assertThat(first.getFindearMatchingLogId()).isEqualTo("11");
        assertThat(first.getLostBoardId()).isEqualTo(1L);
        assertThat(first.getAcquiredBoardId()).isEqualTo(210L);
        assertThat(first.getMatchedAt()).isEqualTo("2026-10-01T10:00:00");
    }

    @DisplayName("findear 분실물별 목록: size 6(기본값) 한 페이지에 6건, 다른 분실물 로그는 섞이지 않는다")
    @Test
    void findearBoardMatchingListOtherBoards() {
        saveFindearLogs();

        SearchFindearBoardMatchingListDto board1 = findearDataService.searchBoardMatchingList(1, 6, 1L);
        SearchFindearBoardMatchingListDto board2 = findearDataService.searchBoardMatchingList(1, 6, 2L);

        assertThat(board1.getMatchingList()).hasSize(6);
        assertThat(board2.getTotalCount()).isEqualTo(3);
        assertThat(rates(board2.getMatchingList())).containsExactly(0.9f, 0.6f, 0.3f);
        assertThat(board2.getMatchingList()).allMatch(m -> m.getLostBoardId() == 2L);
    }

    @DisplayName("findear 분실물별 목록: 없는 분실물은 FindearException, 로그가 없으면 빈 목록과 totalCount 0")
    @Test
    void findearBoardMatchingListEmptyAndMissing() {
        saveFindearLogs();

        assertThatThrownBy(() -> findearDataService.searchBoardMatchingList(1, 6, 999L)).isInstanceOf(FindearException.class);

        SearchFindearBoardMatchingListDto noLogs = findearDataService.searchBoardMatchingList(1, 6, 4L);
        assertThat(noLogs.getMatchingList()).isEmpty();
        assertThat(noLogs.getTotalCount()).isZero();
    }

    @DisplayName("findear 회원별 최고 점수: 회원의 분실물마다 최고 점수 1건, 로그 없는 분실물은 빠진다")
    @Test
    void findearBestMatchingList() {
        saveFindearLogs();

        SearchFindearBestMatchingListDto member1 = findearDataService.searchBestMatchingList(1, 6, 1L);
        SearchFindearBestMatchingListDto member2 = findearDataService.searchBestMatchingList(1, 6, 2L);
        SearchFindearBestMatchingListDto noBoards = findearDataService.searchBestMatchingList(1, 6, 99L);

        assertThat(member1.getTotalCount()).isEqualTo(2);
        assertThat(member1.getMatchingList()).extracting(SearchFindearMatchingListResDto::getLostBoardId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(member1.getMatchingList()).filteredOn(m -> m.getLostBoardId() == 1L)
                .singleElement().satisfies(m -> {
                    assertThat(m.getSimilarityRate()).isEqualTo(0.99f);
                    assertThat(m.getAcquiredBoardId()).isEqualTo(210L);
                });
        assertThat(member1.getMatchingList()).filteredOn(m -> m.getLostBoardId() == 2L)
                .singleElement().satisfies(m -> assertThat(m.getSimilarityRate()).isEqualTo(0.9f));

        // 회원 2: 분실물 3(로그 1건), 4(로그 없음)
        assertThat(member2.getTotalCount()).isEqualTo(1);
        assertThat(member2.getMatchingList().get(0).getLostBoardId()).isEqualTo(3L);
        assertThat(member2.getMatchingList().get(0).getSimilarityRate()).isEqualTo(0.7f);

        assertThat(noBoards.getMatchingList()).isEmpty();
        assertThat(noBoards.getTotalCount()).isZero();
    }

    @DisplayName("findear 회원별 최고 점수: 페이지는 최고 점수 목록을 size씩 자르고 totalCount는 그 목록 건수")
    @Test
    void findearBestMatchingListPaging() {
        saveFindearLogs();

        SearchFindearBestMatchingListDto page1 = findearDataService.searchBestMatchingList(1, 1, 1L);
        SearchFindearBestMatchingListDto page2 = findearDataService.searchBestMatchingList(2, 1, 1L);
        SearchFindearBestMatchingListDto page3 = findearDataService.searchBestMatchingList(3, 1, 1L);

        assertThat(page1.getMatchingList()).hasSize(1);
        assertThat(page2.getMatchingList()).hasSize(1);
        assertThat(page3.getMatchingList()).isEmpty();
        assertThat(page1.getTotalCount()).isEqualTo(2);
        assertThat(page1.getMatchingList().get(0).getLostBoardId()).isNotEqualTo(page2.getMatchingList().get(0).getLostBoardId());
    }

    @DisplayName("searchAllFindearMatchingList: 로그 전체를 scroll로 읽는다")
    @Test
    void findearAllMatchingList() {
        List<FindearMatchingLog> logs = new ArrayList<>();
        for (long i = 1; i <= 1100; i++) {
            logs.add(findearLog(i, 1, i, 0.5f));
        }
        findearMatchingLogRepository.saveAll(logs);

        assertThat(findearDataService.searchAllFindearMatchingList()).hasSize(1100);
    }

    /** 분실물 1에 Lost112 로그 12건, 분실물 2에 2건 */
    private void savePoliceLogs() {
        List<PoliceMatchingLog> logs = new ArrayList<>();
        float[] shuffled = {0.55f, 0.15f, 0.95f, 0.35f, 0.75f, 0.25f, 0.85f, 0.05f, 0.65f, 0.45f, 0.99f, 0.11f};
        for (int i = 0; i < shuffled.length; i++) {
            logs.add(policeLog(i + 1, 1, "A" + (1000 + i), shuffled[i]));
        }
        logs.add(policeLog(101, 2, "B1", 0.4f));
        logs.add(policeLog(102, 2, "B2", 0.8f));
        policeMatchingLogRepository.saveAll(logs);
    }

    private List<String> policeRates(List<SearchPoliceMatchingListResDto> list) {
        return list.stream().map(SearchPoliceMatchingListResDto::getSimilarityRate).toList();
    }

    @DisplayName("lost112 분실물별 목록: 11건 이상에서 similarityRate 내림차순, 페이지 자르기, totalCount는 전체 건수")
    @Test
    void policeBoardMatchingList() {
        savePoliceLogs();

        SearchPoliceBoardMatchingListDto page1 = policeDataService.searchPoliceBoardMatchingList(1, 5, 1L);
        SearchPoliceBoardMatchingListDto page2 = policeDataService.searchPoliceBoardMatchingList(2, 5, 1L);
        SearchPoliceBoardMatchingListDto page3 = policeDataService.searchPoliceBoardMatchingList(3, 5, 1L);
        SearchPoliceBoardMatchingListDto page4 = policeDataService.searchPoliceBoardMatchingList(4, 5, 1L);

        assertThat(policeRates(page1.getMatchingList())).containsExactly("0.99", "0.95", "0.85", "0.75", "0.65");
        assertThat(policeRates(page2.getMatchingList())).containsExactly("0.55", "0.45", "0.35", "0.25", "0.15");
        assertThat(policeRates(page3.getMatchingList())).containsExactly("0.11", "0.05");
        assertThat(page4.getMatchingList()).isEmpty();
        assertThat(page1.getTotalCount()).isEqualTo(12);
        assertThat(page3.getTotalCount()).isEqualTo(12);
        assertThat(page4.getTotalCount()).isEqualTo(12);

        // 항목 필드: 전부 문자열 (응답 JSON 모양은 팀 코드와 같다)
        SearchPoliceMatchingListResDto first = page1.getMatchingList().get(0);
        assertThat(first.getPoliceMatchingLogId()).isEqualTo("11");
        assertThat(first.getLostBoardId()).isEqualTo("1");
        assertThat(first.getMatchedAt()).isEqualTo("2026-10-01T10:00:00");
        assertThat(first.getAcquiredBoardId()).isEqualTo("511");
        assertThat(first.getAtcId()).isEqualTo("A1010");
        assertThat(first.getDepPlace()).isEqualTo("종로경찰서");
        assertThat(first.getFdFilePathImg()).isEqualTo("https://img/A1010");
        assertThat(first.getFdPrdtNm()).isEqualTo("물품");
        assertThat(first.getFdSbjt()).isEqualTo("제목 A1010");
        assertThat(first.getClrNm()).isEqualTo("검정");
        assertThat(first.getFdYmd()).isEqualTo("2026-09-30");
        assertThat(first.getMainPrdtClNm()).isEqualTo("지갑");

        assertThat(policeDataService.searchPoliceBoardMatchingList(1, 6, 2L).getTotalCount()).isEqualTo(2);
        assertThatThrownBy(() -> policeDataService.searchPoliceBoardMatchingList(1, 6, 999L)).isInstanceOf(PoliceException.class);
    }

    @DisplayName("lost112 회원별 최고 점수: 분실물마다 최고 점수 1건")
    @Test
    void policeBestMatchingList() {
        savePoliceLogs();

        SearchPoliceBestMatchingListDto member1 = policeDataService.searchPoliceBestMatchingList(1, 6, 1L);

        assertThat(member1.getTotalCount()).isEqualTo(2);
        assertThat(member1.getMatchingList()).filteredOn(m -> m.getLostBoardId().equals("1"))
                .singleElement().satisfies(m -> {
                    assertThat(m.getSimilarityRate()).isEqualTo("0.99");
                    assertThat(m.getAtcId()).isEqualTo("A1010");
                });
        assertThat(member1.getMatchingList()).filteredOn(m -> m.getLostBoardId().equals("2"))
                .singleElement().satisfies(m -> assertThat(m.getAtcId()).isEqualTo("B2"));

        // 회원 2는 로그가 없는 분실물만 있다 (인덱스에는 분실물 1, 2의 로그가 있어 정렬 필드는 매핑돼 있음)
        SearchPoliceBestMatchingListDto member2 = policeDataService.searchPoliceBestMatchingList(1, 6, 2L);
        assertThat(member2.getMatchingList()).isEmpty();
        assertThat(member2.getTotalCount()).isZero();
    }

    private PoliceAcquiredData acquired(String atcId, String suffix) {
        return PoliceAcquiredData.builder().id(atcId).atcId(atcId).depPlace("종로경찰서").fdFilePathImg("https://img/" + atcId)
                .fdPrdtNm("물품" + suffix).fdSbjt("제목 " + suffix).clrNm("검정").fdYmd(LocalDate.of(2026, 9, 30)).prdtClNm("지갑 > 반지갑")
                .mainPrdtClNm("지갑").subPrdtClNm("반지갑").build();
    }

    @DisplayName("스크랩 조회: atcIdList를 terms 한 번으로 찾고 요청 순서대로 돌려주며 없는 atcId는 빠진다. id는 atcId 문자열")
    @Test
    void searchScrapBoard() {
        policeAcquiredDataRepository.saveAll(List.of(acquired("F20260930A", "1"), acquired("F20260930B", "2"), acquired("F20260930C", "3")));

        List<SearchScrapBoardResDto> result = policeDataService.searchScrapBoard(
                SearchScrapBoardReqDto.builder().atcIdList(List.of("F20260930C", "없는ID", "F20260930A")).build());

        assertThat(result).extracting(SearchScrapBoardResDto::getAtcId).containsExactly("F20260930C", "F20260930A");
        SearchScrapBoardResDto first = result.get(0);
        assertThat(first.getId()).isEqualTo("F20260930C");
        assertThat(first.getDepPlace()).isEqualTo("종로경찰서");
        assertThat(first.getFdFilePathImg()).isEqualTo("https://img/F20260930C");
        assertThat(first.getFdPrdtNm()).isEqualTo("물품3");
        assertThat(first.getFdSbjt()).isEqualTo("제목 3");
        assertThat(first.getClrNm()).isEqualTo("검정");
        assertThat(first.getFdYmd()).isEqualTo("2026-09-30");
        assertThat(first.getPrdtClNm()).isEqualTo("지갑 > 반지갑");
        assertThat(first.getMainPrdtClNm()).isEqualTo("지갑");
        assertThat(first.getSubPrdtClNm()).isEqualTo("반지갑");

        assertThat(policeDataService.searchScrapBoard(SearchScrapBoardReqDto.builder().atcIdList(List.of("없는ID")).build())).isEmpty();
        assertThat(policeDataService.searchScrapBoard(SearchScrapBoardReqDto.builder().atcIdList(List.of()).build())).isEmpty();
    }

    @DisplayName("스크랩 조회: 값이 없는 필드는 null로 나오고 다른 필드는 그대로다")
    @Test
    void searchScrapBoardWithNullFields() {
        policeAcquiredDataRepository.save(PoliceAcquiredData.builder().id("F20260930N").atcId("F20260930N").fdPrdtNm("물품")
                .fdYmd(LocalDate.of(2026, 9, 30)).mainPrdtClNm("지갑").build());

        List<SearchScrapBoardResDto> result = policeDataService.searchScrapBoard(
                SearchScrapBoardReqDto.builder().atcIdList(List.of("F20260930N")).build());

        assertThat(result).hasSize(1);
        SearchScrapBoardResDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo("F20260930N");
        assertThat(dto.getClrNm()).isNull();
        assertThat(dto.getDepPlace()).isNull();
        assertThat(dto.getFdSbjt()).isNull();
        assertThat(dto.getFdPrdtNm()).isEqualTo("물품");
        assertThat(dto.getMainPrdtClNm()).isEqualTo("지갑");
    }
}
