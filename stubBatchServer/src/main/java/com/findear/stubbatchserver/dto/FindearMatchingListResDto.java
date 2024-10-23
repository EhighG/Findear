package com.findear.stubbatchserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FindearMatchingListResDto {
    private List<FindearMatchingDto> matchingList;
    private int totalCount;
}

