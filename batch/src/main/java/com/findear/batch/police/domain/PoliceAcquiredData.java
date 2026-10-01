package com.findear.batch.police.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.time.LocalDate;

/**
 * Lost112 습득물 문서 (06 §3). 문서 ID는 atcId(자연키)라 같은 습득물을 다시 수집하면 덮어쓴다.
 * 응답 JSON의 {@code id} 키는 팀 버전과 같게 유지하고 값만 atcId 문자열이다 (main이 batch 응답을 그대로 넘긴다).
 * 매핑은 아래 어노테이션이 정하며 Spring Data ES가 인덱스를 만들 때만 적용된다 (이미 있는 인덱스는 바꾸지 않는다, PoliceIndexMappingChecker 참고).
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = PoliceAcquiredData.INDEX)
public class PoliceAcquiredData {

    public static final String INDEX = "police_acquired_data";

    @Id
    @Field(type = FieldType.Keyword)
    private String id;          // 문서 ID = atcId

    @Field(type = FieldType.Keyword)
    private String atcId;       // 관리ID

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String depPlace;    // 보관장소

    @Field(type = FieldType.Text)
    private String addr;        // 상세주소

    @Field(type = FieldType.Keyword, index = false)
    private String fdFilePathImg;       // 습득물 사진 이미지 URL

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String fdPrdtNm;        // 물품명

    @Field(type = FieldType.Text)
    private String fdSbjt;      // 게시 제목

    @Field(type = FieldType.Keyword)
    private String clrNm;       // 색상 명

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd")
    private LocalDate fdYmd;    // 습득 일자 (인덱스·JSON 모두 yyyy-MM-dd 문자열)

    @Field(type = FieldType.Keyword)
    private String prdtClNm;        // 물품 분류 (원문)

    @Field(type = FieldType.Keyword)
    private String mainPrdtClNm;    // 물품 대분류

    @Field(type = FieldType.Keyword)
    private String subPrdtClNm;     // 물품 소분류

    // 아래 둘은 인덱스에만 저장한다. 목록 API 응답 모양(07 §2)은 그대로 두려고 JSON에는 싣지 않는다.
    @JsonIgnore
    @Field(type = FieldType.Keyword)
    private String fdSn;        // 습득 순번

    @JsonIgnore
    @Field(type = FieldType.Keyword)
    private String source;      // POLICE(경찰청) / PORTAL(포털기관)
}
