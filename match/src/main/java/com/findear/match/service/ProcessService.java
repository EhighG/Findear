package com.findear.match.service;

import com.findear.match.config.MockProperties;
import com.findear.match.dto.ProcessRequest;
import com.findear.match.dto.ProcessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 습득물 자동채움 mock. OpenAI 대신 SHA-256("productName|imgUrl|seed")으로 카테고리·색상·키워드를 결정적으로 고른다.
 */
@Service
public class ProcessService {

    private static final Logger log = LoggerFactory.getLogger(ProcessService.class);

    static final List<String> CATEGORIES = List.of("카드", "지갑", "현금", "의류", "전자기기", "가방", "휴대폰", "증명서", "쇼핑백",
            "귀금속", "유가증권", "자동차", "서류", "도서용품", "스포츠용품", "컴퓨터", "산업용품", "악기", "기타");
    static final List<String> COLORS = List.of("검정", "흰", "빨강", "오렌지", "노랑", "초록", "파랑", "갈", "보라", "회", "기타");
    static final List<String> KEYWORD_POOL = List.of("소형", "대형", "가죽", "플라스틱", "금속", "천소재", "무지", "줄무늬", "로고",
            "낡음", "새것", "사각형", "원형", "지퍼", "끈");
    static final int DESCRIPTION_SIZE = 5;

    private final long seed;

    @Autowired
    public ProcessService(MockProperties properties) {
        this(properties.seed());
    }

    /** 테스트용: seed를 직접 지정한다. */
    ProcessService(long seed) {
        this.seed = seed;
    }

    public ProcessResponse process(ProcessRequest request) {
        if (request == null || isBlank(request.productName())) {
            throw new BadRequestException("productName은 비어 있지 않은 문자열이어야 합니다.");
        }
        if (isBlank(request.imgUrl())) {
            throw new BadRequestException("imgUrl은 비어 있지 않은 문자열이어야 합니다.");
        }
        String productName = request.productName();
        byte[] h = Hashing.sha256(productName + "|" + request.imgUrl() + "|" + seed);

        String category = CATEGORIES.get((int) (Hashing.unsignedInt(h, 0) % CATEGORIES.size()));
        String color = COLORS.get((int) (Hashing.unsignedInt(h, 4) % COLORS.size()));
        List<String> description = keywords(productName, h);

        log.info("findear 자동채움: productName={}, category={}, color={}", productName, category, color);
        return new ProcessResponse("success", new ProcessResponse.Result(category, color, description));
    }

    /** productName 토큰(중복 제거, 순서 유지)을 앞에서 최대 5개, 모자라면 고정 풀에서 해시 순서로 채운다. */
    private static List<String> keywords(String productName, byte[] h) {
        Set<String> used = new LinkedHashSet<>();
        for (String token : productName.strip().split("(?U)\\s+")) {
            if (!token.isEmpty() && used.size() < DESCRIPTION_SIZE) {
                used.add(token);
            }
        }
        int i = 0;
        while (used.size() < DESCRIPTION_SIZE) {
            int index = (h[8 + i] & 0xFF) % KEYWORD_POOL.size();
            while (used.contains(KEYWORD_POOL.get(index))) {
                index = (index + 1) % KEYWORD_POOL.size();
            }
            used.add(KEYWORD_POOL.get(index));
            i++;
        }
        return new ArrayList<>(used);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
