package com.findear.main.message.query.service;


import com.findear.main.message.common.domain.Message;
import com.findear.main.message.common.domain.MessageRoom;
import com.findear.main.message.common.exception.MessageException;
import com.findear.main.message.query.dto.*;
import com.findear.main.message.query.repository.MessageRoomQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MessageQueryService {

    private final MessageRoomQueryRepository messageRoomQueryRepository;

    public List<ShowMessageListResDto> showMessageRoomList(ShowMessageListReqDto showMessageListReqDto) {

        try {

            List<MessageRoom> messageRoomList = messageRoomQueryRepository.findAllByMemberIdWithBoardAndMessage(showMessageListReqDto.getMemberId());

            if(messageRoomList.isEmpty()) {

                throw new MessageException("존재하는 메시지 방이 없습니다.");
            }

            List<ShowMessageListResDto> result = messageRoomList.stream()
                    .map(mr -> ShowMessageListResDto.builder()
                            .messageRoomId(mr.getId())
                            .boardId(mr.getBoard().getId())
                            .thumbnailUrl(mr.getBoard().getThumbnailUrl())
                            .productName(mr.getBoard().getProductName())
                            .title(mr.getMessageList().get(mr.getMessageList().size()-1).getTitle())
                            .content(mr.getMessageList().get(mr.getMessageList().size()-1).getContent())
                            .sendAt(mr.getMessageList().get(mr.getMessageList().size()-1).getSendAt().toString()).build())
                            .toList();

            return result;

        } catch (Exception e) {

            throw new MessageException(e.getMessage());
        }
    }

    public ShowMessageRoomDetailResDto showMessageRoomDetail(ShowMessageRoomDetailReqDto showMessageListReqDto) {

        try {

            MessageRoom findMessageRoom = messageRoomQueryRepository.findByIdWithBoardAndMessage(showMessageListReqDto.getMessageRoomId());

            ShowMessageRoomDetailResDto result = ShowMessageRoomDetailResDto.builder()
                    .board(ShowMessageRoomDetailBoardDto.builder()
                            .boardId(findMessageRoom.getBoard().getId())
                            .thumbnailUrl(findMessageRoom.getBoard().getThumbnailUrl())
                            .productName(findMessageRoom.getBoard().getProductName()).build())
                    .enquirerTelNum(findMessageRoom.getMember().getPhoneNumber())
                    .build();

            for(Message m : findMessageRoom.getMessageList()) {

                if(result.getMessage() == null) {
                    result.setMessage(new ArrayList<>());
                }

                result.getMessage().add(MessageDto.builder()
                        .messageId(m.getId())
                        .messageRoomId(m.getMessageRoom().getId())
                        .title(m.getTitle())
                        .content(m.getContent())
                        .sendAt(m.getSendAt().toString())
                        .senderId(m.getSenderId()).build());
            }

            return result;

        } catch (Exception e) {

            throw new MessageException(e.getMessage());
        }
    }
}
