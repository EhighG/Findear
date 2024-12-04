package com.findear.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Lost112MatchingListResDto {
    private List<Lost112MatchingDto> matchingList;
    private long totalCount;
}
