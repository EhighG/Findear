package com.findear.match.dto;

import java.util.List;

/** POST /matching/findear 요청. */
public record FindearMatchRequest(LostBoardRequest lostBoard, List<FindearAcquiredRequest> acquiredBoardList) {
}
