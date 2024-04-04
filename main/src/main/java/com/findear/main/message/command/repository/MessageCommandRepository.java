package com.findear.main.message.command.repository;

import com.findear.main.message.common.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageCommandRepository extends JpaRepository<Message, Long> {

}
