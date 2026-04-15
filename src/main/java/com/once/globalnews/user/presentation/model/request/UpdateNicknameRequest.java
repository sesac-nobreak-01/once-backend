package com.once.globalnews.user.presentation.model.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateNicknameRequest(
        @NotBlank String nickname
){
}