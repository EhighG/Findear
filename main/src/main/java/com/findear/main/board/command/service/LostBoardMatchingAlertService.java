package com.findear.main.board.command.service;

import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.BatchServerResponseDto;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * batch 매칭 응답(07 §2)을 보고 분실물 작성자에게 알림을 보낸다 (D-52).
 * findearDatas 또는 policeDatas에 1건 이상 있으면 알림 1건. 작성자는 이벤트에 담아 온 분실물 id로
 * 새 트랜잭션에서 조회한다 (응답의 lostBoardId는 쓰지 않고, 삭제된 분실물이면 건너뛴다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LostBoardMatchingAlertService {

    private final LostBoardQueryRepository lostBoardQueryRepository;
    private final NotificationService notificationService;

    @Transactional
    public void alertIfMatched(Long lostBoardId, BatchServerResponseDto response) {
        if (response == null || response.getResult() == null) {
            log.info("분실물 매칭 결과 없음(result 없음): lostBoardId={}", lostBoardId);
            return;
        }
        if (!(response.getResult() instanceof Map<?, ?> result)) {
            log.warn("분실물 매칭 응답 형식이 다름(result가 객체가 아님): lostBoardId={}", lostBoardId);
            return;
        }
        Object findearDatas = result.get("findearDatas");
        Object policeDatas = result.get("policeDatas");
        if (!(findearDatas instanceof Collection<?>) && !(policeDatas instanceof Collection<?>)) {
            log.warn("분실물 매칭 응답 형식이 다름(findearDatas·policeDatas 없음): lostBoardId={}", lostBoardId);
            return;
        }
        if (isEmpty(findearDatas) && isEmpty(policeDatas)) {
            log.info("분실물 매칭 결과 0건: lostBoardId={}", lostBoardId);
            return;
        }

        Optional<LostBoard> lostBoard = lostBoardQueryRepository.findById(lostBoardId);
        if (lostBoard.isEmpty()) {
            log.info("분실물 매칭 알림 건너뜀(없거나 삭제된 분실물): lostBoardId={}", lostBoardId);
            return;
        }
        notificationService.sendNotification(NotificationRequestDto.builder()
                .title("등록하신 분실물과 유사한 물건들을 찾아봤어요!")
                .message("매칭이 완료되었습니다.")
                .type("message")
                .memberId(lostBoard.get().getBoard().getMember().getId())
                .build());
    }

    private static boolean isEmpty(Object datas) {
        return !(datas instanceof Collection<?> c) || c.isEmpty();
    }
}
