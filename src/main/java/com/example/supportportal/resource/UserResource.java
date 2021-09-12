package com.example.supportportal.resource;

import com.example.supportportal.domain.User;
import com.example.supportportal.exception.domain.EmailExistedException;
import com.example.supportportal.exception.domain.ExceptionHanding;
import com.example.supportportal.exception.domain.UserExistedException;
import com.example.supportportal.exception.domain.UserNotFoundException;
import com.example.supportportal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserResource extends ExceptionHanding {

    @Autowired
    private UserService userService;
    @GetMapping("/home")
    public String showUser() throws EmailExistedException {
        //return "application works";
        throw new EmailExistedException("This email address is already taken.");
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, UserExistedException, EmailExistedException {
        User signupUser = userService.register(user.getFirstName(),user.getLastName(),user.getUsername(),user.getEmail());
        return new ResponseEntity<>(signupUser, HttpStatus.OK);
    }
}
