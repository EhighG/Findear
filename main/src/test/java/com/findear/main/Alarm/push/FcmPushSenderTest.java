package com.findear.main.Alarm.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FCM 발송 구현 검증. Firebase·Google 서버는 호출하지 않는다 (D-38): FirebaseMessaging을 mock으로 두고
 * send에 넘어간 Message를 FCM HTTP v1 요청 본문(message)으로 직렬화해 공식 문서 형식과 비교한다.
 */
class FcmPushSenderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
    private final FcmPushSender sender = new FcmPushSender(firebaseMessaging);

    private final PushMessage pushMessage = new PushMessage(1L, "test-token", "쪽지 도착", "쪽지가 도착했습니다.:message");

    @DisplayName("Message를 FCM v1 요청 본문(message)으로 직렬화하면 token과 webpush.notification.title/body만 담긴다")
    @Test
    void serializesToFcmV1RequestBody() throws Exception {
        when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/p/messages/m1");

        PushResult result = sender.send(pushMessage);

        assertThat(result).isEqualTo(PushResult.SENT);
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(captor.capture());

        // Admin SDK의 Message.wrapForTransport와 같은 방식: {"message": <Message>}를 google-http-client JsonFactory로 직렬화
        String requestBody = GsonFactory.getDefaultInstance().toString(Map.of("message", captor.getValue()));
        System.out.println("FCM v1 request body: " + requestBody);

        String expected = """
                {"message":{
                  "token":"test-token",
                  "webpush":{"notification":{"title":"쪽지 도착","body":"쪽지가 도착했습니다.:message"}}
                }}""";
        assertThat(MAPPER.readTree(requestBody)).isEqualTo(MAPPER.readTree(expected));
    }

    @DisplayName("UNREGISTERED면 TOKEN_INVALID를 돌려주고 예외는 밖으로 나가지 않는다")
    @Test
    void unregisteredIsTokenInvalid() throws Exception {
        FirebaseMessagingException unregistered = messagingException(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(unregistered);

        assertThat(sender.send(pushMessage)).isEqualTo(PushResult.TOKEN_INVALID);
    }

    @DisplayName("INVALID_ARGUMENT·UNAVAILABLE 등 다른 오류 코드는 FAILED (토큰 삭제 대상 아님)")
    @Test
    void otherErrorCodesAreFailed() throws Exception {
        for (MessagingErrorCode code : new MessagingErrorCode[]{MessagingErrorCode.INVALID_ARGUMENT,
                MessagingErrorCode.UNAVAILABLE, MessagingErrorCode.INTERNAL, MessagingErrorCode.QUOTA_EXCEEDED,
                MessagingErrorCode.SENDER_ID_MISMATCH, MessagingErrorCode.THIRD_PARTY_AUTH_ERROR, null}) {
            FirebaseMessagingException exception = messagingException(code);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

            assertThat(sender.send(pushMessage)).as("code=%s", code).isEqualTo(PushResult.FAILED);
        }
    }

    @DisplayName("SDK가 런타임 예외를 던져도 FAILED로 흡수한다")
    @Test
    void runtimeExceptionIsFailed() throws Exception {
        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(new IllegalStateException("FirebaseApp with name [DEFAULT] doesn't exist."));

        assertThat(sender.send(pushMessage)).isEqualTo(PushResult.FAILED);
    }

    /** FirebaseMessagingException은 final이고 생성자가 패키지 전용이라 (Mockito inline) mock으로 만든다. */
    private static FirebaseMessagingException messagingException(MessagingErrorCode code) throws IOException {
        FirebaseMessagingException e = mock(FirebaseMessagingException.class);
        when(e.getMessagingErrorCode()).thenReturn(code);
        when(e.getMessage()).thenReturn("mock error " + code);
        return e;
    }
}
