package com.st3.uber.domain;

import com.st3.uber.enums.SenderType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType;

    // Optional: store sender id for quick reference
    private Long senderId;

    private LocalDateTime sentAt = LocalDateTime.now();

    // getters & setters
}
