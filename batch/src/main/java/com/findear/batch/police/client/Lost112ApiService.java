package com.findear.batch.police.client;

/**
 * Lost112 습득물 조회 오픈API 2종 (공공데이터포털 15058696, 15057670). 경로는 base-url 아래.
 * 요청·응답 명세와 확인 기록: docs/restoration/05-external-integrations.md §3, §8
 */
public enum Lost112ApiService {

    /** 경찰청_습득물정보 조회 서비스 */
    POLICE("/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd"),

    /** 경찰청_포털기관 습득물정보 조회 서비스 */
    PORTAL("/LosPtfundInfoInqireService/getPtLosfundInfoAccToClAreaPd");

    private final String path;

    Lost112ApiService(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
