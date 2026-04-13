package com.once.globalnews.user.presentation.controller;

import com.once.globalnews.global.common.model.ApiResponse;
import com.once.globalnews.global.common.status.SuccessStatus;
import com.once.globalnews.global.security.annotation.GlobalNewsUser;
import com.once.globalnews.user.application.UserService;
import com.once.globalnews.user.domain.User;
import com.once.globalnews.user.presentation.model.request.UpdateNicknameRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "닉네임 수정 API",
            description = ""
    )
    @PatchMapping("/nickname")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<String> updateNickname(
            @Parameter(hidden = true) @GlobalNewsUser User user,
            @RequestBody @Valid UpdateNicknameRequest updateNicknameRequest)
    {
        return ApiResponse.onSuccess(
                SuccessStatus.UPDATE_NICKNAME_SUCCESS,
                userService.updateNickname(user, updateNicknameRequest));
    }
}
