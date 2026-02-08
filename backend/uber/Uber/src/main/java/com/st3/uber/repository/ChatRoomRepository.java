package com.st3.uber.repository;

import com.st3.uber.domain.ChatRoom;
import com.st3.uber.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository
        extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByUserAndAdmin(User user, User admin);

    List<ChatRoom> findByAdminId(Long adminId);


}
