package com.findear.main.Alarm.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * fcm.enabled=false(기본, 속성이 없어도 같음)일 때의 발송 구현. 아무것도 보내지 않는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "fcm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopPushSender implements PushSender {

    @Override
    public PushResult send(PushMessage message) {
        log.info("FCM 비활성: 발송 건너뜀, memberId={}", message.memberId());
        return PushResult.SKIPPED;
    }
}
