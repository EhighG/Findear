package com.findear.match.dto;

/** POST /process 요청 (main의 NotFilledBoardDto). */
public record ProcessRequest(String productName, String imgUrl) {
}
