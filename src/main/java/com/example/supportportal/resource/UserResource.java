package com.example.supportportal.resource;

import com.example.supportportal.domain.User;
import com.example.supportportal.domain.UserPrincipal;
import com.example.supportportal.exception.domain.EmailExistedException;
import com.example.supportportal.exception.domain.ExceptionHanding;
import com.example.supportportal.exception.domain.UserExistedException;
import com.example.supportportal.exception.domain.UserNotFoundException;
import com.example.supportportal.service.UserService;
import com.example.supportportal.util.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserResource extends ExceptionHanding {

    private UserService userService;

    private AuthenticationManager authenticationManager;

    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserResource(UserService userService, AuthenticationManager authenticationManager, JWTTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

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

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) throws UserNotFoundException, UserExistedException, EmailExistedException {
        authenticate(user.getPassword(),user.getUsername());
        User loginUser = userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeader, HttpStatus.OK);
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("JWT_TOKEN", jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authenticate(String password, String username) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username,password));
    }
}
