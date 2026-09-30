package com.findear.main.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 클라이언트 두 개를 만든다 (D-12, D-13). 로컬은 SeaweedFS, 배포는 AWS S3이고 설정만 다르다.
 * - S3Client: 서버가 HeadObject로 업로드 여부를 확인할 때 쓴다. 엔드포인트 = storage.endpoint (compose 내부 주소)
 * - S3Presigner: presigned PUT URL 서명용. 엔드포인트 = storage.public-endpoint (브라우저가 접근할 주소)
 * 자격증명은 SDK 기본 체인(환경변수 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY, 배포는 EC2 IAM Role)이다.
 * 단, 모드 B(IDE·bootRun)에서는 .env가 Spring 속성으로만 들어오고 환경변수가 아니라서 SDK가 못 읽으므로,
 * local 프로필이 storage.access-key/secret-key에 .env 값을 넣어 주면 그 값을 쓴다.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class StorageConfig {

    private final StorageProperties properties;

    @PostConstruct
    void configureImageUrls() {
        ImageUrls.configure(properties.getPublicBaseUrl());
    }

    @Bean
    public AwsCredentialsProvider storageCredentialsProvider() {
        return credentialsProvider(properties);
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client(AwsCredentialsProvider storageCredentialsProvider) {
        return createClient(properties, storageCredentialsProvider);
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(AwsCredentialsProvider storageCredentialsProvider) {
        return createPresigner(properties, storageCredentialsProvider);
    }

    static AwsCredentialsProvider credentialsProvider(StorageProperties p) {
        if (hasText(p.getAccessKey()) && hasText(p.getSecretKey())) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(p.getAccessKey().trim(), p.getSecretKey().trim()));
        }
        return DefaultCredentialsProvider.builder().build();
    }

    static S3Client createClient(StorageProperties p, AwsCredentialsProvider credentials) {
        var builder = S3Client.builder()
                .region(Region.of(p.getRegion()))
                .credentialsProvider(credentials)
                .forcePathStyle(p.isPathStyle())
                // SeaweedFS 등 S3 호환 서버가 SDK의 기본 체크섬 헤더를 다루지 못하는 경우를 피한다 (필요한 요청에만 체크섬)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);
        if (hasText(p.getEndpoint())) {
            builder.endpointOverride(URI.create(p.getEndpoint().trim()));
        }
        return builder.build();
    }

    static S3Presigner createPresigner(StorageProperties p, AwsCredentialsProvider credentials) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(p.getRegion()))
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(p.isPathStyle())
                        .build());
        if (hasText(p.getPublicEndpoint())) {
            builder.endpointOverride(URI.create(p.getPublicEndpoint().trim()));
        }
        return builder.build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
