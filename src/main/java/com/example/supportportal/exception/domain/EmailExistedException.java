package com.example.supportportal.exception.domain;

public class EmailExistedException extends Exception{
    public EmailExistedException(String message) {
        super(message);
    }
}
