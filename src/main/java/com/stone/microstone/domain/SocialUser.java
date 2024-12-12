package com.stone.microstone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name ="user")
@Getter
@Setter
public class SocialUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int user_id;

    @Column(nullable = false)
    private String user_email;

    @Column
    private String user_name;

    @Column
    private String user_password;

    @Column
    private String user_phone;

    @Column
    private String user_logininfo;

    @Column
    private String user_token;

//    @OneToMany(mappedBy = "user")
//    private List<workbook> workbooks;

    public void setLocalLogin(String password){
        this.user_password = password;
        this.user_token = null;
        this.user_logininfo = "local";
    }

    public void setSocialLogin(String usertoken,
                               String logininfo){
        this.user_password = null;
        this.user_token = usertoken;
        this.user_logininfo = logininfo;
    }
}
