package com.st3.uber.controller;

import com.st3.uber.dto.chat.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage message, Principal principal) {

        // sender ID iz JWT websocket auth
        Long fromUserId = Long.parseLong(principal.getName());

        message.setFromUserId(fromUserId);
        message.setTimestamp(Instant.now().toString());

        // šaljemo direktno primaocu
        messagingTemplate.convertAndSendToUser(
                message.getToUserId().toString(),
                "/queue/messages",
                message
        );
    }
}
