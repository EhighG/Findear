package com.findear.main.board.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Board.fillAutoColumns: AI 자동채움은 비어 있는 컬럼만 채운다 (D-52). */
class BoardFillAutoColumnsTest {

    private static Board board() {
        return Board.builder().isLost(false).productName("검정 가죽 지갑").build();
    }

    @DisplayName("빈 컬럼은 응답 값으로 채우고 키워드는 공백으로 이어 붙인다")
    @Test
    void fillsEmptyColumns() {
        Board board = board();

        board.fillAutoColumns("지갑", "검정", List.of("검정", "가죽", "지갑", "소형", "로고"));

        assertThat(board.getCategoryName()).isEqualTo("지갑");
        assertThat(board.getColor()).isEqualTo("검정");
        assertThat(board.getAiDescription()).isEqualTo("검정 가죽 지갑 소형 로고");
    }

    @DisplayName("이미 값이 있는 컬럼은 유지하고 나머지만 채운다")
    @Test
    void keepsExistingValues() {
        Board board = board();
        board.modify("빨강", null, "기타");

        board.fillAutoColumns("지갑", "검정", List.of("가죽"));

        assertThat(board.getCategoryName()).isEqualTo("기타");
        assertThat(board.getColor()).isEqualTo("빨강");
        assertThat(board.getAiDescription()).isEqualTo("가죽");
    }

    @DisplayName("aiDescription이 이미 있으면 유지한다")
    @Test
    void keepsExistingAiDescription() {
        Board board = Board.builder().isLost(false).aiDescription("기존 키워드").build();

        board.fillAutoColumns("지갑", "검정", List.of("가죽"));

        assertThat(board.getAiDescription()).isEqualTo("기존 키워드");
        assertThat(board.getCategoryName()).isEqualTo("지갑");
    }

    @DisplayName("공백뿐인 기존 값은 빈 값으로 취급해 채운다")
    @Test
    void blankExistingValuesAreTreatedAsEmpty() {
        Board board = Board.builder().isLost(false).categoryName("  ").color("").aiDescription(" ").build();

        board.fillAutoColumns("지갑", "검정", List.of("가죽"));

        assertThat(board.getCategoryName()).isEqualTo("지갑");
        assertThat(board.getColor()).isEqualTo("검정");
        assertThat(board.getAiDescription()).isEqualTo("가죽");
    }

    @DisplayName("응답 값이 null·공백이면 컬럼을 바꾸지 않는다")
    @Test
    void blankResponseValuesDoNotOverwrite() {
        Board board = board();

        board.fillAutoColumns(null, "  ", List.of("가죽"));

        assertThat(board.getCategoryName()).isNull();
        assertThat(board.getColor()).isNull();
        assertThat(board.getAiDescription()).isEqualTo("가죽");
    }

    @DisplayName("키워드의 null·공백은 빼고 앞뒤 공백을 제거해 이어 붙인다")
    @Test
    void keywordsAreCleaned() {
        Board board = board();

        board.fillAutoColumns(null, null, Arrays.asList(" 검정 ", null, "", "  ", "가죽"));

        assertThat(board.getAiDescription()).isEqualTo("검정 가죽");
    }

    @DisplayName("키워드가 null·빈 리스트·전부 공백이면 aiDescription을 바꾸지 않는다 (예외 없음)")
    @Test
    void emptyKeywordsKeepAiDescription() {
        Board withValue = Board.builder().isLost(false).aiDescription("기존").build();
        withValue.fillAutoColumns(null, null, List.of());
        assertThat(withValue.getAiDescription()).isEqualTo("기존");

        for (List<String> keywords : Arrays.asList(null, List.<String>of(), Arrays.asList(null, " ", ""))) {
            Board board = board();
            board.fillAutoColumns("지갑", "검정", keywords);
            assertThat(board.getAiDescription()).isNull();
            assertThat(board.getCategoryName()).isEqualTo("지갑");
        }
    }
}
