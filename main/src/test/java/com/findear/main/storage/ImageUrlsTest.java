package com.findear.main.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** object key -> 공개 URL 조립 (D-13). DB·네트워크 불필요. */
class ImageUrlsTest {

    @AfterEach
    void restoreDefault() {
        ImageUrls.configure(ImageUrls.DEFAULT_BASE_URL);
    }

    @DisplayName("public-base-url + / + key")
    @Test
    void joinsBaseUrlAndKey() {
        ImageUrls.configure("http://localhost:8333/findear-images");

        assertThat(ImageUrls.toUrl("images/2026/09/a.jpg"))
                .isEqualTo("http://localhost:8333/findear-images/images/2026/09/a.jpg");
    }

    @DisplayName("base-url 끝의 /와 key 앞의 /는 중복되지 않는다 (AWS/CloudFront 주소 형태)")
    @Test
    void normalizesSlashes() {
        ImageUrls.configure("https://findear-images.s3.ap-northeast-2.amazonaws.com/");

        assertThat(ImageUrls.toUrl("/images/2026/09/a.jpg"))
                .isEqualTo("https://findear-images.s3.ap-northeast-2.amazonaws.com/images/2026/09/a.jpg");
    }

    @DisplayName("key가 null이거나 비어 있으면 null (이미지 없는 게시글)")
    @Test
    void nullKeyReturnsNull() {
        assertThat(ImageUrls.toUrl(null)).isNull();
        assertThat(ImageUrls.toUrl("")).isNull();
        assertThat(ImageUrls.toUrl("  ")).isNull();
    }

    @DisplayName("base-url이 비어 있으면 기본값을 쓴다")
    @Test
    void blankBaseUrlFallsBackToDefault() {
        ImageUrls.configure("");

        assertThat(ImageUrls.toUrl("images/a.jpg")).isEqualTo(ImageUrls.DEFAULT_BASE_URL + "/images/a.jpg");
    }
}
