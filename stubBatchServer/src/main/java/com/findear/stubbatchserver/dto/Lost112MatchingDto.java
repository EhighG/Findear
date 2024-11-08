package com.findear.stubbatchserver.dto;

import com.findear.stubbatchserver.domain.Lost112Acquired;
import com.findear.stubbatchserver.domain.Lost112Matching;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class Lost112MatchingDto {
    private Long policeMatchingLogId;
    private Long lostBoardId;
    private float similarityRate;
    private String matchedAt;

    private Long acquiredBoardId;
    private String atcId;
    private String depPlace;
    private String fdFilePathImg;
    private String fdPrdtNm;
    private String fdSbjt;
    private String clrNm;
    private String fdYmd;
    private String mainPrdtClNm;

    public static Lost112MatchingDto of(Lost112Matching lost112Matching) {
        Lost112Acquired lost112Acquired = lost112Matching.getLost112Acquired();
        return Lost112MatchingDto.builder()
                // 매칭 관련 정보
                .policeMatchingLogId(lost112Matching.getId())
                .lostBoardId(lost112Matching.getLostBoardId())
                .similarityRate(lost112Matching.getSimilarityRate())
                .matchedAt(lost112Matching.getMatchedAt().format(DateTimeFormatter.ISO_DATE))
                // 습득물 상세 정보
                .acquiredBoardId(lost112Acquired.getId())
                .atcId(lost112Acquired.getAtcId())
                .depPlace(lost112Acquired.getDepPlace())
                .fdFilePathImg(lost112Acquired.getFdFilePathImg())
                .fdSbjt(lost112Acquired.getFdSbjt())
                .clrNm(lost112Acquired.getClrNm())
                .fdYmd(lost112Acquired.getFdYmd().format(DateTimeFormatter.ISO_DATE))
                .mainPrdtClNm(lost112Acquired.getMainPrdtClNm())
                .build();
    }
}
