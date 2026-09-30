package com.findear.main.storage;

/**
 * DB에 저장된 object key를 응답용 공개 URL로 바꾸는 한 곳 (D-13). URL = STORAGE_PUBLIC_BASE_URL + "/" + key.
 * DTO 변환 코드가 정적 메소드로 쓰도록 static이고, 앱 기동 때 {@link StorageConfig}가 설정값을 넣는다.
 */
public final class ImageUrls {

    public static final String DEFAULT_BASE_URL = "http://localhost:8333/findear-images";

    private static volatile String baseUrl = DEFAULT_BASE_URL;

    private ImageUrls() {
    }

    /** 앱 기동 때 StorageConfig가 호출한다. 테스트에서 값을 바꿀 때도 쓴다 */
    public static void configure(String publicBaseUrl) {
        baseUrl = trimTrailingSlash(publicBaseUrl == null || publicBaseUrl.isBlank() ? DEFAULT_BASE_URL : publicBaseUrl.trim());
    }

    /** key가 null이거나 비어 있으면 null (이미지 없는 게시글) */
    public static String toUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return baseUrl + "/" + (key.startsWith("/") ? key.substring(1) : key);
    }

    private static String trimTrailingSlash(String url) {
        String result = url;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
