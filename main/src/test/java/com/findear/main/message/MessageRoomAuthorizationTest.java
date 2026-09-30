package com.findear.main.message;

import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.common.domain.Board;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.repository.MemberQueryRepository;
import com.findear.main.message.command.dto.ReplyMessageReqDto;
import com.findear.main.message.command.repository.MessageCommandRepository;
import com.findear.main.message.command.repository.MessageRoomCommandRepository;
import com.findear.main.message.command.service.MessageCommandService;
import com.findear.main.message.common.domain.MessageRoom;
import com.findear.main.message.common.exception.MessageException;
import com.findear.main.message.query.dto.ShowMessageRoomDetailReqDto;
import com.findear.main.message.query.repository.MessageRoomQueryRepository;
import com.findear.main.message.query.service.MessageQueryService;
import com.findear.main.storage.ImageUrls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AuthorizationServiceException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 쪽지방은 참여자(문의한 회원, 게시글 작성자)만 조회·답장할 수 있고, 아니면 403이다 (MessageException 400으로 뭉개지지 않는다). */
class MessageRoomAuthorizationTest {

    private static final long ENQUIRER = 1L;
    private static final long BOARD_WRITER = 2L;
    private static final long OUTSIDER = 3L;

    private MessageRoomQueryRepository roomQueryRepository;
    private MessageQueryService queryService;
    private MessageCommandService commandService;

    @BeforeEach
    void setUp() {
        ImageUrls.configure("http://localhost:8333/findear-images");
        roomQueryRepository = mock(MessageRoomQueryRepository.class);
        queryService = new MessageQueryService(roomQueryRepository);
        commandService = new MessageCommandService(mock(MessageCommandRepository.class), mock(BoardCommandRepository.class),
                mock(MessageRoomCommandRepository.class), roomQueryRepository, mock(MemberQueryRepository.class),
                mock(NotificationService.class));

        Member enquirer = Member.builder().id(ENQUIRER).naverUid("u1").phoneNumber("01000000001").role(Role.NORMAL).build();
        Member writer = Member.builder().id(BOARD_WRITER).naverUid("u2").phoneNumber("01000000002").role(Role.NORMAL).build();
        Board board = Board.builder().id(50L).isLost(true).member(writer).productName("지갑").build();
        MessageRoom room = MessageRoom.builder().member(enquirer).board(board).build();
        when(roomQueryRepository.findByIdWithBoardAndMessage(9L)).thenReturn(room);
        when(roomQueryRepository.findById(9L)).thenReturn(Optional.of(room));
    }

    private static ShowMessageRoomDetailReqDto detailBy(long memberId, long roomId) {
        ShowMessageRoomDetailReqDto req = new ShowMessageRoomDetailReqDto();
        req.setMemberId(memberId);
        req.setMessageRoomId(roomId);
        return req;
    }

    private static ReplyMessageReqDto replyBy(long memberId) {
        return ReplyMessageReqDto.builder().messageRoomId(9L).memberId(memberId).title("t").content("c").build();
    }

    @Test
    @DisplayName("상세 조회: 문의한 회원과 게시글 작성자는 볼 수 있다")
    void participantsCanView() {
        assertThat(queryService.showMessageRoomDetail(detailBy(ENQUIRER, 9L)).getBoard().getProductName()).isEqualTo("지갑");
        assertThat(queryService.showMessageRoomDetail(detailBy(BOARD_WRITER, 9L)).getBoard().getProductName()).isEqualTo("지갑");
    }

    @Test
    @DisplayName("상세 조회: 참여자가 아니면 403")
    void outsiderCannotView() {
        assertThatThrownBy(() -> queryService.showMessageRoomDetail(detailBy(OUTSIDER, 9L)))
                .isInstanceOf(AuthorizationServiceException.class);
    }

    @Test
    @DisplayName("상세 조회: 없는 쪽지방은 MessageException(400)")
    void missingRoom() {
        assertThatThrownBy(() -> queryService.showMessageRoomDetail(detailBy(ENQUIRER, 404L)))
                .isInstanceOf(MessageException.class);
    }

    @Test
    @DisplayName("답장: 참여자가 아니면 403, 참여자는 가능")
    void replyOnlyParticipants() {
        assertThatThrownBy(() -> commandService.replyMessage(replyBy(OUTSIDER)))
                .isInstanceOf(AuthorizationServiceException.class);
        assertThatCode(() -> commandService.replyMessage(replyBy(ENQUIRER))).doesNotThrowAnyException();
        assertThatCode(() -> commandService.replyMessage(replyBy(BOARD_WRITER))).doesNotThrowAnyException();
    }
}
