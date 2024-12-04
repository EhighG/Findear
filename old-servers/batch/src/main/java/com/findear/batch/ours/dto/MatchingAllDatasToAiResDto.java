package com.findear.batch.ours.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class MatchingAllDatasToAiResDto {

    List<MatchingFindearDatasToAiResDto> findearDatas;

    List<MatchingPoliceDatasToAiResDto> policeDatas;
}
