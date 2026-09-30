package com.findear.main.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이미지 스토리지 설정 (application.yml의 storage.*, 설계: docs/restoration/06-db-and-config.md §4).
 * 엔드포인트는 둘로 나뉜다. 서명(SigV4)에 Host가 들어가므로 서버가 스토리지에 직접 요청할 때 쓰는 주소(endpoint)와
 * 브라우저가 접근할 주소(publicEndpoint)가 다르면(compose 내부망 vs 호스트) presigned URL은 publicEndpoint로 서명해야 한다.
 * 두 엔드포인트가 비면 SDK 기본(AWS) 엔드포인트를 쓴다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** 서버용 S3 클라이언트 엔드포인트 (예: http://seaweedfs:8333). 비면 AWS */
    private String endpoint = "";
    /** presigned URL 서명용 엔드포인트 (예: http://localhost:8333). 비면 AWS */
    private String publicEndpoint = "";
    /** 조회 URL prefix. object key 앞에 붙여 응답 URL을 만든다 */
    private String publicBaseUrl = "http://localhost:8333/findear-images";
    private String bucket = "findear-images";
    private boolean pathStyle = true;
    private long presignExpireSeconds = 600;
    private String region = "ap-northeast-2";
    /** local 프로필에서만 .env 값을 넣는다 (application-local.yml). 배포는 비워 두고 SDK 기본 체인(EC2 IAM Role)을 쓴다 */
    private String accessKey = "";
    private String secretKey = "";
}
