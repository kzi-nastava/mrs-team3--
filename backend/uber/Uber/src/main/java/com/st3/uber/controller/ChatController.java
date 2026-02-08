package com.st3.uber.controller;

import com.st3.uber.domain.ChatRoom;
import com.st3.uber.domain.Message;
import com.st3.uber.dto.chat.AdminDto;
import com.st3.uber.dto.chat.ChatMessage;
import com.st3.uber.dto.chat.ChatRoomDto;
import com.st3.uber.service.ChatService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @RolesAllowed({"PASSENGER","ADMIN","DRIVER"})
    @GetMapping("/history")
    public List<ChatMessage> history(
            @RequestParam Long user1,
            @RequestParam Long user2
    ){
        return chatService.getChatHistoryDto(user1, user2);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/rooms")
    public List<ChatRoomDto> adminRooms(
            @RequestParam Long adminId){

        return chatService.getAdminRooms(adminId)
                .stream()
                .map(ChatRoomDto::new)
                .toList();
    }
    @RolesAllowed({"PASSENGER","ADMIN","DRIVER"})
    @GetMapping("/admin")
    public AdminDto getFirstAdmin() {
        return chatService.getAdmin();
    }


}
