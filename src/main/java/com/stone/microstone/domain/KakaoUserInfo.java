package com.stone.microstone.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Profile;

@Getter
@Setter
public class KakaoUserInfo {
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;
    @Getter
    @Setter
    public static class KakaoAccount {
        private String email;

        @JsonProperty("profile")
        private Profile profile;

        @Getter
        @Setter
        public static class Profile {
            private String nickname;
        }
    }
    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.getEmail() : null;
    }

    public String getNickname() {
        return kakaoAccount != null && kakaoAccount.getProfile() != null ?
                kakaoAccount.getProfile().getNickname() : null;
    }

}
