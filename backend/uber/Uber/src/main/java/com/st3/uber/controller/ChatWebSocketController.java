package com.st3.uber.controller;

import com.st3.uber.domain.User;
import com.st3.uber.dto.chat.ChatMessage;
import com.st3.uber.enums.UserRole;
import com.st3.uber.repository.UserRepository;
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
    private final UserRepository userRepository;

    public ChatWebSocketController(
            SimpMessagingTemplate messagingTemplate,
            ChatService chatService,
            UserRepository userRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage message, Principal principal) {

        Long fromUserId = Long.parseLong(principal.getName());

        message.setFromUserId(fromUserId);
        message.setTimestamp(Instant.now().toString());

        // Save message to database
        chatService.saveMessage(
                fromUserId,
                message.getToUserId(),
                message.getContent()
        );

       Long recipientId = message.getToUserId();

        System.out.println("📨 Sending WebSocket message:");
        System.out.println("   From: " + fromUserId);
        System.out.println("   To: " + recipientId);
        System.out.println("   Content: " + message.getContent());

        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                message
        );

        System.out.println("✅ Message sent to user " + recipientId + " via WebSocket");
    }
}