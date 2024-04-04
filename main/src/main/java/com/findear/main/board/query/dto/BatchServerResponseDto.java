package com.findear.main.board.query.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.findear.main.board.common.domain.Lost112AcquiredBoardDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class BatchServerResponseDto {
    @JsonProperty("status")
    private Integer status;
    @JsonProperty("message")
    private String message;
    @JsonProperty("result")
    private Object result;
}
