package com.findear.main.board.command.service;

import java.time.LocalDate;

/**
 * 분실물 등록 직후 매칭 요청 이벤트 (D-52). 등록 트랜잭션이 커밋된 뒤에 batch `POST /findear/matching`을 호출하려고
 * LostBoardCommandServiceImpl이 발행하고 LostBoardMatchingRequestListener가 받는다.
 * 엔티티를 담지 않는다 (다른 스레드에서 요청 스레드의 영속성 컨텍스트 객체를 건드리지 않기 위해).
 *
 * @param lostBoardId 분실물(tbl_lost_board) id. batch 요청의 lostBoardId이자 알림 대상 조회 키
 */
public record LostBoardMatchingRequestedEvent(Long lostBoardId, String productName, String color, String categoryName,
                                              String description, LocalDate lostAt, Float xPos, Float yPos) {
}
