package com.stone.microstone.domain;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NaverUserInfo {
//    private String resultcode;
//    private String message;
    @JsonProperty("response")
    private Response response;

    @Getter
    @Setter
    private static class Response{
        private String email;
        private String name;
    }
//    @JsonProperty("response")
//    private void unpackNested(Map<String, Object> response) {
//        this.email = (String) response.get("email");
//        this.nickname = (String) response.get("nickname");
//    }

    public String getEmail() {
        return response != null ? response.getEmail() : null;
    }
    public String getName() {return response != null ? response.getName() : null;}
}
