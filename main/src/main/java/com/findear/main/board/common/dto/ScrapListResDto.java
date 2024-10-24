package com.findear.main.board.common.dto;

import com.findear.main.board.query.dto.AcquiredBoardListResDto;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapListResDto {
    private List<AcquiredBoardListResDto> acquiredBoard;
    private List<Map<String, Object>> lost112AcquiredBoard;
}
