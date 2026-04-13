package com.once.globalnews.user.presentation.model.response;

import jakarta.validation.constraints.NotEmpty;

public record AccessTokenResponse(
        @NotEmpty String accessToken,
        boolean isFirstLogin
) {
}
