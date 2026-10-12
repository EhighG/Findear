package com.findear.main.message.command.service;

import com.findear.main.Alarm.dto.AlarmDataDto;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.service.EmitterService;
import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.query.repository.MemberQueryRepository;
import com.findear.main.message.command.dto.ReplyMessageReqDto;
import com.findear.main.message.command.dto.SendMessageReqDto;
import com.findear.main.message.command.repository.MessageCommandRepository;
import com.findear.main.message.command.repository.MessageRoomCommandRepository;
import com.findear.main.message.common.domain.Message;
import com.findear.main.message.common.domain.MessageRoom;
import com.findear.main.message.common.exception.MessageException;
import com.findear.main.message.query.repository.MessageRoomQueryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MessageCommandService {

    static final int MAX_TITLE_LENGTH = 255;
    static final int MAX_CONTENT_BYTES = 65_535;

    private final MessageCommandRepository messageCommandRepository;
    private final BoardCommandRepository boardQueryRepository;
    private final MessageRoomCommandRepository messageRoomCommandRepository;
    private final MessageRoomQueryRepository messageRoomQueryRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final NotificationService notificationService;

    public void sendMessage(SendMessageReqDto sendMessageReqDto) {

        // boardId를 빼먹은 요청은 입력 오류(400)다 (findById(null)은 저장소 예외라 감싸지 않으면 500이 된다)
        if (sendMessageReqDto.getBoardId() == null) {
            throw new MessageException("해당 게시판이 존재하지 않습니다.");
        }

        Board findBoard = boardQueryRepository.findById(sendMessageReqDto.getBoardId())
                .orElseThrow(() -> new MessageException("해당 게시판이 존재하지 않습니다."));

        // 쪽지방은 서로 다른 두 사람의 공간이다: 자기 게시글에는 쪽지를 보낼 수 없다 (쪽지방이 이미 있어도 막는다)
        if (findBoard.getMember().getId().equals(sendMessageReqDto.getMemberId())) {
            throw new MessageException("자신의 게시글에는 쪽지를 보낼 수 없습니다.");
        }

        // 쪽지방·쪽지·알림을 만들기 전에 컬럼 길이를 검사한다 (넘으면 DB 예외라 500이 된다)
        validateLength(sendMessageReqDto.getTitle(), sendMessageReqDto.getContent());

        MessageRoom findMessageRoom = messageRoomQueryRepository.findByBoardIdAndMemberId(sendMessageReqDto.getBoardId(), sendMessageReqDto.getMemberId());

        // 쪽지방이 생성되지 않은 상태면 쪽지방 생성
        if(findMessageRoom == null) {

            Member findMember = memberQueryRepository.findById(sendMessageReqDto.getMemberId())
                    .orElseThrow(() -> new MessageException("해당 유저가 없습니다."));

            findMessageRoom = MessageRoom.builder()
                    .member(findMember).board(findBoard).build();

            messageRoomCommandRepository.save(findMessageRoom);
        }

        // 메세지 생성
        Message newMessage = Message.builder()
                .messageRoom(findMessageRoom)
                .title(sendMessageReqDto.getTitle())
                .senderId(sendMessageReqDto.getMemberId())
                .content(sendMessageReqDto.getContent())
                .sendAt(LocalDateTime.now())
                .build();

        messageCommandRepository.save(newMessage);

        notificationService.sendNotification(NotificationRequestDto.builder()
                .title("쪽지 도착")
                .message("쪽지가 도착했습니다.")
                .type("message")
                .memberId(findMessageRoom.getBoard().getMember().getId())
                .build());
    }

    public void replyMessage(ReplyMessageReqDto replyMessageReqDto) {

        // messageRoomId를 빼먹은 요청은 입력 오류(400)다 (findById(null)은 저장소 예외라 감싸지 않으면 500이 된다)
        if (replyMessageReqDto.getMessageRoomId() == null) {
            throw new MessageException("해당 쪽지방이 없습니다.");
        }

        MessageRoom findMessageRoom = messageRoomQueryRepository.findById(replyMessageReqDto.getMessageRoomId())
                .orElseThrow(() -> new MessageException("해당 쪽지방이 없습니다."));

        // 쪽지방 참여자(문의한 회원, 게시글 작성자)만 답장할 수 있다
        Long requesterId = replyMessageReqDto.getMemberId();
        if (!findMessageRoom.getMember().getId().equals(requesterId)
                && !findMessageRoom.getBoard().getMember().getId().equals(requesterId)) {
            throw new AuthorizationServiceException("권한이 없습니다.");
        }

        validateLength(replyMessageReqDto.getTitle(), replyMessageReqDto.getContent());

        Message newMessage = Message.builder()
                .messageRoom(findMessageRoom)
                .title(replyMessageReqDto.getTitle())
                .senderId(replyMessageReqDto.getMemberId())
                .content(replyMessageReqDto.getContent())
                .sendAt(LocalDateTime.now())
                .build();

        messageCommandRepository.save(newMessage);

        Long receiverId = null;
        Long hostId = findMessageRoom.getBoard().getMember().getId();
        if(!Objects.equals(hostId, newMessage.getSenderId())) {
            receiverId = hostId;
        } else {
            receiverId = findMessageRoom.getMember().getId();
        }

        notificationService.sendNotification(NotificationRequestDto.builder()
                .title("쪽지 도착")
                .message("쪽지가 도착했습니다.")
                .type("message")
                .memberId(receiverId)
                .build());
    }

    /** tbl_message.title은 varchar(255)(문자 수), content는 TEXT(UTF-8 65,535바이트)다. null·빈 값은 허용한다. */
    private void validateLength(String title, String content) {
        if (title != null && title.codePointCount(0, title.length()) > MAX_TITLE_LENGTH) {
            throw new MessageException("제목은 " + MAX_TITLE_LENGTH + "자 이하여야 합니다.");
        }
        if (content != null && content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new MessageException("내용이 너무 깁니다.");
        }
    }
}
