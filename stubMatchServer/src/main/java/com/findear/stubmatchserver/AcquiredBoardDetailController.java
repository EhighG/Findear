package com.findear.stubmatchserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class AcquiredBoardDetailController {

    @PostMapping("/process")
    public ResponseEntity<?> getDetailsOfLostBoard(@RequestBody AutoFillReqDto autoFillReqDto) {
        AutoFillResDto details = null;

        return ResponseEntity
                .ok(details);
    }


}
