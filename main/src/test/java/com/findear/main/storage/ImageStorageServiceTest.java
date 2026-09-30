package com.findear.main.storage;

import com.findear.main.storage.dto.PresignReqDto;
import com.findear.main.storage.dto.PresignResDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * presigned PUT 발급과 key 검증 (D-13, D-42의 오프라인 부분).
 * presigned URL 서명은 로컬 계산이라 네트워크를 쓰지 않는다. 그래서 Presigner의 엔드포인트를
 * 연결할 수 없는 주소(예약 도메인 .invalid)로 두고 URL 문자열만 확인한다. S3Client는 mock이다 (AWS·SeaweedFS 호출 없음).
 */
class ImageStorageServiceTest {

    private static final AwsCredentialsProvider FAKE_CREDENTIALS =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    private static final Pattern KEY_FORMAT = Pattern.compile(
            "^images/2026/10/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|gif)$");
    // 2026-09-30T15:30:00Z = 2026-10-01 00:30 Asia/Seoul (UTC 기준으로는 아직 9월이라 날짜 변환을 확인할 수 있다)
    private static final Instant NOW = Instant.parse("2026-09-30T15:30:00Z");

    private S3Client s3Client;
    private StorageProperties localProps;
    private S3Presigner localPresigner;
    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        ImageUrls.configure("http://localhost:8333/findear-images");
        s3Client = mock(S3Client.class);
        localProps = new StorageProperties();
        localProps.setPublicEndpoint("http://localhost:8333");
        localProps.setPathStyle(true);
        localPresigner = StorageConfig.createPresigner(localProps, FAKE_CREDENTIALS);
        service = new ImageStorageService(s3Client, localPresigner, localProps,
                Clock.fixed(NOW, ZoneId.of("UTC")));
    }

    @AfterEach
    void tearDown() {
        localPresigner.close();
        ImageUrls.configure(ImageUrls.DEFAULT_BASE_URL);
    }

    // ---- key 생성 ----

    @DisplayName("key는 images/{yyyy}/{MM}/{uuid}.{ext}, 날짜는 Asia/Seoul 기준")
    @Test
    void generatesKeyInSeoulDate() {
        String key = service.generateKey("jpg");

        assertThat(key).startsWith("images/2026/10/"); // UTC로는 9월 30일이지만 서울은 10월 1일
        assertThat(ImageStorageService.isValidKeyFormat(key)).isTrue();
        assertThat(key).endsWith(".jpg");
        assertThat(service.generateKey("jpg")).isNotEqualTo(key);
    }

    @DisplayName("Content-Type별 확장자")
    @Test
    void extensionByContentType() {
        assertThat(ImageStorageService.extensionOf("image/jpeg")).isEqualTo("jpg");
        assertThat(ImageStorageService.extensionOf("image/png")).isEqualTo("png");
        assertThat(ImageStorageService.extensionOf("image/webp")).isEqualTo("webp");
        assertThat(ImageStorageService.extensionOf("image/gif")).isEqualTo("gif");
    }

    // ---- 요청 검증 ----

    @DisplayName("허용하지 않는 Content-Type은 거부")
    @Test
    void rejectsUnsupportedContentType() {
        for (String type : new String[]{"text/plain", "image/svg+xml", "IMAGE/JPEG", "", null}) {
            assertThatThrownBy(() -> service.presign(request(type, 1000L)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @DisplayName("contentLength는 1 이상 10MB 이하")
    @Test
    void validatesContentLength() {
        assertThatThrownBy(() -> service.presign(request("image/jpeg", 0L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.presign(request("image/jpeg", -5L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.presign(request("image/jpeg", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.presign(request("image/jpeg", 10L * 1024 * 1024 + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.presign(request("image/jpeg", 20_000_000L)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(service.presign(request("image/jpeg", 1L)).getKey()).isNotBlank();
        assertThat(service.presign(request("image/jpeg", 10L * 1024 * 1024)).getKey()).isNotBlank();
    }

    // ---- presign 결과 (오프라인) ----

    @DisplayName("로컬 설정: localhost:8333 path-style, 만료 600초, content-length·content-type·host 서명")
    @Test
    void presignForLocalStorage() {
        PresignResDto res = service.presign(request("image/jpeg", 12345L));
        URI uri = URI.create(res.getUploadUrl());
        Map<String, String> query = queryOf(uri);

        assertThat(KEY_FORMAT.matcher(res.getKey()).matches()).isTrue();
        assertThat(uri.getScheme() + "://" + uri.getAuthority()).isEqualTo("http://localhost:8333");
        assertThat(uri.getPath()).isEqualTo("/findear-images/" + res.getKey());
        assertThat(query).containsEntry("X-Amz-Expires", "600");
        assertThat(query).containsEntry("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
        assertThat(query.get("X-Amz-Credential")).startsWith("test/").endsWith("/ap-northeast-2/s3/aws4_request");
        assertThat(query).containsKey("X-Amz-Signature");
        assertThat(Arrays.asList(query.get("X-Amz-SignedHeaders").split(";")))
                .contains("content-length", "content-type", "host");
        assertThat(res.getUrl()).isEqualTo("http://localhost:8333/findear-images/" + res.getKey());
        assertThat(res.getExpiresAt()).matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d.*\\+09:00"); // 만료 시각은 SDK가 실제 시계로 계산
        assertThat(res.getHeaders())
                .containsEntry("Content-Type", "image/jpeg")
                .containsEntry("Content-Length", "12345")
                .doesNotContainKeys("host", "Host");
        System.out.println("[presign local] " + res.getUploadUrl() + " headers=" + res.getHeaders());
    }

    @DisplayName("만료 시간은 설정값을 따른다")
    @Test
    void presignExpireFollowsProperty() {
        localProps.setPresignExpireSeconds(120);

        Map<String, String> query = queryOf(URI.create(service.presign(request("image/png", 10L)).getUploadUrl()));

        assertThat(query).containsEntry("X-Amz-Expires", "120");
    }

    @DisplayName("AWS 설정(엔드포인트 없음, path-style false): 호스트가 {bucket}.s3.ap-northeast-2.amazonaws.com")
    @Test
    void presignForAws() {
        StorageProperties aws = new StorageProperties();
        aws.setBucket("findear-images");
        aws.setPublicEndpoint("");
        aws.setPathStyle(false);
        aws.setRegion("ap-northeast-2");
        aws.setPublicBaseUrl("https://findear-images.s3.ap-northeast-2.amazonaws.com");
        ImageUrls.configure(aws.getPublicBaseUrl());
        try (S3Presigner awsPresigner = StorageConfig.createPresigner(aws, FAKE_CREDENTIALS)) {
            ImageStorageService awsService = new ImageStorageService(s3Client, awsPresigner, aws,
                    Clock.fixed(NOW, ZoneId.of("UTC")));

            PresignResDto res = awsService.presign(request("image/webp", 2048L));
            URI uri = URI.create(res.getUploadUrl());

            assertThat(uri.getScheme()).isEqualTo("https");
            assertThat(uri.getHost()).isEqualTo("findear-images.s3.ap-northeast-2.amazonaws.com");
            assertThat(uri.getPath()).isEqualTo("/" + res.getKey());
            assertThat(res.getKey()).endsWith(".webp");
            assertThat(res.getUrl()).isEqualTo("https://findear-images.s3.ap-northeast-2.amazonaws.com/" + res.getKey());
            System.out.println("[presign aws] " + res.getUploadUrl());
        }
    }

    @DisplayName("presign은 서버용 S3Client를 호출하지 않는다 (로컬 계산)")
    @Test
    void presignDoesNotCallS3Client() {
        service.presign(request("image/gif", 100L));

        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    // ---- key 검증 (HeadObject는 mock) ----

    @DisplayName("presign이 만든 형식이 아닌 key는 거부 (다른 경로, 경로 탈출, 확장자)")
    @Test
    void rejectsMalformedKeys() {
        for (String key : List.of(
                "avatars/2026/09/3f2b8c1e-1111-4222-8333-444455556666.jpg",
                "images/../secret.jpg",
                "images/2026/13/3f2b8c1e-1111-4222-8333-444455556666.jpg",
                "images/2026/09/not-a-uuid.jpg",
                "images/2026/09/3f2b8c1e-1111-4222-8333-444455556666.exe",
                "https://evil.example.com/a.jpg",
                "")) {
            assertThatThrownBy(() -> service.validateUploadedKeys(List.of(key)))
                    .as(key).isInstanceOf(IllegalArgumentException.class);
        }
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @DisplayName("스토리지에 없는 key는 거부: 업로드되지 않은 이미지")
    @Test
    void rejectsMissingObject() {
        String key = service.generateKey("jpg");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertThatThrownBy(() -> service.validateUploadedKeys(List.of(key)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("업로드되지 않은");
    }

    @DisplayName("HeadObject가 404 S3Exception이어도 없는 것으로 본다")
    @Test
    void treats404AsMissing() {
        String key = service.generateKey("png");
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        assertThatThrownBy(() -> service.validateUploadedKeys(List.of(key)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("업로드되지 않은");
    }

    @DisplayName("저장소 오류(403·5xx)는 없는 이미지로 위장하지 않고 다른 예외로 알린다")
    @Test
    void storageErrorIsNotReportedAsMissing() {
        String key = service.generateKey("png");
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).build());

        assertThatThrownBy(() -> service.validateUploadedKeys(List.of(key)))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("존재하는 key는 통과하고, 같은 key를 두 번 보내면 거부")
    @Test
    void acceptsExistingAndRejectsDuplicates() {
        String key = service.generateKey("jpg");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        service.validateUploadedKeys(List.of(key));
        service.validateUploadedKeys(null);
        service.validateUploadedKeys(List.of());

        assertThatThrownBy(() -> service.validateUploadedKeys(List.of(key, key)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("S3Client 생성은 네트워크 없이 가능하고 엔드포인트가 비면 AWS 기본을 쓴다")
    @Test
    void clientBuildsWithoutNetwork() {
        StorageProperties p = new StorageProperties();
        p.setEndpoint("http://seaweedfs.invalid:8333");
        try (S3Client client = StorageConfig.createClient(p, FAKE_CREDENTIALS)) {
            assertThat(client).isNotNull();
        }
        p.setEndpoint("");
        p.setPathStyle(false);
        try (S3Client client = StorageConfig.createClient(p, FAKE_CREDENTIALS)) {
            assertThat(client).isNotNull();
        }
    }

    // ---- helpers ----

    private static PresignReqDto request(String contentType, Long contentLength) {
        PresignReqDto req = new PresignReqDto();
        req.setContentType(contentType);
        req.setContentLength(contentLength);
        return req;
    }

    private static Map<String, String> queryOf(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(kv -> kv.split("=", 2))
                .collect(Collectors.toMap(kv -> kv[0], kv -> URLDecoder.decode(kv.length > 1 ? kv[1] : "", StandardCharsets.UTF_8)));
    }
}
