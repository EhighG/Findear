package com.findear.batch.ours.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.findear.batch.common.exception.FindearException;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.dto.MatchingAllDatasToAiResDto;
import com.findear.batch.ours.dto.MatchingFindearDatasToAiResDto;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.PoliceDocs;
import com.findear.batch.support.MatchMock;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * match 호출 계약 테스트 (07 §5.2, §5.3). match는 호출하지 않고 mock 서버(mockwebserver3)로 요청 모양과 응답 처리를 확인한다.
 * 요청 키는 match 쪽 계약 픽스처(match/src/test/resources/contracts)와 같아야 한다.
 */
class FindearDataServiceMatchingTest extends IntegrationTestBase {

    private static final Path CONTRACTS = Path.of("../match/src/test/resources/contracts");
    private static final LocalDate LOST_AT = LocalDate.now().minusDays(3);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    FindearDataService findearDataService;

    @BeforeEach
    void insertData() {
        insertMember(1);
        insertMember(2);
        // 분실물: 지갑, 3일 전 분실. 습득물: 지갑(등록 2일 전, 포함), 지갑(등록 5일 전, 분실일 이전이라 제외), 전자기기(제외)
        insertBoard(10, 1, true, "지갑", "검정", "검은색 가죽 지갑", "검정 가죽 지갑 카드 수납", LocalDateTime.now().minusDays(3));
        insertLostBoard(1, 10, LOST_AT, 126.9707f, 37.5547f);
        insertBoard(11, 2, false, "지갑", "검정", "검은색 반지갑", "검정 가죽 반지갑", LocalDateTime.now().minusDays(2));
        insertAcquiredBoard(7, 11, 126.97f, 37.55f);
        insertBoard(12, 2, false, "지갑", "갈색", "갈색 지갑", null, LocalDateTime.now().minusDays(5));
        insertAcquiredBoard(8, 12, 126.98f, 37.56f);
        insertBoard(13, 2, false, "전자기기", "흰색", "이어폰", "무선 이어폰", LocalDateTime.now().minusDays(2));
        insertAcquiredBoard(9, 13, 127.0f, 37.5f);
    }

    private LostBoardMatchingDto lostBoardDto() {
        return LostBoardMatchingDto.builder()
                .lostBoardId("1").productName("검은색 가죽 지갑").color("검정").categoryName("지갑")
                .description("검정 가죽 지갑 카드 수납").lostAt(LOST_AT.toString()).xPos("126.9707").yPos("37.5547").build();
    }

    private void saveLost112Docs() {
        policeAcquiredDataRepository.saveAll(List.of(
                lost112Doc("F5001", "지갑", LOST_AT.plusDays(1)),     // 포함
                lost112Doc("F5002", "휴대폰", LOST_AT.plusDays(1)),   // 카테고리 다름
                lost112Doc("F5003", "지갑", LOST_AT.minusDays(1)),    // 분실일 이전
                lost112Doc("F5004", "지갑", LOST_AT)));                // 분실일 당일 (포함)
    }

    private PoliceAcquiredData lost112Doc(String atcId, String category, LocalDate fdYmd) {
        return PoliceDocs.builder(atcId, category, fdYmd).fdFilePathImg("https://www.lost112.go.kr/img/" + atcId).build();
    }

