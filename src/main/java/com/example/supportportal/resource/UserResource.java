package com.example.supportportal.resource;

import exception.domain.ExceptionHanding;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserResource extends ExceptionHanding {

    @GetMapping("/home")
    public String showUser() {
        return "application works";
    }
}
