package com.stone.microstone.domain;

import com.stone.microstone.domain.repository.SocialUserRepository;
import com.stone.microstone.domain.repository.SocialUserRepositoryImpl;

import com.stone.microstone.domain.service.SocialUserSerivce;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public SocialUserRepository SocialuserRepository(){
        return new SocialUserRepositoryImpl(em);
    }

    @Bean
    public SocialUserSerivce socialUserService(SocialUserRepository userRepository) {
        return new SocialUserSerivce(userRepository);
    }

//    @Bean
//    public AuthController authController() {}
}
