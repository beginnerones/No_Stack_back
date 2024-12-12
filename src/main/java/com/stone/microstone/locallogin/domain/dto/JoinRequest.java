package com.stone.microstone.locallogin.domain.dto;

import com.stone.microstone.locallogin.domain.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRequest {
    private String email;
    private String name;
    private String password;
    private String passwordCheck;
    private String phone;  // 변경: Integer -> String

    public User toEntity() {
        User user = new User();
        user.setEmail(this.email);
        user.setName(this.name);
        user.setPassword(this.password);
        user.setPhone(this.phone);
        return user;
    }
}