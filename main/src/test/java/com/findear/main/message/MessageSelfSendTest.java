package com.findear.main.message;

import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.common.domain.Board;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.repository.MemberQueryRepository;
import com.findear.main.message.command.dto.SendMessageReqDto;
import com.findear.main.message.command.repository.MessageCommandRepository;
import com.findear.main.message.command.repository.MessageRoomCommandRepository;
import com.findear.main.message.command.service.MessageCommandService;
import com.findear.main.message.common.domain.Message;
import com.findear.main.message.common.domain.MessageRoom;
import com.findear.main.message.common.exception.MessageException;
import com.findear.main.message.query.repository.MessageRoomQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 쪽지방은 서로 다른 두 사람의 공간이다: 게시글 작성자가 자기 글에 쪽지를 보내면 400(MessageException)이고 쪽지방·쪽지·알림이 생기지 않는다. */
class MessageSelfSendTest {

    private static final long ENQUIRER = 1L;
    private static final long BOARD_WRITER = 2L;
    private static final long BOARD_ID = 50L;

    private MessageCommandRepository messageRepository;
    private MessageRoomCommandRepository roomCommandRepository;
    private MessageRoomQueryRepository roomQueryRepository;
    private NotificationService notificationService;
    private MessageCommandService service;
    private Member enquirer;
    private Board board;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageCommandRepository.class);
        roomCommandRepository = mock(MessageRoomCommandRepository.class);
        roomQueryRepository = mock(MessageRoomQueryRepository.class);
        notificationService = mock(NotificationService.class);
        BoardCommandRepository boardRepository = mock(BoardCommandRepository.class);
        MemberQueryRepository memberRepository = mock(MemberQueryRepository.class);
        service = new MessageCommandService(messageRepository, boardRepository, roomCommandRepository,
                roomQueryRepository, memberRepository, notificationService);

        enquirer = Member.builder().id(ENQUIRER).naverUid("u1").phoneNumber("01000000001").role(Role.NORMAL).build();
        Member writer = Member.builder().id(BOARD_WRITER).naverUid("u2").phoneNumber("01000000002").role(Role.NORMAL).build();
        board = Board.builder().id(BOARD_ID).isLost(true).member(writer).productName("지갑").build();
        when(boardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
        when(memberRepository.findById(ENQUIRER)).thenReturn(Optional.of(enquirer));
        when(memberRepository.findById(BOARD_WRITER)).thenReturn(Optional.of(writer));
    }

    private static SendMessageReqDto sendBy(long memberId) {
        SendMessageReqDto req = new SendMessageReqDto();
        req.setBoardId(BOARD_ID);
        req.setMemberId(memberId);
        req.setTitle("제목");
        req.setContent("내용");
        return req;
    }

    private void verifyNothingCreated() {
        verify(roomCommandRepository, never()).save(any());
        verify(messageRepository, never()).save(any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("작성자가 자기 글에 보내면 400이고 쪽지방이 없을 때 아무것도 만들지 않는다")
    void writerCannotSendToOwnBoardWithoutRoom() {
        when(roomQueryRepository.findByBoardIdAndMemberId(BOARD_ID, BOARD_WRITER)).thenReturn(null);

        assertThatThrownBy(() -> service.sendMessage(sendBy(BOARD_WRITER)))
                .isInstanceOf(MessageException.class)
                .hasMessage("자신의 게시글에는 쪽지를 보낼 수 없습니다.");

        verifyNothingCreated();
    }

    @Test
    @DisplayName("작성자가 자기 글에 보내면 400이고 과거에 만들어진 자기 쪽지방이 있어도 막는다")
    void writerCannotSendToOwnBoardWithExistingRoom() {
        MessageRoom ownRoom = MessageRoom.builder().member(board.getMember()).board(board).build();
        when(roomQueryRepository.findByBoardIdAndMemberId(BOARD_ID, BOARD_WRITER)).thenReturn(ownRoom);

        assertThatThrownBy(() -> service.sendMessage(sendBy(BOARD_WRITER)))
                .isInstanceOf(MessageException.class)
                .hasMessage("자신의 게시글에는 쪽지를 보낼 수 없습니다.");

        verifyNothingCreated();
    }

    @Test
    @DisplayName("다른 회원이 보내면 쪽지방(없으면 생성)·쪽지가 저장되고 작성자에게 알림이 간다")
    void otherMemberCanSend() {
        when(roomQueryRepository.findByBoardIdAndMemberId(BOARD_ID, ENQUIRER)).thenReturn(null);

        assertThatCode(() -> service.sendMessage(sendBy(ENQUIRER))).doesNotThrowAnyException();

        verify(roomCommandRepository).save(any(MessageRoom.class));
        verify(messageRepository).save(any(Message.class));
        verify(notificationService).sendNotification(org.mockito.ArgumentMatchers.argThat(
                request -> request.getMemberId().equals(BOARD_WRITER)));
    }

    @Test
    @DisplayName("다른 회원이 이미 있는 쪽지방에 보내면 쪽지만 저장되고 작성자에게 알림이 간다")
    void otherMemberCanSendToExistingRoom() {
        MessageRoom room = MessageRoom.builder().member(enquirer).board(board).build();
        when(roomQueryRepository.findByBoardIdAndMemberId(BOARD_ID, ENQUIRER)).thenReturn(room);

        assertThatCode(() -> service.sendMessage(sendBy(ENQUIRER))).doesNotThrowAnyException();

        verify(roomCommandRepository, never()).save(any());
        verify(messageRepository).save(any(Message.class));
        verify(notificationService).sendNotification(org.mockito.ArgumentMatchers.argThat(
                request -> request.getMemberId().equals(BOARD_WRITER)));
    }
}
