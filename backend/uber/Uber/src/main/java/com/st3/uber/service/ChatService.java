package com.st3.uber.service;

import com.st3.uber.domain.ChatRoom;
import com.st3.uber.domain.Message;
import com.st3.uber.domain.User;
import com.st3.uber.dto.chat.AdminDto;
import com.st3.uber.dto.chat.ChatMessage;
import com.st3.uber.enums.SenderType;
import com.st3.uber.enums.UserRole;
import com.st3.uber.repository.ChatRoomRepository;
import com.st3.uber.repository.MessageRepository;
import com.st3.uber.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    public ChatService(MessageRepository messageRepository,
                       ChatRoomRepository chatRoomRepository,
                       UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
    }

    public AdminDto getAdmin(){
        User admin = userRepository.findFirstByRole(UserRole.ADMIN)
                .orElseThrow(() -> new RuntimeException("No admin found in system"));

        return new AdminDto(
                admin.getId(),
                admin.getName() + " " + admin.getSurname(),
                "ADMIN"
        );
    }

    public void saveMessage(Long fromId,
                            Long toId,
                            String content) {

        User from = userRepository.findById(fromId).orElseThrow();
        User to = userRepository.findById(toId).orElseThrow();

        User admin;
        User user;

        if(from.getRole() == UserRole.ADMIN){
            admin = from;
            user = to;
        } else if(to.getRole() == UserRole.ADMIN) {
            admin = to;
            user = from;
        } else {
            admin = userRepository.findFirstByRole(UserRole.ADMIN)
                    .orElseThrow(() -> new RuntimeException("No admin found in system"));
            user = from;
        }

        ChatRoom room = chatRoomRepository
                .findByUserAndAdmin(user, admin)
                .orElseGet(() -> {
                    ChatRoom r = new ChatRoom();
                    r.setAdmin(admin);
                    r.setUser(user);
                    return chatRoomRepository.save(r);
                });

        Message msg = new Message();
        msg.setChatRoom(room);
        msg.setContent(content);
        msg.setSenderId(fromId);
        msg.setSenderType(
                from.getRole() == UserRole.ADMIN
                        ? SenderType.ADMIN
                        : SenderType.USER
        );

        messageRepository.save(msg);
    }


    public List<Message> getChatHistory(Long userId1, Long userId2) {

        User u1 = userRepository.findById(userId1).orElseThrow();
        User u2 = userRepository.findById(userId2).orElseThrow();

        ChatRoom room = chatRoomRepository
                .findByUserAndAdmin(u1, u2)
                .orElseGet(() -> chatRoomRepository
                        .findByUserAndAdmin(u2, u1)
                        .orElse(null));

        if(room == null) return List.of();

        return messageRepository
                .findByChatRoomIdOrderBySentAtAsc(room.getId());
    }

    public List<ChatRoom> getAdminRooms(Long adminId){
        return chatRoomRepository.findByAdminId(adminId);
    }

    public List<ChatMessage> getChatHistoryDto(
            Long userId1,
            Long userId2
    ){

        List<Message> msgs =
                getChatHistory(userId1, userId2);

        return msgs.stream()
                .map(m -> new ChatMessage(
                        m.getSenderId(),
                        null, // toUserId isn't needed for history
                        m.getContent(),
                        m.getSentAt().toString()
                ))
                .toList();
    }
}