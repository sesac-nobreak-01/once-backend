package com.once.globalnews.chat.infrastructure.persistence;

import com.once.globalnews.chat.domain.ChatAttachment;
import com.once.globalnews.chat.domain.ChatAttachmentStatus;
import com.once.globalnews.chat.domain.ChatSession;
import com.once.globalnews.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

    Optional<ChatAttachment> findByIdAndUser(Long id, User user);

    List<ChatAttachment> findAllByIdInAndUser(Collection<Long> ids, User user);

    List<ChatAttachment> findAllByStatusAndCreatedAtBefore(ChatAttachmentStatus status, LocalDateTime threshold);

    @Modifying
    @Query("delete from ChatAttachment a where a.chatSession = :session")
    int deleteAllByChatSession(ChatSession session);
}
