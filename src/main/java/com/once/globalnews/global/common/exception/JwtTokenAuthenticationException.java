package com.once.globalnews.global.common.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenAuthenticationException extends AuthenticationException {
	public JwtTokenAuthenticationException(String msg) {
		super(msg);
	}
}
