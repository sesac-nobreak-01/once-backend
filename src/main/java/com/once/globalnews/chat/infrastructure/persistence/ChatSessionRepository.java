package com.once.globalnews.chat.infrastructure.persistence;

import com.once.globalnews.chat.domain.ChatSession;
import com.once.globalnews.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Page<ChatSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT cs FROM ChatSession cs LEFT JOIN FETCH cs.messages WHERE cs.id = :id AND cs.user = :user")
    Optional<ChatSession> findByIdAndUserWithMessages(@Param("id") Long id, @Param("user") User user);

    Optional<ChatSession> findByIdAndUser(Long id, User user);

    Optional<ChatSession> findFirstByUserAndNewsIdAndIsActiveTrueOrderByCreatedAtDesc(User user, String newsId);

    boolean existsByIdAndUser(Long id, User user);
}