package com.st3.uber.repository;

import com.st3.uber.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
}
