package com.findear.batch.ours.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
@Builder
public class SearchPoliceBoardMatchingListDto {

    List<SearchPoliceMatchingListResDto> matchingList;

    private int totalCount;
}
