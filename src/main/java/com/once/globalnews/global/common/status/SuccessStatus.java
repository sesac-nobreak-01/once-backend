package com.once.globalnews.global.common.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus{
    // 일반적인 응답
    OK(HttpStatus.OK, "COMMON200", "성공입니다."),
    CREATED(HttpStatus.CREATED, "COMMON201", "생성되었습니다."),

    // User 관련 응답
    USER_KAKAO_LOGIN_SUCCESS(HttpStatus.OK, "USER_KAKAO_LOGIN_SUCCESS", "로그인에 성공하였습니다."),
    UPDATE_NICKNAME_SUCCESS(HttpStatus.OK, "UPDATE_NICKNAME_SUCCESS", "닉네임 변경에 성공하였습니다.");
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}

