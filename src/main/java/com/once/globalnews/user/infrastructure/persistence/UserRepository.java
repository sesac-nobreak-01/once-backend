package com.once.globalnews.user.infrastructure.persistence;

import com.once.globalnews.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(Long kakaoId);

    boolean existsByNickname(String newNickname);
}
