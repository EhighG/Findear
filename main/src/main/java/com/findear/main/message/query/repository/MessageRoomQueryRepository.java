package com.findear.main.message.query.repository;

import com.findear.main.message.common.domain.MessageRoom;
import com.findear.main.message.query.dto.ShowMessageListResDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRoomQueryRepository extends JpaRepository<MessageRoom, Long> {

    @Query("select mr from MessageRoom mr where mr.board.id = :boardId and (mr.board.member.id = :memberId or mr.member.id = :memberId)")
    MessageRoom findByBoardIdAndMemberId(Long boardId, Long memberId);

    @Query("select mr from MessageRoom mr join fetch mr.messageList " +
            "where mr.board.member.id = :memberId " +
            "   or mr.member.id = :memberId")
    List<MessageRoom> findAllByMemberIdWithBoardAndMessage(Long memberId);

    @Query("select mr from MessageRoom mr right join fetch mr.board left join fetch mr.messageList where mr.id = :messageRoomId order by mr.id desc")
    MessageRoom findByIdWithBoardAndMessage(Long messageRoomId);
}
