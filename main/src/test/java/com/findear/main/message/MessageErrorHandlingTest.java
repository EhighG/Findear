package com.findear.main.message;

import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.common.domain.Board;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.repository.MemberQueryRepository;
import com.findear.main.message.command.dto.ReplyMessageReqDto;
import com.findear.main.message.command.dto.SendMessageReqDto;
import com.findear.main.message.command.repository.MessageCommandRepository;
import com.findear.main.message.command.repository.MessageRoomCommandRepository;
import com.findear.main.message.command.service.MessageCommandService;
import com.findear.main.message.common.domain.MessageRoom;
import com.findear.main.message.common.exception.MessageException;
import com.findear.main.message.query.dto.ShowMessageListReqDto;
import com.findear.main.message.query.dto.ShowMessageRoomDetailReqDto;
import com.findear.main.message.query.repository.MessageRoomQueryRepository;
import com.findear.main.message.query.service.MessageQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * D-51: 의도한 도메인 오류(MessageException)만 400이고, 예상하지 못한 예외는 감싸지 않아 advice가 500으로 처리한다.
 * 예전에는 catch (Exception)이 모든 예외를 MessageException(400)으로 바꿨다.
 */
class MessageErrorHandlingTest {

    private static final long ENQUIRER = 1L;
    private static final long BOARD_WRITER = 2L;
    private static final long BOARD_ID = 50L;

    private MessageRoomQueryRepository roomQueryRepository;
    private BoardCommandRepository boardRepository;
    private NotificationService notificationService;
    private MessageQueryService queryService;
    private MessageCommandService commandService;
    private MessageCommandRepository messageRepository;
    private MessageRoomCommandRepository roomCommandRepository;
    private final RuntimeException dbDown = new DataAccessResourceFailureException("db down");

