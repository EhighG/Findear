package com.findear.main.storage.controller;

import com.findear.main.common.response.SuccessResponse;
import com.findear.main.storage.ImageStorageService;
import com.findear.main.storage.dto.PresignReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이미지 업로드용 presigned URL 발급. 로그인 회원만 호출할 수 있다 (SecurityConfig의 공개 경로에 넣지 않는다) */
@RequestMapping("/images")
@RequiredArgsConstructor
@RestController
public class ImageController {

    private final ImageStorageService imageStorageService;

    @PostMapping("/presign")
    public ResponseEntity<?> presign(@RequestBody PresignReqDto presignReqDto) {
        return ResponseEntity.ok(new SuccessResponse(HttpStatus.OK.value(), "업로드 URL이 발급되었습니다.",
                imageStorageService.presign(presignReqDto)));
    }
}
