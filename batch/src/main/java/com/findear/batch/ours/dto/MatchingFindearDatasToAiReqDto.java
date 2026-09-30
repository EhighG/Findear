package com.findear.batch.ours.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class MatchingFindearDatasToAiReqDto {

    private LostBoardMatchingDto lostBoard;

    private List<AcquiredBoardMatchingDto> acquiredBoardList;

}
