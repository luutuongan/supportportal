package com.example.supportportal.service;

import com.example.supportportal.domain.User;
import com.example.supportportal.exception.domain.EmailExistedException;
import com.example.supportportal.exception.domain.UserExistedException;
import com.example.supportportal.exception.domain.UserNotFoundException;

import java.util.List;

public interface UserService {

    User register(String firstName, String lastName, String username, String email) throws UserExistedException, EmailExistedException, UserNotFoundException;

    List<User> getUsers();

    User findUserByUsername(String username);

    User findUserByEmail(String email);
}
