package com.findear.main.common.utils.query;

import lombok.Data;

@Data
public class LocationSearchReqDto {
    // 비어 있으면 VWorld 기본값(size 10, page 1)을 쓴다
    private Long size;
    private Long page;
    private String query;
}
