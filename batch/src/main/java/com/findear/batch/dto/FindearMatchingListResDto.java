package com.findear.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FindearMatchingListResDto {
    private List<FindearMatchingDto> matchingList;
    private long totalCount;
}

