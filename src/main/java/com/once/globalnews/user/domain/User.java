package com.once.globalnews.user.domain;

import com.once.globalnews.global.common.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "kakao_id", nullable = false, unique = true)
    private Long kakaoId;

    @NotNull
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "preferred_country", length = 50)
    private String preferredCountry;

    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "USER";

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    public String updateNickname(String nickname) {
        this.nickname = nickname;
        return this.nickname;
    }

    public String updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
        return this.profileImage;
    }

    public void updatePreferredCountry(String preferredCountry) {
        this.preferredCountry = preferredCountry;
    }
    }