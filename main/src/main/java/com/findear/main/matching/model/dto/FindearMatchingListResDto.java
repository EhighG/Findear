package com.findear.main.matching.model.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class FindearMatchingListResDto {
    private List<FindearMatchingDto> matchingList;
    private int totalPageNum;

    public FindearMatchingListResDto(List<FindearMatchingDto> matchingList, int totalPageNum) {
        this.matchingList = matchingList;
        this.totalPageNum = totalPageNum;
    }
}
