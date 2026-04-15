package com.once.globalnews.user.presentation.model.response;

import jakarta.validation.constraints.NotEmpty;

public record AccessTokenResponse(
        @jakarta.validation.constraints.NotEmpty String accessToken,
        boolean isFirstLogin,
        String nickname,
        String profileImage,
        String preferredCountry
) {
}
