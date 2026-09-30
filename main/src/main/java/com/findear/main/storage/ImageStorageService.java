package com.findear.main.storage;

import com.findear.main.storage.dto.PresignReqDto;
import com.findear.main.storage.dto.PresignResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 이미지 업로드용 presigned PUT URL 발급과 object key 검증 (D-13).
 * 키 규칙: images/{yyyy}/{MM}/{uuid}.{ext} (Asia/Seoul 기준 날짜). 설계: docs/restoration/06-db-and-config.md §4
 */
@Slf4j
@Service
public class ImageStorageService {

    static final long MAX_CONTENT_LENGTH = 10L * 1024 * 1024; // 기존 multipart 제한(10MB)과 동일
    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^images/\\d{4}/(0[1-9]|1[0-2])/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|gif)$");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;
    private final Clock clock;

    @Autowired
    public ImageStorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties) {
        this(s3Client, s3Presigner, properties, Clock.system(ZONE));
    }

    ImageStorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties, Clock clock) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.clock = clock;
    }

    /** 요청 검증 후 key를 만들고 서명한다. 서명은 로컬 계산이라 스토리지를 호출하지 않는다 */
    public PresignResDto presign(PresignReqDto req) {
        String extension = extensionOf(req.getContentType());
        long contentLength = validateContentLength(req.getContentLength());
        String contentType = req.getContentType();
        String key = generateKey(extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(properties.getPresignExpireSeconds()))
                .putObjectRequest(putObjectRequest)
                .build());

        return new PresignResDto(
                key,
                presigned.url().toExternalForm(),
                ImageUrls.toUrl(key),
                OffsetDateTime.ofInstant(presigned.expiration(), ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                headersToSend(presigned));
    }

    /** 클라이언트가 PUT에 같이 보내야 하는 헤더. host는 클라이언트가 알아서 붙이므로 뺀다 */
    static Map<String, String> headersToSend(PresignedPutObjectRequest presigned) {
        Map<String, String> headers = new LinkedHashMap<>();
        presigned.signedHeaders().forEach((name, values) -> {
            if (!"host".equalsIgnoreCase(name) && !values.isEmpty()) {
                headers.put(displayName(name), values.get(0));
            }
        });
        return headers;
    }

    private static String displayName(String name) {
        return switch (name.toLowerCase()) {
            case "content-type" -> "Content-Type";
            case "content-length" -> "Content-Length";
            default -> name;
        };
    }

    static String extensionOf(String contentType) {
        String extension = contentType == null ? null : EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("허용하지 않는 Content-Type입니다. (image/jpeg, image/png, image/webp, image/gif)");
        }
        return extension;
    }

    static long validateContentLength(Long contentLength) {
        if (contentLength == null || contentLength < 1 || contentLength > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("contentLength는 1바이트 이상 10MB 이하여야 합니다.");
        }
        return contentLength;
    }

    String generateKey(String extension) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZONE);
        return "images/%04d/%02d/%s.%s".formatted(now.getYear(), now.getMonthValue(), UUID.randomUUID(), extension);
    }

    /** presign이 발급하는 형식의 key인지 (다른 경로·임의 문자열 차단) */
    public static boolean isValidKeyFormat(String key) {
        return key != null && KEY_PATTERN.matcher(key).matches();
    }

    /**
     * 게시글 등록·수정에 들어온 key 목록 검증: 형식, 중복, 스토리지에 실제 업로드됐는지(HeadObject, 서버용 S3Client).
     * 하나라도 어긋나면 IllegalArgumentException(공통 처리에서 400).
     */
    public void validateUploadedKeys(List<String> keys) {
        if (keys == null) {
            return;
        }
        if (new LinkedHashSet<>(keys).size() != keys.size()) {
            throw new IllegalArgumentException("중복된 이미지가 있습니다.");
        }
        for (String key : keys) {
            if (!isValidKeyFormat(key)) {
                throw new IllegalArgumentException("올바르지 않은 이미지 key입니다.");
            }
            if (!exists(key)) {
                throw new IllegalArgumentException("업로드되지 않은 이미지입니다.");
            }
        }
    }

    private boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(properties.getBucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.error("이미지 존재 확인 실패: status={}, key={}", e.statusCode(), key, e);
            throw new IllegalStateException("이미지 저장소를 확인하지 못했습니다.");
        }
    }
}
