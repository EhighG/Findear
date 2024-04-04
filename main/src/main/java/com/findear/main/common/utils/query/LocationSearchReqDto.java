package com.findear.main.common.utils.query;

import lombok.Data;

@Data
public class LocationSearchReqDto {
    private Long size;
    private Long page;
    private String query;
}
