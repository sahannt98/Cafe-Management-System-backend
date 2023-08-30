package com.inn.cafe.serviceimpl;

import com.google.common.base.Strings;
import com.inn.cafe.JWT.CustomerUserDetailsService;
import com.inn.cafe.JWT.JwtAuthFilter;
import com.inn.cafe.JWT.JwtUtil;
import com.inn.cafe.POJO.Role;
import com.inn.cafe.POJO.User;
import com.inn.cafe.constants.CafeConstants;
import com.inn.cafe.dao.UserDao;
import com.inn.cafe.service.UserService;
import com.inn.cafe.utils.CafeUtils;
import com.inn.cafe.utils.EmailUtils;
import com.inn.cafe.wrapper.UserWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private  final UserDao userDao;

    private final AuthenticationManager authenticationManager;

    private  final CustomerUserDetailsService customerUserDetailsService;

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    private final JwtAuthFilter jwtAuthFilter;

    private final EmailUtils emailUtils;


    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        log.info("inside signup {}", requestMap);
        try {
            if (validateSignUpMap(requestMap)) {
                User user = userDao.findByEmailId(requestMap.get("email"));
                if (Objects.isNull(user)) {
                    userDao.save(getUserFromMap(requestMap));
                    return CafeUtils.getResponseEntity("Successfully Registered", HttpStatus.OK);
                } else {
                    return CafeUtils.getResponseEntity("Email already exits", HttpStatus.BAD_REQUEST);
                }
            } else {
                return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside login");
        try {

            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestMap.get("email"), requestMap.get("password"))
            );
            if (auth.isAuthenticated()){
                if (customerUserDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")){
                    return new ResponseEntity<String>("{\"token\":\""+
                            jwtUtil.generateToken(customerUserDetailsService.getUserDetail().getEmail(),
                                    customerUserDetailsService.getUserDetail().getRole()) + "\"}", HttpStatus.OK);
                }
                else {
                    return new ResponseEntity<String>("{\"message\":\""+"Wait for admin approval"+"\"}",
                            HttpStatus.BAD_REQUEST);
                }
            }
        }catch (Exception ex) {
            log.error("{}",ex);
        }
        return new ResponseEntity<String>("{\"message\":\""+"Bad credentials"+"\"}",
                HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllUsers() {
        try {
            if (jwtAuthFilter.isAdmin()){
                return new ResponseEntity<>(userDao.getAllUsers(), HttpStatus.OK);
            }
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if (jwtAuthFilter.isAdmin()){
                Optional<User> optionalUser = userDao.findById(Integer.parseInt(requestMap.get("id")));
                if (optionalUser.isPresent()){
                    userDao.updateStatus(requestMap.get("status"), Integer.parseInt(requestMap.get("id")));
                    sendMailToAllAdmins(requestMap.get("status"), optionalUser.get().getEmail(), userDao.getAllAdmins());
                    return CafeUtils.getResponseEntity("User status updated successfully", HttpStatus.OK);
                }
                return CafeUtils.getResponseEntity("User id doesn't exits", HttpStatus.OK);
            }
            return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return  CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> checkToken() {
        return CafeUtils.getResponseEntity("true", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            User user = userDao.findByEmail(jwtAuthFilter.getCurrentUser());
            if (user != null){
                if(user.getPassword().equals(requestMap.get("oldPassword"))){
                    user.setPassword(requestMap.get("newPassword"));
                    userDao.save(user);
                    return CafeUtils.getResponseEntity("Password changed successfully", HttpStatus.OK);
                }
                return CafeUtils.getResponseEntity("Incorrect old password", HttpStatus.BAD_REQUEST);
            }
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return  CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            User user = userDao.findByEmailId(requestMap.get("email"));
            if (user != null && !Strings.isNullOrEmpty(user.getEmail()))
                emailUtils.forgotMail(user.getEmail(), "Credential by Flavorfusion", user.getPassword());
            return CafeUtils.getResponseEntity("Check your mail for credintials", HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void sendMailToAllAdmins(String status, String user, List<String> allAdmins) {
        allAdmins.remove(jwtAuthFilter.getCurrentUser());
        if (status != null && status.equalsIgnoreCase("true")){
            emailUtils.sendSimpleMessage(
                    jwtAuthFilter.getCurrentUser(),
                    "Account Approved",
                    "USER:- "+user+"\n " +
                    "is approved by \n ADMIN :-"+jwtAuthFilter.getCurrentUser(),
                    allAdmins
            );
        }else{
            emailUtils.sendSimpleMessage(
                    jwtAuthFilter.getCurrentUser(),
                    "Account Disabled",
                    "USER:- "+user+"\n " +
                            "is disabled by \n ADMIN :-"+jwtAuthFilter.getCurrentUser(),
                    allAdmins
            );
        }
    }

    private boolean validateSignUpMap(Map<String, String> requestMap) {
        if (requestMap.containsKey("name") && requestMap.containsKey("contactNumber")
                && requestMap.containsKey("email") && requestMap.containsKey("password")){
            return true;
        }
        return false;
    }

    private User getUserFromMap(Map<String, String> requestMap){
        User user = new User();
        user.setName(requestMap.get("name"));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(requestMap.get("password"));
        user.setStatus("false");
        user.setRole(Role.USER.name());
        return user;
    }
}
