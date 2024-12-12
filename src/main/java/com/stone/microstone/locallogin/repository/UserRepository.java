package com.stone.microstone.locallogin.repository;

import com.stone.microstone.locallogin.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // 추가된 메서드
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailAndPhoneAndName(String email, String phone, String name);
    Optional<User> findByEmailAndLoginInfo(String email, String loginInfo);
    Optional<User> findById(int id);
}