package com.cst.campussecondhand.repository;

import com.cst.campussecondhand.entity.ChatMessage;
import com.cst.campussecondhand.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    // 查找两个用户之间的所有聊天记录，并按时间升序排列
    List<ChatMessage> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
            Integer senderId1, Integer recipientId1, Integer senderId2, Integer recipientId2);

    // 查找与指定用户相关的所有对话伙伴
    @Query("SELECT DISTINCT CASE WHEN m.sender.id = ?1 THEN m.recipient.id ELSE m.sender.id END FROM ChatMessage m WHERE m.sender.id = ?1 OR m.recipient.id = ?1")
    List<Integer> findConversationPartnerIds(Integer userId);

    // 新增：统计某个用户收到的所有未读消息总数
    long countByRecipientIdAndIsReadFalse(Integer recipientId);

    // 新增：检查某个用户与特定伙伴之间是否有未读消息
    boolean existsByRecipientIdAndSenderIdAndIsReadFalse(Integer recipientId, Integer senderId);

    // 新增：将两个用户之间的所有未读消息标记为已读
    @Transactional
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.sender.id = ?1 AND m.recipient.id = ?2 AND m.isRead = false")
    void markMessagesAsRead(Integer senderId, Integer recipientId);
}