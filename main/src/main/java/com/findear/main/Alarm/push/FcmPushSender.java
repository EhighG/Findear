package com.findear.main.Alarm.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * fcm.enabled=true일 때의 발송 구현. Firebase Admin SDK로 FCM HTTP v1에 등록 토큰 대상 웹푸시를 보낸다.
 *
 * 토큰 삭제 기준은 FCM 문서(manage-tokens, 오류 코드)의 UNREGISTERED(HTTP 404)뿐이다.
 * INVALID_ARGUMENT는 페이로드 문제로도 나올 수 있어 토큰을 지우지 않고 경고만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "fcm", name = "enabled", havingValue = "true")
public class FcmPushSender implements PushSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushResult send(PushMessage pushMessage) {
        try {
            String messageId = firebaseMessaging.send(toFcmMessage(pushMessage));
            log.info("FCM 발송 완료: memberId={}, messageId={}", pushMessage.memberId(), messageId);
            return PushResult.SENT;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED) {
                log.warn("FCM 토큰이 유효하지 않음(UNREGISTERED): memberId={}", pushMessage.memberId());
                return PushResult.TOKEN_INVALID;
            }
            log.warn("FCM 발송 실패: memberId={}, code={}, message={}", pushMessage.memberId(), code, e.getMessage());
            return PushResult.FAILED;
        } catch (RuntimeException e) {
            log.warn("FCM 발송 중 예상하지 못한 오류: memberId={}, {}", pushMessage.memberId(), e.toString());
            return PushResult.FAILED;
        }
    }

    /**
     * Admin SDK 9.11.0에서 Message.Builder#setToken은 deprecated이고 문서는 FID(setFid)를 권장한다.
     * 다만 tbl_notification에 저장된 값은 웹 SDK가 발급한 등록 토큰이라 FID로 바꿀 수 없다. 등록 토큰 발송은 계속 지원된다.
     */
    @SuppressWarnings("deprecation")
    static Message toFcmMessage(PushMessage pushMessage) {
        return Message.builder()
                .setToken(pushMessage.token())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(pushMessage.title())
                                .setBody(pushMessage.body())
                                .build())
                        .build())
                .build();
    }
}
