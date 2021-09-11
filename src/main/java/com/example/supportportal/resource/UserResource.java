package com.example.supportportal.resource;

import com.example.supportportal.exception.domain.EmailExistedException;
import com.example.supportportal.exception.domain.ExceptionHanding;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserResource extends ExceptionHanding {

    @GetMapping("/home")
    public String showUser() throws EmailExistedException {
        //return "application works";
        throw new EmailExistedException("This email address is already taken.");
    }
}
