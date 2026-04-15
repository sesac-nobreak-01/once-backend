package com.once.globalnews.user.application;

import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.user.domain.User;
import com.once.globalnews.user.infrastructure.persistence.UserRepository;
import com.once.globalnews.user.presentation.model.request.UpdateNicknameRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public String updateNickname(User user, UpdateNicknameRequest updateNicknameRequest) {
        String newNickname = updateNicknameRequest.nickname();

        if (userRepository.existsByNickname(newNickname)) {
            throw ErrorStatus.DUPLICATE_NICKNAME.serviceException();
        }
        return user.updateNickname(newNickname);
    }

    @Transactional
    public void updatePreferredCountry(User user, String country) {
        user.updatePreferredCountry(country);
        userRepository.save(user);
    }
}
