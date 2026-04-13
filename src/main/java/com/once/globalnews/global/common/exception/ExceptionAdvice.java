package com.once.globalnews.global.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.once.globalnews.global.common.model.ApiResponse;
import com.once.globalnews.global.common.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.BindException;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            ServletRequestBindingException.class,
            MissingServletRequestPartException.class,
            ConversionNotSupportedException.class,
            TypeMismatchException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            HandlerMethodValidationException.class,
    })
    public ResponseEntity<ApiResponse<ErrorStatus>> handleConstraintViolation(HttpServletRequest req, Exception e) {

        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException)e;
            String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
            return handle(req, ErrorStatus.INVALID_PARAMETER, errorMessage);
        }

        return handle(req,ErrorStatus.INVALID_PARAMETER);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
    })
    public ResponseEntity<ApiResponse<ErrorStatus>> constraintViolationException(HttpServletRequest req, Exception e) {
        if (e instanceof ConstraintViolationException) {
            String message = ((ConstraintViolationException) e).getConstraintViolations()
                    .iterator()
                    .next()
                    .getMessage();
            return handle(req, ErrorStatus.INVALID_PARAMETER, message);
        }
        return handle(req,ErrorStatus.INVALID_PARAMETER);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
    })
    public ResponseEntity<ApiResponse<ErrorStatus>> httpMessageNotReadableException(HttpServletRequest req, Exception e) {
        if( e.getCause() instanceof InvalidFormatException invalidEx){
            String fieldName = invalidEx.getPath().get(0).getFieldName();
            String message = String.format("'%s' 필드의 값 형식이 올바르지 않습니다.", fieldName);
            return handle(req, ErrorStatus.INVALID_PARAMETER, message);
        }
        return handle(req,ErrorStatus.INVALID_PARAMETER);
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ApiResponse<ErrorStatus>> httpRequestMethodNotSupportedException(HttpServletRequest req, Exception e) {
        return handle(req,ErrorStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler({ServiceException.class})
    public ResponseEntity<ApiResponse<ErrorStatus>> serviceException(HttpServletRequest req, ServiceException e) {
        ErrorStatus errorStatus;
        try {
            errorStatus = ErrorStatus.valueOf(e.getErrCode());
        } catch (IllegalArgumentException iae) {
            errorStatus = ErrorStatus.INTERNAL_SERVER_ERROR;
        }
        return handle(req,errorStatus);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ErrorStatus>> noResourceFoundException(HttpServletRequest req,Exception e) {
        return handle(req,ErrorStatus.NO_RESOURCE_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorStatus>> handleException(HttpServletRequest req,Exception e) {
        log.error("Unhandled exception: ", e);
        return handle(req,ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<ErrorStatus>> handle(HttpServletRequest request, ErrorStatus errorStatus) {
        return new ResponseEntity<>(ApiResponse.onFailure(errorStatus), errorStatus.getHttpStatus());
    }

    private ResponseEntity<ApiResponse<ErrorStatus>> handle(HttpServletRequest request, ErrorStatus errorStatus,String message) {
        return new ResponseEntity<>(ApiResponse.onFailure(errorStatus,message), errorStatus.getHttpStatus());
    }


}
