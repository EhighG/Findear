package com.findear.batch.support;

import com.findear.batch.police.domain.PoliceAcquiredData;

import java.time.LocalDate;

/** 테스트용 Lost112 문서. id(문서 ID)는 atcId와 같다 */
public final class PoliceDocs {

    private PoliceDocs() {
    }

    /** 모든 필드가 채워진 문서의 빌더. 필요한 필드만 바꿔 쓴다 */
    public static PoliceAcquiredData.PoliceAcquiredDataBuilder builder(String atcId, String category, LocalDate fdYmd) {
        return PoliceAcquiredData.builder()
                .id(atcId)
                .atcId(atcId)
                .depPlace("종로경찰서")
                .fdFilePathImg("https://img.test/" + atcId)
                .fdPrdtNm("물품 " + atcId)
                .fdSbjt("제목 " + atcId)
                .clrNm("검정")
                .fdYmd(fdYmd)
                .prdtClNm(category + " > 소분류")
                .mainPrdtClNm(category)
                .subPrdtClNm("소분류")
                .fdSn("1")
                .source("POLICE");
    }

    public static PoliceAcquiredData doc(String atcId, String category, LocalDate fdYmd) {
        return builder(atcId, category, fdYmd).build();
    }
}
