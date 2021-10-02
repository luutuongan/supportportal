package com.example.supportportal.service;

import com.example.supportportal.domain.User;
import com.example.supportportal.exception.domain.EmailExistedException;
import com.example.supportportal.exception.domain.EmailNotFoundException;
import com.example.supportportal.exception.domain.UserExistedException;
import com.example.supportportal.exception.domain.UserNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    User register(String firstName, String lastName, String username, String email) throws UserExistedException, EmailExistedException, UserNotFoundException, MessagingException;

    List<User> getUsers();

    User findUserByUsername(String username);

    User findUserByEmail(String email);

    User addNewUser(String firstName, String lastName, String username, String email, String role, boolean isNonLocked,
                    boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UserExistedException, EmailExistedException, IOException;

    User updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername, String newEmail,
                    String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UserExistedException, EmailExistedException, IOException;

    void deleteUser(long id);

    void resetPassword(String email) throws EmailNotFoundException;

    User updateProfileImage(String username, MultipartFile profileImage) throws IOException, UserNotFoundException, UserExistedException, EmailExistedException;
}
