package com.findear.main.Alarm.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * fcm.enabled=true일 때만 Firebase를 초기화한다. 서비스계정 JSON은 fcm.credentials-path(파일 시스템 경로)에서 읽는다.
 * 파일이 없거나 읽을 수 없으면 기동을 멈춘다 (켰는데 조용히 꺼지는 것보다 낫다).
 * 초기화 방식은 Admin SDK 설정 문서(FirebaseOptions.builder + GoogleCredentials.fromStream)를 따른다.
 */
@Slf4j
@Configuration
@ConditionalOnFcm(enabled = true)
public class FcmConfig {

    /** 응답이 없을 때 요청 스레드가 무한정 붙잡히지 않게 한다 (SDK 기본값은 제한 없음). */
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;

    @Bean
    public FirebaseApp firebaseApp(@Value("${fcm.credentials-path:}") String credentialsPath) {
        // 이미 초기화된 기본 앱이 있으면 재사용한다 (재시작·테스트 대비)
        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (FirebaseApp.DEFAULT_APP_NAME.equals(app.getName())) {
                log.info("이미 초기화된 FirebaseApp을 재사용합니다.");
                return app;
            }
        }

        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException(
                    "fcm.enabled=true인데 fcm.credentials-path(FCM_CREDENTIALS_PATH)가 비어 있습니다.");
        }
        Path path = Paths.get(credentialsPath);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("fcm.enabled=true인데 Firebase 서비스계정 파일을 읽을 수 없습니다: " + credentialsPath
                    + " (FCM_CREDENTIALS_PATH). 파일이 있는지, 컨테이너 실행 사용자(uid 10001)가 읽을 수 있는지 확인하거나 FCM_ENABLED=false로 끄세요.");
        }

        try (InputStream in = Files.newInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .setConnectTimeout(CONNECT_TIMEOUT_MILLIS)
                    .setReadTimeout(READ_TIMEOUT_MILLIS)
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("FirebaseApp 초기화 완료 (credentials-path={})", credentialsPath);
            return app;
        } catch (IOException e) {
            throw new IllegalStateException("Firebase 서비스계정 파일을 해석하지 못했습니다: " + credentialsPath
                    + " (" + e.getMessage() + ")", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
