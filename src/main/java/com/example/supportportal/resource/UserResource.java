package com.example.supportportal.resource;

import com.example.supportportal.constant.FileConstant;
import com.example.supportportal.domain.HttpResponse;
import com.example.supportportal.domain.User;
import com.example.supportportal.domain.UserPrincipal;
import com.example.supportportal.exception.domain.*;
import com.example.supportportal.service.UserService;
import com.example.supportportal.util.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;


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
    public ResponseEntity<User> register(
            @RequestBody User user)
            throws UserNotFoundException, UserExistedException, EmailExistedException, MessagingException {
        User signupUser = userService.register(user.getFirstName(),user.getLastName(),user.getUsername(),user.getEmail());
        return new ResponseEntity<>(signupUser, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<User> addNewUser(@RequestParam String firstName,
                                           @RequestParam String lastName,
                                           @RequestParam String username,
                                           @RequestParam String email,
                                           @RequestParam String role,
                                           @RequestParam String isActive,
                                           @RequestParam String isNonLocked,
                                           @RequestParam(required = false) MultipartFile profileImage)
            throws UserNotFoundException, UserExistedException, IOException, EmailExistedException {
        User user = userService.addNewUser(firstName, lastName, username, email, role, Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive),profileImage);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<User> updateUser(@RequestParam String firstName,
                                           @RequestParam String lastName,
                                           @RequestParam String currentUsername,
                                           @RequestParam String newUsername,
                                           @RequestParam(required = false) String email,
                                           @RequestParam String role,
                                           @RequestParam String isActive,
                                           @RequestParam String isNonLocked,
                                           @RequestParam(required = false) MultipartFile profileImage)
            throws UserNotFoundException, UserExistedException, IOException, EmailExistedException {
        User user = userService.updateUser(currentUsername, firstName, lastName, newUsername, email, role,
                Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive),profileImage);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = userService.getUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/find/{username}")
    public ResponseEntity<User> findUser(
            @PathVariable String username) {
        User user = userService.findUserByUsername(username);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(
            @PathVariable long id) {
        userService.deleteUser(id);
        return response(HttpStatus.OK, "Deleted user id: " + id);
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<User> updateProfileImage(
            @RequestParam String username,
            @RequestParam MultipartFile profileImage) throws UserNotFoundException, UserExistedException, IOException, EmailExistedException {
        User user = userService.updateProfileImage(username, profileImage);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping(value = "image/{username}/{filename}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(
            @PathVariable String username,
            @PathVariable String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(FileConstant.USER_FOLDER + username + FileConstant.FWD_SLASH + fileName));
    }

    @GetMapping(value = "image/{profile}/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage(
            @PathVariable String username) throws IOException {
        URL url = new URL(FileConstant.TEMP_PROFILE_IMAGE_BASE_URL + username);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = url.openStream()) {
            int bytesRead;
            byte[] chunk = new byte[1024];
            while((bytesRead = inputStream.read(chunk)) > 0) {
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    @GetMapping("/reset/{email}")
    public ResponseEntity<HttpResponse> resetPassword(
            @PathVariable String email) throws EmailNotFoundException {
        userService.resetPassword(email);
        return response(HttpStatus.OK, "Password sent to email: " + email);
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        return new ResponseEntity<>(new HttpResponse(status.value(), status, status.getReasonPhrase().toUpperCase(), message), status);
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(
            @RequestBody User user) throws UserNotFoundException, UserExistedException, EmailExistedException {
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
