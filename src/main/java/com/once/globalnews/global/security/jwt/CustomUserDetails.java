package com.once.globalnews.global.security.jwt;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Collections;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CustomUserDetails extends User {
	private final Long id;
	private final String nickname;
	private final String profileImage;

	public CustomUserDetails(Long id, Collection<? extends GrantedAuthority> authorities,
                             String nickname, String profileImage) {
		super(String.valueOf(id), "", true, true, true, true,
			authorities != null ? authorities : Collections.emptyList());
		this.id = id;
		this.nickname = nickname;
		this.profileImage = profileImage;
	}

	public static CustomUserDetails from(com.once.globalnews.user.domain.User user) {
		return new CustomUserDetails(
			user.getId(),
			null,
			user.getNickname(),
			user.getProfileImage()
		);
	}
}
