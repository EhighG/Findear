package com.findear.stubbatchserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Lost112MatchingListResDto {
    private List<Lost112MatchingDto> matchingList;
    private int totalCount;
}
