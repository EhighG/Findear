package com.findear.main.board.command.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostLostBoardResDto {
    private Long boardId;
    private List<MatchingFindearDatasToAiResDto> matchingList;
}
