package com.example.supportportal.exception.domain;

public class UserExistedException extends Exception{
    public UserExistedException(String message) {
        super(message);
    }
}
