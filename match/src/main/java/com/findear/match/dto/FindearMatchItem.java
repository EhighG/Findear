package com.findear.match.dto;

/** /matching/findear 결과 항목. */
public record FindearMatchItem(long lostBoardId, long acquiredBoardId, double similarityRate) {
}
