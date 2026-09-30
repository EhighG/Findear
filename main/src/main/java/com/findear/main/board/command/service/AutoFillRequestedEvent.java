package com.findear.main.board.command.service;

/**
 * 습득물 자동채움 요청 이벤트 (D-52). 등록 트랜잭션이 커밋된 뒤에 match를 호출하려고
 * AcquiredBoardCommandServiceImpl이 발행하고 AutoFillRequestListener가 받는다.
 * 엔티티를 담지 않는다 (다른 스레드에서 요청 스레드의 영속성 컨텍스트 객체를 건드리지 않기 위해).
 *
 * @param imgUrl 첫 이미지 key의 공개 URL
 */
public record AutoFillRequestedEvent(Long boardId, String productName, String imgUrl) {
}
