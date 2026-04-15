package com.once.globalnews.global.common.status;

import com.once.globalnews.global.common.exception.ServiceException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

    // Common Error
    NO_HANDLER_FOUND(HttpStatus.NOT_FOUND, "COMMON500","no handler found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,"COMMON405", "method not allowed"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON400","Invalid parameter"),
    NO_PERMISSION(HttpStatus.FORBIDDEN, "COMMON403","no permission"),
    NOT_AUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON403","not authorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "COMMON403","Invalid Token. Please request a new token."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "COMMON400","Invalid status"),
    NO_RESOURCE_FOUND(HttpStatus.NOT_FOUND, "COMMON404","no resource found"),

    // JWT Error
    INVALID_ACCESSTOKEN(HttpStatus.BAD_REQUEST, "COMMON400","invalid accessToken"),
    NOT_EXIST_ACCESSTOKEN(HttpStatus.BAD_REQUEST, "COMMON400","Access Token not exist"),

    // User Error
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER_DUPLICATE_NICKNAME","중복된 닉네임입니다."),

    // Chat Error
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404", "채팅 세션을 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404", "채팅 메시지를 찾을 수 없습니다."),
    CHAT_AI_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CHAT503", "AI 서비스가 일시적으로 사용할 수 없습니다."),
    CHAT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "CHAT400", "잘못된 채팅 요청입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public ServiceException serviceException() {
        throw new ServiceException(this.name(), this.message);
    }


}

