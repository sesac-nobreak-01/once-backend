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
    CHAT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "CHAT400", "잘못된 채팅 요청입니다."),

    // Chat Attachment Error
    CHAT_ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ATTACH_404", "첨부파일을 찾을 수 없습니다."),
    CHAT_ATTACHMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT_ATTACH_403", "해당 첨부파일에 접근할 권한이 없습니다."),
    CHAT_ATTACHMENT_INVALID_MIME(HttpStatus.BAD_REQUEST, "CHAT_ATTACH_400", "지원하지 않는 파일 형식입니다."),
    CHAT_ATTACHMENT_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "CHAT_ATTACH_413", "파일 크기가 허용 한도를 초과했습니다."),
    CHAT_ATTACHMENT_ALREADY_LINKED(HttpStatus.CONFLICT, "CHAT_ATTACH_409", "이미 메시지에 연결된 첨부파일입니다."),
    CHAT_ATTACHMENT_INVALID_STATE(HttpStatus.BAD_REQUEST, "CHAT_ATTACH_400", "첨부파일 상태가 유효하지 않습니다."),
    CHAT_ATTACHMENT_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "CHAT_ATTACH_400", "메시지당 첨부 가능한 파일 수를 초과했습니다."),
    CHAT_UPLOAD_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CHAT_ATTACH_429", "일일 업로드 한도를 초과했습니다."),
    STORAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE500", "스토리지 업로드 처리에 실패했습니다."),
    STORAGE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE500", "스토리지 다운로드 처리에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public ServiceException serviceException() {
        throw new ServiceException(this.name(), this.message);
    }


}

