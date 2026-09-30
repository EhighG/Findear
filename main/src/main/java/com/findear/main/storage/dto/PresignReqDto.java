package com.findear.main.storage.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PresignReqDto {
    /** image/jpeg, image/png, image/webp, image/gif */
    private String contentType;
    /** 업로드할 파일 크기(바이트). 1 이상 10MB 이하 */
    private Long contentLength;
}
