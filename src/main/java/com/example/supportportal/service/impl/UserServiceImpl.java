package com.example.supportportal.service.impl;

import com.example.supportportal.constant.FileConstant;
import com.example.supportportal.constant.UserImplConstant;
import com.example.supportportal.domain.User;
import com.example.supportportal.domain.UserPrincipal;
import com.example.supportportal.enumeration.Role;
import com.example.supportportal.exception.domain.EmailExistedException;
import com.example.supportportal.exception.domain.EmailNotFoundException;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
        user.setAuthorities(Role.ROLE_USER.getRoleAuthorities());
        user.setProfileImageUrl(getTempProfileImageUrl(username));
        userRepository.save(user);
        LOGGER.info("New user password: " + password);
        //emailService.sendNewPasswordEmail(firstName, password, email);
        return user;
    }

    private String getTempProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(FileConstant.DEFAULT_USER_IMAGE_PATH + username)
                .toUriString();
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

    @Override
    public User addNewUser(String firstName, String lastName, String username, String email, String role,
                           boolean isNonLocked, boolean isActive, MultipartFile profileImage)
            throws UserNotFoundException, UserExistedException, EmailExistedException, IOException {
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
        user.setRole(getRoleEnumName(role).name());
        user.setNonLocked(isNonLocked);
        user.setActive(isActive);
        user.setProfileImageUrl(getTempProfileImageUrl(username));
        userRepository.save(user);
        saveProfileImage(user, profileImage);
        return user;
    }

    private String setUserProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(FileConstant.USER_IMAGE_PATH + username
                + FileConstant.FWD_SLASH + username + FileConstant.DOT + FileConstant.JPG_EXTENSTION).toUriString();
    }

    private void saveProfileImage(User user, MultipartFile profileImage) throws IOException {
        if (profileImage != null) {
            Path userFolder = Paths.get(FileConstant.USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
            if (!Files.exists(userFolder)) {
                Files.createDirectories(userFolder);
                LOGGER.info(FileConstant.DIR_CREATED + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + user.getUsername() + FileConstant.DOT
                    + FileConstant.JPG_EXTENSTION));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUsername() + FileConstant.DOT
                    + FileConstant.JPG_EXTENSTION), REPLACE_EXISTING);
            user.setProfileImageUrl(setUserProfileImageUrl(user.getUsername()));
            userRepository.save(user);
            LOGGER.info(FileConstant.FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
        }
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    @Override
    public User updateUser(String currentUsername, String firstName, String lastName, String newUsername,
                           String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)
            throws UserNotFoundException, UserExistedException, EmailExistedException, IOException {
        User user = validateUsernameAndEmail(currentUsername, newUsername, email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(newUsername);
        if (email != null) {
            user.setEmail(email);
        }
        user.setJoinDate(new Date());
        user.setRole(getRoleEnumName(role).name());
        user.setAuthorities(getRoleEnumName(role).getRoleAuthorities());
        user.setNonLocked(isNonLocked);
        user.setActive(isActive);
        userRepository.save(user);
        saveProfileImage(user, profileImage);
        return user;
    }

    @Override
    public void deleteUser(long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void resetPassword(String email) throws EmailNotFoundException {
        User user = userRepository.findUserByEmail(email);
        if (user == null) {
            throw new EmailNotFoundException(UserImplConstant.NO_USER_FOUND_BY_EMAIL);
        }
        String password = generatePassword();
        user.setPassword(encodePassword(password));
        userRepository.save(user);
    }

    @Override
    public User updateProfileImage(String username, MultipartFile profileImage)
            throws IOException, UserNotFoundException, UserExistedException, EmailExistedException {
        User user = validateUsernameAndEmail(username, null, null);
        saveProfileImage(user, profileImage);
        return user;
    }


}
