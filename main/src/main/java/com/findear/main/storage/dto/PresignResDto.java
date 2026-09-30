package com.findear.main.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * presigned PUT URL 발급 결과. 클라이언트는 headers를 그대로 PUT 요청에 붙여 uploadUrl로 보내고,
 * 게시글 등록·수정 때는 key를 imgKeys에 담는다. url은 업로드 뒤 조회용 공개 URL이다.
 */
@Getter
@AllArgsConstructor
public class PresignResDto {
    private String key;
    private String uploadUrl;
    private String url;
    private String expiresAt;
    private Map<String, String> headers;
}