    /** 07 §5.2/§5.3 모양의 응답을 돌려주는 match mock: findear는 첫 후보만 0.85, lost는 모든 후보를 0.7로 */
    private void respondLikeMatch() {
        MATCH.respondWith(request -> {
            try {
                JsonNode body = mapper.readTree(request.getBody().utf8());
                String path = request.getUrl().encodedPath();
                ObjectNode response = mapper.createObjectNode();
                ArrayNode result = response.putArray("result");
                if (path.equals("/matching/findear")) {
                    response.put("message", "해당 분실물과 findear 데이터와의 매칭이 완료되었습니다");
                    for (JsonNode candidate : body.get("acquiredBoardList")) {
                        ObjectNode item = result.addObject();
                        item.put("lostBoardId", body.get("lostBoard").get("lostBoardId").asLong());
                        item.put("acquiredBoardId", candidate.get("acquiredBoardId").asLong());
                        item.put("similarityRate", 0.85);
                    }
                } else if (path.equals("/matching/lost")) {
                    response.put("message", "해당 분실물과 lost112 데이터와의 매칭이 완료되었습니다");
                    for (JsonNode candidate : body.get("acquiredBoardList")) {
                        ObjectNode item = result.addObject();
                        item.put("lostBoardId", body.get("lostBoard").get("lostBoardId").asLong());
                        item.put("acquiredBoardId", candidate.get("id").asText()); // id는 atcId 문자열 (match mock도 정수가 아니면 문자열로 돌려준다)
                        item.put("similarityRate", 0.7);
                        for (String key : new String[]{"atcId", "depPlace", "fdFilePathImg", "fdPrdtNm", "fdSbjt", "clrNm", "fdYmd", "mainPrdtClNm"}) {
                            item.set(key, candidate.get(key));
                        }
                    }
                } else {
                    return MatchMock.json(404, "{\"message\":\"없는 경로\"}");
                }
                return MatchMock.json(200, mapper.writeValueAsString(response));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private Set<String> keys(JsonNode node) {
        Set<String> keys = new HashSet<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            keys.add(it.next());
        }
        return keys;
    }

    private JsonNode fixture(String name) throws IOException {
        return mapper.readTree(Files.readString(CONTRACTS.resolve(name)));
    }

    private JsonNode body(RecordedRequest request) throws IOException {
        return mapper.readTree(request.getBody().utf8());
    }

    @DisplayName("POST /matching/findear: 요청 본문 키가 계약 픽스처와 같고, 후보는 같은 카테고리·분실일 이후 습득물 게시글(board_id)뿐")
    @Test
    void findearRequestShape() throws Exception {
        respondLikeMatch();

        findearDataService.matchingFindearDatas(lostBoardDto());

        List<RecordedRequest> requests = MATCH.requests();
        assertThat(requests).hasSize(2);
        RecordedRequest request = requests.get(0);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/matching/findear");
        assertThat(request.getHeaders().get("Content-Type")).startsWith("application/json");

        JsonNode actual = body(request);
        JsonNode contract = fixture("matching-findear-request.json");
        assertThat(keys(actual)).isEqualTo(keys(contract));
        assertThat(keys(actual.get("lostBoard"))).isEqualTo(keys(contract.get("lostBoard")));
        assertThat(actual.get("acquiredBoardList")).hasSize(1);
        assertThat(keys(actual.get("acquiredBoardList").get(0))).isEqualTo(keys(contract.get("acquiredBoardList").get(0)));

        // 값은 전부 문자열이고 xpos/ypos 키로 나간다
        JsonNode lost = actual.get("lostBoard");
        assertThat(lost.get("lostBoardId").asText()).isEqualTo("1");
        assertThat(lost.get("categoryName").asText()).isEqualTo("지갑");
        assertThat(lost.get("lostAt").asText()).isEqualTo(LOST_AT.toString());
        assertThat(lost.get("xpos").isTextual()).isTrue();
        assertThat(lost.get("xpos").asText()).isEqualTo("126.9707");
        assertThat(lost.get("ypos").asText()).isEqualTo("37.5547");

        // acquiredBoardId는 습득물 "게시글" board_id (11), 습득물 테이블 PK(7)가 아니다 (지금 코드 그대로)
        JsonNode candidate = actual.get("acquiredBoardList").get(0);
        assertThat(candidate.get("acquiredBoardId").asText()).isEqualTo("11");
        assertThat(candidate.get("productName").asText()).isEqualTo("검은색 반지갑");
        assertThat(candidate.get("color").asText()).isEqualTo("검정");
        assertThat(candidate.get("categoryName").asText()).isEqualTo("지갑");
        assertThat(candidate.get("description").asText()).isEqualTo("검정 가죽 반지갑");
        assertThat(candidate.get("xpos").asText()).isEqualTo("126.97");
        assertThat(candidate.get("ypos").asText()).isEqualTo("37.55");
        assertThat(candidate.get("registeredAt").asText()).startsWith(LocalDate.now().minusDays(2).toString());
    }

    @DisplayName("POST /matching/lost: Lost112 후보는 같은 카테고리이고 fdYmd가 분실일 이후인 문서뿐, 키는 계약 픽스처와 같다")
    @Test
    void lostRequestShape() throws Exception {
        respondLikeMatch();
        saveLost112Docs();

        findearDataService.matchingFindearDatas(lostBoardDto());

        List<RecordedRequest> requests = MATCH.requests();
        assertThat(requests).hasSize(2);
        RecordedRequest request = requests.get(1);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/matching/lost");

        JsonNode actual = body(request);
        JsonNode contract = fixture("matching-lost-request.json");
        assertThat(keys(actual)).isEqualTo(keys(contract));
        assertThat(keys(actual.get("lostBoard"))).isEqualTo(keys(contract.get("lostBoard")));

        List<String> atcIds = new ArrayList<>();
        for (JsonNode candidate : actual.get("acquiredBoardList")) {
            assertThat(keys(candidate)).isEqualTo(keys(contract.get("acquiredBoardList").get(0)));
            atcIds.add(candidate.get("atcId").asText());
        }
        // 5002(다른 카테고리)·5003(분실일 이전)은 빠진다
        assertThat(atcIds).containsExactlyInAnyOrder("F5001", "F5004");

        JsonNode first = null;
        for (JsonNode candidate : actual.get("acquiredBoardList")) {
            if (candidate.get("atcId").asText().equals("F5001")) {
                first = candidate;
            }
        }
        assertThat(first).isNotNull();
        assertThat(first.get("id").asText()).isEqualTo("F5001"); // 문서 ID = atcId
        assertThat(first.get("depPlace").asText()).isEqualTo("종로경찰서");
        assertThat(first.get("fdFilePathImg").asText()).isEqualTo("https://www.lost112.go.kr/img/F5001");
        assertThat(first.get("fdPrdtNm").asText()).isEqualTo("물품 F5001");
        assertThat(first.get("fdSbjt").asText()).isEqualTo("제목 F5001");
        assertThat(first.get("clrNm").asText()).isEqualTo("검정");
        assertThat(first.get("fdYmd").asText()).isEqualTo(LOST_AT.plusDays(1).toString());
        assertThat(first.get("mainPrdtClNm").asText()).isEqualTo("지갑");
    }

    @DisplayName("07 §5 모양의 응답은 반환 DTO(findearDatas, policeDatas)를 채우고 ES 매칭 로그를 저장한다")
    @Test
    void responseFillsDtoAndSavesLogs() {
        respondLikeMatch();
        saveLost112Docs();

        MatchingAllDatasToAiResDto result = findearDataService.matchingFindearDatas(lostBoardDto());

        assertThat(result.getFindearDatas()).hasSize(1);
        MatchingFindearDatasToAiResDto findear = result.getFindearDatas().get(0);
        assertThat(String.valueOf(findear.getLostBoardId())).isEqualTo("1");
        assertThat(String.valueOf(findear.getAcquiredBoardId())).isEqualTo("11");
        assertThat(String.valueOf(findear.getSimilarityRate())).isEqualTo("0.85");

        assertThat(result.getPoliceDatas()).hasSize(2);
        assertThat(result.getPoliceDatas()).extracting(p -> String.valueOf(p.getAtcId())).containsExactlyInAnyOrder("F5001", "F5004");
        assertThat(result.getPoliceDatas()).allSatisfy(p -> {
            assertThat(String.valueOf(p.getLostBoardId())).isEqualTo("1");
            assertThat(String.valueOf(p.getSimilarityRate())).isEqualTo("0.7");
            assertThat(p.getDepPlace()).isEqualTo("종로경찰서");
            assertThat(p.getMainPrdtClNm()).isEqualTo("지갑");
        });

        // ES 매칭 로그
        List<FindearMatchingLog> findearLogs = new ArrayList<>();
        findearMatchingLogRepository.findAll().forEach(findearLogs::add);
        assertThat(findearLogs).hasSize(1);
        assertThat(findearLogs.get(0).getLostBoardId()).isEqualTo(1L);
        assertThat(findearLogs.get(0).getAcquiredBoardId()).isEqualTo(11L);
        assertThat(findearLogs.get(0).getSimilarityRate()).isEqualTo(0.85f);

        List<PoliceMatchingLog> policeLogs = new ArrayList<>();
        policeMatchingLogRepository.findAll().forEach(policeLogs::add);
        assertThat(policeLogs).hasSize(2);
        assertThat(policeLogs).extracting(PoliceMatchingLog::getAtcId).containsExactlyInAnyOrder("F5001", "F5004");
        assertThat(policeLogs).allSatisfy(l -> {
            assertThat(l.getLostBoardId()).isEqualTo(1L);
            assertThat(l.getSimilarityRate()).isEqualTo(0.7f);
        });
    }

    @DisplayName("match가 결과 없음(result: null)을 주면 빈 목록이고 로그를 저장하지 않는다")
    @Test
    void nullResultIsEmpty() {
        MATCH.respondWith(request -> MatchMock.json(200, "{\"message\":\"없습니다\",\"result\":null}"));
        saveLost112Docs();

        MatchingAllDatasToAiResDto result = findearDataService.matchingFindearDatas(lostBoardDto());

        assertThat(result.getFindearDatas()).isEmpty();
        assertThat(result.getPoliceDatas()).isEmpty();
        assertThat(findearMatchingLogRepository.count()).isZero();
        assertThat(policeMatchingLogRepository.count()).isZero();
    }

    @DisplayName("match가 500을 주면 FindearException")
    @Test
    void matchServerErrorThrows() {
        MATCH.respondWith(request -> MatchMock.json(500, "{\"message\":\"서버 오류\"}"));

        assertThatThrownBy(() -> findearDataService.matchingFindearDatas(lostBoardDto()))
                .isInstanceOf(FindearException.class);
        assertThat(findearMatchingLogRepository.count()).isZero();
    }

    @DisplayName("lost112 매칭 호출만 500이면 FindearException (findear 로그는 이미 저장된 상태)")
    @Test
    void lostMatchServerErrorThrows() {
        saveLost112Docs();
        MockResponse findearOk = MatchMock.json(200,
                "{\"message\":\"ok\",\"result\":[{\"lostBoardId\":1,\"acquiredBoardId\":11,\"similarityRate\":0.5}]}");
        MATCH.respondWith(request -> request.getUrl().encodedPath().equals("/matching/findear")
                ? findearOk : MatchMock.json(500, "{\"message\":\"서버 오류\"}"));

        assertThatThrownBy(() -> findearDataService.matchingFindearDatas(lostBoardDto()))
                .isInstanceOf(FindearException.class);
    }

    @DisplayName("lostAt 형식이 날짜가 아니면 FindearException이고 match는 호출하지 않는다")
    @Test
    void invalidLostAt() {
        LostBoardMatchingDto dto = lostBoardDto();
        dto.setLostAt("2026-09-20T10:30:00");

        assertThatThrownBy(() -> findearDataService.matchingFindearDatas(dto)).isInstanceOf(FindearException.class);
        assertThat(MATCH.requests()).isEmpty();
    }
}
