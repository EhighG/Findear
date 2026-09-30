package com.findear.main.board.command.dto;

/**
 * match `POST /process`(07 §5.1) 요청 본문.
 *
 * @param imgUrl 첫 이미지의 공개 URL
 */
public record NotFilledBoardDto(String productName, String imgUrl) {
}
