package com.st3.uber.controller;

import com.st3.uber.dto.chat.ChatMessage;
import com.st3.uber.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public ChatWebSocketController(
            SimpMessagingTemplate messagingTemplate,
            ChatService chatService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage message, Principal principal) {

        Long fromUserId = Long.parseLong(principal.getName());

        message.setFromUserId(fromUserId);
        message.setTimestamp(Instant.now().toString());

        chatService.saveMessage(
                fromUserId,
                message.getToUserId(),
                message.getContent()
        );

        Long adminId = 3L;

// ako sender NIJE admin → šalji adminu
        if(!fromUserId.equals(adminId)){
            message.setToUserId(adminId);
        }

        Long targetId = message.getToUserId();

        messagingTemplate.convertAndSendToUser(
                targetId.toString(),
                "/queue/messages",
                message
        );

    }

}
