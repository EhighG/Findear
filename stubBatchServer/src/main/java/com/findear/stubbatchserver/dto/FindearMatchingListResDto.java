package com.findear.stubbatchserver.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FindearMatchingListResDto {
    private List<FindearMatchingResDto> matchingList;
    private int totalCount;
}

