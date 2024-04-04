package com.findear.main.message.command.repository;

import com.findear.main.message.common.domain.MessageRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRoomCommandRepository extends JpaRepository<MessageRoom, Long> {
}
