package com.once.globalnews.chat.infrastructure.persistence;

import com.once.globalnews.chat.domain.ChatMessage;
import com.once.globalnews.chat.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionOrderByCreatedAtAsc(ChatSession chatSession);
}