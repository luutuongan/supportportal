package com.example.supportportal.service.impl;

import com.example.supportportal.constant.Authority;
import com.example.supportportal.domain.User;
import com.example.supportportal.domain.UserPrincipal;
import com.example.supportportal.enumeration.Role;
import com.example.supportportal.exception.domain.EmailExistedException;
import com.example.supportportal.exception.domain.UserExistedException;
import com.example.supportportal.exception.domain.UserNotFoundException;
import com.example.supportportal.repo.UserRepository;
import com.example.supportportal.service.EmailService;
import com.example.supportportal.service.LoginAttemptService;
import com.example.supportportal.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    private UserRepository userRepository;

    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private LoginAttemptService loginAttemptService;

    private EmailService emailService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder,
                           LoginAttemptService loginAttemptService, EmailService emailService) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(s);
        if (user==null) {
            LOGGER.error("User not found by username: " + s);
            throw new UsernameNotFoundException("User not found by username: " + s);
        } else {
            validateLoginAttempt(user);
            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info("Returning found user by username: " + s);
            return userPrincipal;
        }
    }

    private void validateLoginAttempt(User user) {
        if (user.isNonLocked()) {
            if (loginAttemptService.hasExceedMaxAttempts(user.getUsername())) {
                user.setNonLocked(false);
            } else {
                user.setNonLocked(true);
            }
        } else {
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }

    @Override
    public User register(String firstName, String lastName, String username, String email) throws
            UserExistedException, EmailExistedException, UserNotFoundException, MessagingException {
        validateUsernameAndEmail(StringUtils.EMPTY, username, email);
        User user = new User();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setActive(true);
        user.setNonLocked(true);
        user.setRole(Role.ROLE_USER.name());
        user.setAuthorities(Role.ROLE_USER.getAuthorities());
        user.setProfileImageUrl(getTempProfImgUrl());
        userRepository.save(user);
        LOGGER.info("New user password: "+password);
        emailService.sendNewPasswordEmail(firstName, password, email);
        return user;
    }

    private String getTempProfImgUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/img/profile/temp").toUriString();
    }

    private String encodePassword(String password) {
        return bCryptPasswordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private User validateUsernameAndEmail(String currentUsername, String newUsername, String email) throws
            UserExistedException, EmailExistedException, UserNotFoundException {
        if (StringUtils.isNotBlank(currentUsername)) {
            User currentUser = findUserByUsername(currentUsername);
            if (currentUser == null) {
                throw new UserNotFoundException("No User found by username: "+currentUsername);
            }
            User userByNewUsername = findUserByUsername(newUsername);
            if (userByNewUsername != null && currentUser.getId().equals(userByNewUsername.getId())) {
                throw new UserExistedException("Username already exists.");
            }
            User userByNewEmail = findUserByEmail(email);
            if (userByNewEmail != null && currentUser.getId().equals(userByNewEmail.getId())) {
                throw new EmailExistedException("Email already exists.");
            }
            return currentUser;
        } else {
            User userByUsername = findUserByUsername(newUsername);
            if (userByUsername != null) {
                throw new UserExistedException("Username already exists.");
            }
            User userByEmail = findUserByEmail(email);
            if (userByEmail != null) {
                throw new EmailExistedException("Email already exists.");
            }
            return null;
        }
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public User findUserByEmail(String email) {

        return userRepository.findUserByEmail(email);
    }
}