    @BeforeEach
    void setUp() {
        roomQueryRepository = mock(MessageRoomQueryRepository.class);
        boardRepository = mock(BoardCommandRepository.class);
        notificationService = mock(NotificationService.class);
        messageRepository = mock(MessageCommandRepository.class);
        roomCommandRepository = mock(MessageRoomCommandRepository.class);
        queryService = new MessageQueryService(roomQueryRepository);
        commandService = new MessageCommandService(messageRepository, boardRepository,
                roomCommandRepository, roomQueryRepository, mock(MemberQueryRepository.class),
                notificationService);

        Member writer = Member.builder().id(BOARD_WRITER).naverUid("u2").phoneNumber("01000000002").role(Role.NORMAL).build();
        Board board = Board.builder().id(BOARD_ID).isLost(true).member(writer).productName("지갑").build();
        when(boardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
    }

    private static SendMessageReqDto send(Long boardId) {
        SendMessageReqDto req = new SendMessageReqDto();
        req.setBoardId(boardId);
        req.setMemberId(ENQUIRER);
        req.setTitle("t");
        req.setContent("c");
        return req;
    }

    private static ReplyMessageReqDto reply(Long roomId) {
        return ReplyMessageReqDto.builder().messageRoomId(roomId).memberId(ENQUIRER).title("t").content("c").build();
    }

    @Test
    @DisplayName("sendMessage: 저장소의 예상 밖 예외는 MessageException(400)으로 바뀌지 않고 그대로 전파된다")
    void sendMessagePropagatesUnexpected() {
        when(roomQueryRepository.findByBoardIdAndMemberId(BOARD_ID, ENQUIRER)).thenThrow(dbDown);

        assertThatThrownBy(() -> commandService.sendMessage(send(BOARD_ID))).isSameAs(dbDown);
    }

    @Test
    @DisplayName("sendMessage: 알림 저장의 예상 밖 예외도 그대로 전파된다")
    void sendMessagePropagatesNotificationFailure() {
        Member enquirer = Member.builder().id(ENQUIRER).naverUid("u1").phoneNumber("01000000001").role(Role.NORMAL).build();
        Board board = boardRepository.findById(BOARD_ID).orElseThrow();
        when(roomQueryRepository.findByBoardIdAndMemberId(BOARD_ID, ENQUIRER))
                .thenReturn(MessageRoom.builder().member(enquirer).board(board).build());
        doThrow(dbDown).when(notificationService).sendNotification(any());

        assertThatThrownBy(() -> commandService.sendMessage(send(BOARD_ID))).isSameAs(dbDown);
    }

    @Test
    @DisplayName("sendMessage: 없는 게시판과 boardId 누락은 MessageException(400) \"해당 게시판이 존재하지 않습니다.\"")
    void sendMessageMissingBoard() {
        assertThatThrownBy(() -> commandService.sendMessage(send(404L)))
                .isInstanceOf(MessageException.class)
                .hasMessage("해당 게시판이 존재하지 않습니다.");
        assertThatThrownBy(() -> commandService.sendMessage(send(null)))
                .isInstanceOf(MessageException.class)
                .hasMessage("해당 게시판이 존재하지 않습니다.");
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("replyMessage: 저장소의 예상 밖 예외는 MessageException(400)으로 바뀌지 않고 그대로 전파된다")
    void replyMessagePropagatesUnexpected() {
        when(roomQueryRepository.findById(9L)).thenThrow(dbDown);

        assertThatThrownBy(() -> commandService.replyMessage(reply(9L))).isSameAs(dbDown);
    }

    @Test
    @DisplayName("replyMessage: 없는 쪽지방과 messageRoomId 누락은 MessageException(400) \"해당 쪽지방이 없습니다.\"")
    void replyMessageMissingRoom() {
        when(roomQueryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.replyMessage(reply(404L)))
                .isInstanceOf(MessageException.class)
                .hasMessage("해당 쪽지방이 없습니다.");
        assertThatThrownBy(() -> commandService.replyMessage(reply(null)))
                .isInstanceOf(MessageException.class)
                .hasMessage("해당 쪽지방이 없습니다.");
    }

    private MessageRoom existingRoom() {
        Member enquirer = Member.builder().id(ENQUIRER).naverUid("u1").phoneNumber("01000000001").role(Role.NORMAL).build();
        Board board = boardRepository.findById(BOARD_ID).orElseThrow();
        return MessageRoom.builder().member(enquirer).board(board).build();
    }

    private void assertNothingSaved() {
        verify(messageRepository, never()).save(any());
        verify(roomCommandRepository, never()).save(any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("sendMessage: 제목 256자 초과와 내용 65,535바이트 초과는 MessageException(400)이고 아무것도 저장하지 않는다")
    void sendMessageTooLong() {
        SendMessageReqDto longTitle = send(BOARD_ID);
        longTitle.setTitle("가".repeat(256));
        assertThatThrownBy(() -> commandService.sendMessage(longTitle))
                .isInstanceOf(MessageException.class)
                .hasMessage("제목은 255자 이하여야 합니다.");

        SendMessageReqDto longContent = send(BOARD_ID);
        longContent.setContent("가".repeat(21_846)); // 3바이트 x 21,846 = 65,538
        assertThatThrownBy(() -> commandService.sendMessage(longContent))
                .isInstanceOf(MessageException.class)
                .hasMessage("내용이 너무 깁니다.");

        assertNothingSaved();
    }

    @Test
    @DisplayName("sendMessage: 제목 정확히 255자, 내용 정확히 65,535바이트, null·빈 값은 통과한다")
    void sendMessageAtLimit() {
        MessageRoom room = existingRoom();
        when(roomQueryRepository.findByBoardIdAndMemberId(BOARD_ID, ENQUIRER)).thenReturn(room);

        SendMessageReqDto atLimit = send(BOARD_ID);
        atLimit.setTitle("가".repeat(255));
        atLimit.setContent("a".repeat(65_535));
        commandService.sendMessage(atLimit);

        SendMessageReqDto empty = send(BOARD_ID);
        empty.setTitle(null);
        empty.setContent("");
        commandService.sendMessage(empty);

        verify(messageRepository, times(2)).save(any());
        verify(notificationService, times(2)).sendNotification(any());
    }

    @Test
    @DisplayName("replyMessage: 제목 256자 초과와 내용 65,535바이트 초과는 MessageException(400)이고 아무것도 저장하지 않는다")
    void replyMessageTooLong() {
        MessageRoom room = existingRoom();
        when(roomQueryRepository.findById(9L)).thenReturn(Optional.of(room));

        ReplyMessageReqDto longTitle = ReplyMessageReqDto.builder().messageRoomId(9L).memberId(ENQUIRER)
                .title("a".repeat(256)).content("c").build();
        assertThatThrownBy(() -> commandService.replyMessage(longTitle))
                .isInstanceOf(MessageException.class)
                .hasMessage("제목은 255자 이하여야 합니다.");

        ReplyMessageReqDto longContent = ReplyMessageReqDto.builder().messageRoomId(9L).memberId(ENQUIRER)
                .title("t").content("a".repeat(65_536)).build();
        assertThatThrownBy(() -> commandService.replyMessage(longContent))
                .isInstanceOf(MessageException.class)
                .hasMessage("내용이 너무 깁니다.");

        assertNothingSaved();
    }

    @Test
    @DisplayName("replyMessage: 제목 정확히 255자, 내용 정확히 65,535바이트는 통과한다")
    void replyMessageAtLimit() {
        MessageRoom room = existingRoom();
        when(roomQueryRepository.findById(9L)).thenReturn(Optional.of(room));

        commandService.replyMessage(ReplyMessageReqDto.builder().messageRoomId(9L).memberId(ENQUIRER)
                .title("a".repeat(255)).content("a".repeat(65_535)).build());

        verify(messageRepository).save(any());
        verify(notificationService).sendNotification(any());
    }

    @Test
    @DisplayName("showMessageRoomList: 저장소의 예상 밖 예외는 그대로 전파되고, 쪽지방이 없으면 기존처럼 MessageException(400)")
    void showMessageRoomList() {
        ShowMessageListReqDto req = new ShowMessageListReqDto();
        req.setMemberId(ENQUIRER);

        when(roomQueryRepository.findAllByMemberIdWithBoardAndMessage(ENQUIRER)).thenThrow(dbDown);
        assertThatThrownBy(() -> queryService.showMessageRoomList(req)).isSameAs(dbDown);

        doReturn(List.of()).when(roomQueryRepository).findAllByMemberIdWithBoardAndMessage(ENQUIRER);
        assertThatThrownBy(() -> queryService.showMessageRoomList(req))
                .isInstanceOf(MessageException.class)
                .hasMessage("존재하는 메시지 방이 없습니다.");
    }

    @Test
    @DisplayName("showMessageRoomDetail: 저장소의 예상 밖 예외는 MessageException(400)으로 바뀌지 않고 그대로 전파된다")
    void showMessageRoomDetailPropagatesUnexpected() {
        ShowMessageRoomDetailReqDto req = new ShowMessageRoomDetailReqDto();
        req.setMemberId(ENQUIRER);
        req.setMessageRoomId(9L);
        when(roomQueryRepository.findByIdWithBoardAndMessage(9L)).thenThrow(dbDown);

        assertThatThrownBy(() -> queryService.showMessageRoomDetail(req)).isSameAs(dbDown);
    }
}
