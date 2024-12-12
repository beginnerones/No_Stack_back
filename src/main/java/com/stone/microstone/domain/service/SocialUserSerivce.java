package com.stone.microstone.domain.service;

import com.stone.microstone.domain.SocialUser;

import com.stone.microstone.domain.repository.SocialUserRepository;

import com.stone.microstone.locallogin.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

//@Service
public class SocialUserSerivce {
//    @Autowired
    private SocialUserRepository userRepository;
    @Autowired
    public SocialUserSerivce(SocialUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreateUser(String email, String username, String token, String logininfo){
        Optional<User> users = userRepository.findByLoginInfo(email,logininfo);
        User user;
        if(users.isEmpty()){
            user=new User();
            user.setEmail(email);
            user.setName(username);
            user.setSocialLogin(token,logininfo);
            userRepository.save(user);
        }else{
            user=users.get();
            //로그인 실패는 존재하지 않으니,찾으면 그냥 유저 정보를 반환.나중에 변경
            user.setSocialLogin(token,logininfo);
            userRepository.save(user);
        }
        return user;
    }


    @Transactional
    public void deleteAllUsers() {
        userRepository.deleteAll();

    }
}
