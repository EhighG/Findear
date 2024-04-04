package com.findear.main.matching.model.dto;

import com.findear.main.board.query.dto.AcquiredBoardDetailResDto;
import com.findear.main.board.query.dto.LostBoardDetailResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter @Setter
@AllArgsConstructor
public class GetBestMatchingsResDto {
    private LostBoardMatchingDto lostBoard;
    private AcquiredBoardMatchingDto acquiredBoard;
    private Float similarityRate;
    private String matchedAt;

    @Builder
    public GetBestMatchingsResDto(LostBoardDetailResDto lostBoardDto, AcquiredBoardDetailResDto acquiredBoardDto,
                                  Float similarityRate, LocalDateTime matchedAt, Boolean isLost112Data) {
        this.lostBoard = LostBoardMatchingDto.fromDetailResDto(lostBoardDto);
        this.acquiredBoard = AcquiredBoardMatchingDto.fromDetailResDto(acquiredBoardDto, isLost112Data);
        this.similarityRate = similarityRate;
        this.matchedAt = matchedAt.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
