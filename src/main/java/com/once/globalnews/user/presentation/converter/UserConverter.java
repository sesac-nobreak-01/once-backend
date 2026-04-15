package com.once.globalnews.user.presentation.converter;

import com.once.globalnews.user.domain.User;
import com.once.globalnews.user.presentation.model.response.KakaoUserInfoResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserConverter {
    public static User userOf(KakaoUserInfoResponse userInfo) {
        String nickname = "익명 사용자";
        String email = null;
        String profileImage = null;

        if (userInfo.getKakaoAccount() != null) {
            email = userInfo.getKakaoAccount().getEmail();
            if (userInfo.getKakaoAccount().getProfile() != null) {
                nickname = userInfo.getKakaoAccount().getProfile().getNickName();
                profileImage = userInfo.getKakaoAccount().getProfile().getProfileImageUrl();
            }
        }

        return User.builder()
                .kakaoId(userInfo.getId())
                .nickname(nickname != null ? nickname : "익명 사용자")
                .email(email)
                .profileImage(profileImage)
                .build();
    }
}

