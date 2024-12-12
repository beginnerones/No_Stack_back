package com.stone.microstone.domain.repository;

import com.stone.microstone.domain.SocialUser;
import com.stone.microstone.locallogin.domain.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


import java.util.List;
import java.util.Optional;

public class SocialUserRepositoryImpl implements SocialUserRepository {
    @PersistenceContext
    private EntityManager em;

    public SocialUserRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public User save(User user) {
        em.persist(user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        List<User> result = em.createQuery("SELECT u FROM User u WHERE u.name = :username", User.class)
                .setParameter("username", username)
                .getResultList();
        return result.stream().findFirst();
    }
    @Override
    public Optional<User> findByEmail(String email) {
        List<User> result = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public Optional<User> findByLoginInfo(String email,String loginInfo) {
        List<User> result=em.createQuery("SELECT u FROM User u WHERE u.email= : email AND u.loginInfo = :loginInfo",User.class)
                .setParameter("email",email)
                .setParameter("loginInfo",loginInfo)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public void deleteAll() {
        em.createQuery("DELETE FROM User").executeUpdate();
        em.createNativeQuery("ALTER TABLE user AUTO_INCREMENT = 1").executeUpdate();
    }
}


