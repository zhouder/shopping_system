package com.cst.campussecondhand.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "chat_message")
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Date timestamp;

    // 新增字段：标记消息是否已读，默认为 false (未读)
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;
}