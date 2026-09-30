package com.findear.batch.ours.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class MatchingPoliceDatasToAiReqDto {

    private LostBoardMatchingDto lostBoard;

    private List<PoliceAcquiredBoardMatchingDto> acquiredBoardList;
}
