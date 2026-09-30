package com.findear.main.board.command.service;

import com.findear.main.board.command.dto.AiGeneratedColumnDto;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.query.repository.BoardQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * match 응답을 습득물 게시글에 반영한다 (D-52). 새 트랜잭션에서 게시글을 id로 다시 읽어 비어 있는 컬럼만 채우고
 * 더티 체킹으로 저장한다 (등록 시점의 엔티티를 merge하지 않으므로 그 사이의 수정을 되돌리지 않는다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcquiredBoardAutoFillService {

    private final BoardQueryRepository boardQueryRepository;

    @Transactional
    public void apply(Long boardId, AiGeneratedColumnDto result) {
        Optional<Board> board = boardQueryRepository.findByIdAndDeleteYnFalse(boardId);
        if (board.isEmpty()) {
            log.info("습득물 자동채움 건너뜀(없거나 삭제된 게시글): boardId={}", boardId);
            return;
        }
        board.get().fillAutoColumns(result.getCategory(), result.getColor(), result.getDescription());
    }
}
