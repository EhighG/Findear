package com.findear.main.matching.model.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class Lost112MatchingListResDto {
    private List<Lost112MatchingDto> matchingList;
    private int totalPageNum;

    public Lost112MatchingListResDto(List<Lost112MatchingDto> matchingList, int totalPageNum) {
        this.matchingList = matchingList;
        this.totalPageNum = totalPageNum;
    }
}
