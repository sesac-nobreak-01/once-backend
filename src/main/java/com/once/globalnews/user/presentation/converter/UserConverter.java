package com.once.globalnews.user.presentation.converter;

import com.once.globalnews.user.domain.User;
import com.once.globalnews.user.presentation.model.response.KakaoUserInfoResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserConverter {
    public static User userOf(KakaoUserInfoResponse userInfo) {
        return User.builder()
                .kakaoId(userInfo.getId())
                .nickname("globalnews-new-kakao-user")
                .profileImage(userInfo.getKakaoAccount().getProfile().getProfileImageUrl())
                .build();
    }
}

