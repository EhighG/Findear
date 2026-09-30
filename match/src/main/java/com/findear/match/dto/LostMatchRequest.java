package com.findear.match.dto;

import java.util.List;

/** POST /matching/lost 요청. */
public record LostMatchRequest(LostBoardRequest lostBoard, List<LostAcquiredRequest> acquiredBoardList) {
}
