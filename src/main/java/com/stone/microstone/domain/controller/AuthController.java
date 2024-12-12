package com.stone.microstone.domain.controller;

import com.stone.microstone.domain.*;
import com.stone.microstone.domain.service.GoogleService;
import com.stone.microstone.domain.service.KakaoService;
import com.stone.microstone.domain.service.NaverService;
import com.stone.microstone.domain.service.SocialUserSerivce;

import com.stone.microstone.locallogin.domain.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final SocialUserSerivce userSerivce;
    private final KakaoService kakaoService;
    private final NaverService naverService;
    private final GoogleService googleService;

    @Autowired
    public AuthController(SocialUserSerivce userSerivce, KakaoService kakaoService, NaverService naverService, GoogleService googleService){
        this.userSerivce = userSerivce;
        this.kakaoService = kakaoService;
        this.naverService = naverService;
        this.googleService = googleService;
    }

    @DeleteMapping("/Delete")
    public ResponseEntity<String> deleteUser(){
        userSerivce.deleteAllUsers();
        log.info("인포로 만들었어용");
        return ResponseEntity.ok("다 지우기 성공");
    }

    @PostMapping("/kakao-login")
    public ResponseEntity<?> kakaoLogin(@RequestHeader("Authorization") String accesToken, HttpSession session, HttpServletResponse response){
        try{
            KakaoUserInfo userInfo = kakaoService.getUserInfo(accesToken);
            if(userInfo.getEmail()==null ){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),"이메일이 존재하지 않습니다."));
            }else if(userInfo.getNickname() ==null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),"닉네임이 존재하지 않습니다."));
            }
            User user= userSerivce.findOrCreateUser(userInfo.getEmail(), userInfo.getNickname(), accesToken,"kakao");
            //SocialUser user= userSerivce.findOrCreateUser("huns0905@naver.com","hhs" , "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c","kakao");

            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("loginType","kakao");
            //session.setMaxInactiveInterval(7 * 24 * 60 * 60);
//            Cookie sessionCookie = new Cookie("SESSIONID", session.getId());
//            sessionCookie.setHttpOnly(true);
//            sessionCookie.setPath("/");
//            sessionCookie.setMaxAge(60*60);

//            response.addCookie(sessionCookie);
            return ResponseEntity.ok(Map.of("로그인 성공",accesToken,"userId",user.getId()));
        }catch(Exception e){
            ErrorResponse errorResponse=new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),"로그인 실패"+e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/naver-login")
    public ResponseEntity<?> naverLogin(@RequestHeader("Authorization") String accesToken, HttpSession session, HttpServletResponse response){
        try{
            NaverUserInfo userInfo = naverService.getUserInfo(accesToken);
            if(userInfo.getEmail()==null ){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),"이메일이 존재하지 않습니다."));
            }else if(userInfo.getName() ==null){
                log.info(userInfo.getEmail());
                log.info(userInfo.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),"닉네임이 존재하지 않습니다."));
            }
            User user=userSerivce.findOrCreateUser(userInfo.getEmail(), userInfo.getName() , accesToken,"naver");
            //SocialUser user= userSerivce.findOrCreateUser("huns0905@naver.com","hhs" , "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c","kakao");
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("loginType","naver");
            //session.setMaxInactiveInterval(7 * 24 * 60 * 60);
//            Cookie sessionCookie = new Cookie("SESSIONID", session.getId());
//            sessionCookie.setHttpOnly(true);
//            sessionCookie.setPath("/");
//            sessionCookie.setMaxAge(60*60);
//
//            response.addCookie(sessionCookie);

            return ResponseEntity.ok(Map.of("로그인 성공",accesToken,"userId",user.getId()));
        }catch(Exception e){
            ErrorResponse errorResponse=new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),"로그인 실패"+e.getMessage()); // 예외 스택 트레이스를 출력하여 디버깅에 도움
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestHeader("Authorization") String accesToken, HttpSession session, HttpServletResponse response){
        try{
            //String accessToken = accesToken.replace("Bearer ", "");
            GoogleUserInfo userInfo = googleService.getUserInfo(accesToken);
            if(userInfo.getEmail()==null ){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),"이메일이 존재하지 않습니다."));
            }else if(userInfo.getName() ==null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),"닉네임이 존재하지 않습니다."));
            }
            User user= userSerivce.findOrCreateUser(userInfo.getEmail(), userInfo.getName(), accesToken,"google");
            //SocialUser user= userSerivce.findOrCreateUser("huns0905@naver.com","hhs" , "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c","kakao");
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("loginType","google");
            //session.setMaxInactiveInterval(7 * 24 * 60 * 60);
//            Cookie sessionCookie = new Cookie("SESSIONID", session.getId());
//            sessionCookie.setHttpOnly(true);
//            sessionCookie.setPath("/");
//            sessionCookie.setMaxAge(60*60);
//
//            response.addCookie(sessionCookie);

            return ResponseEntity.ok(Map.of("로그인 성공",accesToken,"userId",user.getId()));
        }catch(Exception e){
            ErrorResponse errorResponse=new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),"로그인 실패"+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
