package com.stone.microstone.domain.repository;

import com.stone.microstone.domain.SocialUser;
import com.stone.microstone.locallogin.domain.entity.User;


import java.util.Optional;

public interface SocialUserRepository {
    User save(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByLoginInfo(String email,String loginInfo);
    void deleteAll();
}
