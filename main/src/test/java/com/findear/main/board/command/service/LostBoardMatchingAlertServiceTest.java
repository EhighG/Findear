package com.findear.main.board.command.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.BatchServerResponseDto;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.member.common.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** batch 응답 모양별 알림 여부 (07 §2). DB 없이 저장소·알림 서비스는 mock. */
class LostBoardMatchingAlertServiceTest {

    private static final long LOST_BOARD_ID = 7L;
    private static final long WRITER_ID = 42L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LostBoardQueryRepository lostBoardQueryRepository;
    private NotificationService notificationService;
    private LostBoardMatchingAlertService service;

    @BeforeEach
    void setUp() {
        lostBoardQueryRepository = mock(LostBoardQueryRepository.class);
        notificationService = mock(NotificationService.class);
        service = new LostBoardMatchingAlertService(lostBoardQueryRepository, notificationService);
        Member writer = Member.builder().id(WRITER_ID).naverUid("u").phoneNumber("010-0000-0042").build();
        Board board = Board.builder().id(100L).isLost(true).member(writer).build();
        when(lostBoardQueryRepository.findById(LOST_BOARD_ID))
                .thenReturn(Optional.of(LostBoard.builder().id(LOST_BOARD_ID).board(board).build()));
    }

    private BatchServerResponseDto response(String json) throws Exception {
        return objectMapper.readValue(json, BatchServerResponseDto.class);
    }

    @DisplayName("findearDatas만 있어도 작성자에게 알림 1건 (지금 문구 그대로)")
    @Test
    void findearOnly() throws Exception {
        service.alertIfMatched(LOST_BOARD_ID, response(
                "{\"status\":200,\"result\":{\"findearDatas\":[{\"lostBoardId\":7,\"acquiredBoardId\":3,\"similarityRate\":0.9}],\"policeDatas\":[]}}"));

        ArgumentCaptor<NotificationRequestDto> request = ArgumentCaptor.forClass(NotificationRequestDto.class);
        verify(notificationService).sendNotification(request.capture());
        assertThat(request.getValue().getMemberId()).isEqualTo(WRITER_ID);
        assertThat(request.getValue().getTitle()).isEqualTo("등록하신 분실물과 유사한 물건들을 찾아봤어요!");
        assertThat(request.getValue().getMessage()).isEqualTo("매칭이 완료되었습니다.");
        assertThat(request.getValue().getType()).isEqualTo("message");
    }

    @DisplayName("policeDatas만 있어도 알림 1건")
    @Test
    void policeOnly() throws Exception {
        service.alertIfMatched(LOST_BOARD_ID, response(
                "{\"status\":200,\"result\":{\"findearDatas\":[],\"policeDatas\":[{\"lostBoardId\":7,\"acquiredBoardId\":\"F2099\",\"similarityRate\":0.8}]}}"));

        verify(notificationService).sendNotification(any());
    }

    @DisplayName("둘 다 있어도 알림은 1건")
    @Test
    void bothGiveSingleNotification() throws Exception {
        service.alertIfMatched(LOST_BOARD_ID, response(
                "{\"status\":200,\"result\":{\"findearDatas\":[{\"lostBoardId\":7}],\"policeDatas\":[{\"lostBoardId\":7}]}}"));

        verify(notificationService, org.mockito.Mockito.times(1)).sendNotification(any());
    }

    @DisplayName("둘 다 빈 목록이면 알림 없음 (분실물도 조회하지 않음)")
    @Test
    void bothEmpty() throws Exception {
        service.alertIfMatched(LOST_BOARD_ID, response(
                "{\"status\":200,\"result\":{\"findearDatas\":[],\"policeDatas\":[]}}"));

        verify(notificationService, never()).sendNotification(any());
        verify(lostBoardQueryRepository, never()).findById(any());
    }

    @DisplayName("result가 null이면 알림 없음")
    @Test
    void nullResult() throws Exception {
        service.alertIfMatched(LOST_BOARD_ID, response("{\"status\":200,\"message\":\"ok\",\"result\":null}"));

        verify(notificationService, never()).sendNotification(any());
    }

    @DisplayName("응답 형식이 다르면(result가 배열, 키 없음) 알림 없이 예외도 없다")
    @Test
    void unexpectedShape() throws Exception {
        service.alertIfMatched(LOST_BOARD_ID, response("{\"status\":200,\"result\":[1,2]}"));
        service.alertIfMatched(LOST_BOARD_ID, response("{\"status\":200,\"result\":{\"other\":1}}"));
        service.alertIfMatched(LOST_BOARD_ID, response("{\"status\":200,\"result\":{\"findearDatas\":\"x\"}}"));
        service.alertIfMatched(LOST_BOARD_ID, null);

        verify(notificationService, never()).sendNotification(any());
    }

    @DisplayName("응답의 lostBoardId가 달라도 알림 대상은 이벤트의 분실물 id로 조회한다")
    @Test
    void usesEventLostBoardId() throws Exception {
        service.alertIfMatched(LOST_BOARD_ID, response(
                "{\"status\":200,\"result\":{\"findearDatas\":[{\"lostBoardId\":999}],\"policeDatas\":[]}}"));

        verify(lostBoardQueryRepository).findById(LOST_BOARD_ID);
        verify(lostBoardQueryRepository, never()).findById(999L);
        verify(notificationService).sendNotification(any());
    }

    @DisplayName("분실물이 없거나 삭제됐으면 알림 없이 건너뛴다")
    @Test
    void missingLostBoard() throws Exception {
        when(lostBoardQueryRepository.findById(LOST_BOARD_ID)).thenReturn(Optional.empty());

        service.alertIfMatched(LOST_BOARD_ID, response(
                "{\"status\":200,\"result\":{\"findearDatas\":[{\"lostBoardId\":7}],\"policeDatas\":[]}}"));

        verify(notificationService, never()).sendNotification(any());
    }
}
