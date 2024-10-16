package com.findear.stubbatchserver.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class Lost112MatchingListResDto {
    private List<Lost112MatchingResDto> matchingList;
    private int totalCount;
}
