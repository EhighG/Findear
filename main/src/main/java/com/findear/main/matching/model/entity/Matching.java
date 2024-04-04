package com.findear.main.matching.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class Matching {
    private Long id;
    private Long lostBoardId;
    private Long acquiredBoardId;
    private LocalDateTime matchedAt;
}
