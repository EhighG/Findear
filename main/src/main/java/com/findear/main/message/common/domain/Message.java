package com.findear.main.message.common.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tbl_message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_room_id")
    private MessageRoom messageRoom;

    private String title;

    private Long senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime sendAt;

    @Builder
    public Message(MessageRoom messageRoom, String title, Long senderId, String content, LocalDateTime sendAt) {
        this.messageRoom = messageRoom;
        this.title = title;
        this.senderId = senderId;
        this.content = content;
        this.sendAt = sendAt;
    }

}
