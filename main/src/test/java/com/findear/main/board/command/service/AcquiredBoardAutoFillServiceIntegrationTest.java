package com.findear.main.board.command.service;

import com.findear.main.board.command.dto.AiGeneratedColumnDto;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.BoardStatus;
import com.findear.main.board.query.repository.BoardQueryRepository;
import com.findear.main.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** 자동채움 반영(D-52): 실제 MySQL(Testcontainers)에서 게시글을 id로 다시 읽어 빈 컬럼만 채우는지 확인한다. */
@IntegrationTest
class AcquiredBoardAutoFillServiceIntegrationTest {

    @Autowired AcquiredBoardAutoFillService autoFillService;
    @Autowired BoardCommandRepository boardCommandRepository;
    @Autowired BoardQueryRepository boardQueryRepository;

    private static AiGeneratedColumnDto result() {
        AiGeneratedColumnDto dto = new AiGeneratedColumnDto();
        dto.setCategory("지갑");
        dto.setColor("검정");
        dto.setDescription(List.of("검정", "가죽", "지갑", "소형", "로고"));
        return dto;
    }

    private Board saveBoard(Board.BoardBuilder builder) {
        return boardCommandRepository.save(builder.isLost(false).productName("검정 가죽 지갑").status(BoardStatus.ONGOING).build());
    }

    private Board reload(Board board) {
        return boardQueryRepository.findById(board.getId()).orElseThrow();
    }

    @DisplayName("빈 게시글은 category·color·ai_description이 채워진다")
    @Test
    void fillsEmptyBoard() {
        Board board = saveBoard(Board.builder());

        autoFillService.apply(board.getId(), result());

        Board saved = reload(board);
        assertThat(saved.getCategoryName()).isEqualTo("지갑");
        assertThat(saved.getColor()).isEqualTo("검정");
        assertThat(saved.getAiDescription()).isEqualTo("검정 가죽 지갑 소형 로고");
    }

    @DisplayName("category가 미리 있는 게시글은 category를 유지하고 나머지만 반영한다")
    @Test
    void keepsExistingCategory() {
        Board board = saveBoard(Board.builder().categoryName("기타"));

        autoFillService.apply(board.getId(), result());

        Board saved = reload(board);
        assertThat(saved.getCategoryName()).isEqualTo("기타");
        assertThat(saved.getColor()).isEqualTo("검정");
        assertThat(saved.getAiDescription()).isEqualTo("검정 가죽 지갑 소형 로고");
    }

    @DisplayName("삭제된 게시글은 바뀌지 않고 예외도 없다")
    @Test
    void skipsDeletedBoard() {
        Board board = saveBoard(Board.builder().deleteYn(true));

        assertThatCode(() -> autoFillService.apply(board.getId(), result())).doesNotThrowAnyException();

        Board saved = reload(board);
        assertThat(saved.getCategoryName()).isNull();
        assertThat(saved.getColor()).isNull();
        assertThat(saved.getAiDescription()).isNull();
    }

    @DisplayName("없는 id는 예외 없이 끝난다")
    @Test
    void skipsMissingBoard() {
        assertThatCode(() -> autoFillService.apply(999_999_999L, result())).doesNotThrowAnyException();
    }
}
