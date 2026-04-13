package com.once.globalnews.global.common.model;

import com.once.globalnews.global.common.status.ErrorStatus;
import com.once.globalnews.global.common.status.SuccessStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final Boolean isSuccess;
    private final String code;
    private final String message;

    private T data;

    // 성공한 경우 응답 생성
    public static <T> ApiResponse<T> onSuccess(SuccessStatus successStatus, T data){
        return new ApiResponse<>(true, successStatus.getCode() , successStatus.getMessage(), data);
    }

    // 실패한 경우 응답 생성
    public static <T> ApiResponse<T> onFailure(ErrorStatus commonErrorStatus, String message){
        return new ApiResponse<>(false, commonErrorStatus.getCode(),  message, null);
    }

    public static <T> ApiResponse<T> onFailure(ErrorStatus commonErrorStatus){
        return new ApiResponse<>(false, commonErrorStatus.getCode(), commonErrorStatus.getMessage(), null);
    }

}

