package com.example.supportportal.exception.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHanding {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
}
