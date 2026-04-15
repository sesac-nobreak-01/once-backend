package com.once.globalnews.user.presentation.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfoResponse {

    @JsonProperty("id")
    public Long id;

    @JsonProperty("has_signed_up")
    public Boolean hasSignedUp;

    @JsonProperty("connected_at")
    public Date connectedAt;

    @JsonProperty("synched_at")
    public Date synchedAt;

    @JsonProperty("kakao_account")
    public KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class KakaoAccount {

        @JsonProperty("profile_needs_agreement")
        public Boolean isProfileAgree;

        @JsonProperty("profile_nickname_needs_agreement")
        public Boolean isNickNameAgree;

        @JsonProperty("profile_image_needs_agreement")
        public Boolean isProfileImageAgree;

        @JsonProperty("profile")
        public Profile profile;

        @JsonProperty("ci_needs_agreement")
        public Boolean isCIAgree;

        @JsonProperty("ci")
        public String ci;

        @JsonProperty("ci_authenticated_at")
        public Date ciCreatedAt;

        @JsonProperty("email_needs_agreement")
        public Boolean isEmailAgree;

        @JsonProperty("email")
        public String email;

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public class Profile {

            @JsonProperty("nickname")
            public String nickName;

            @JsonProperty("thumbnail_image_url")
            public String thumbnailImageUrl;

            @JsonProperty("profile_image_url")
            public String profileImageUrl;

            @JsonProperty("is_default_image")
            public String isDefaultImage;
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Partner {
        @JsonProperty("uuid")
        public String uuid;
    }
}
