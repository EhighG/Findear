package com.findear.batch.ours.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MatchingFindearDatasToAiResDto {

    private Object lostBoardId;

    private Object acquiredBoardId;

    private Object similarityRate;

}
