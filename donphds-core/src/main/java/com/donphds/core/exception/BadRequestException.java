package com.donphds.core.exception;

import org.springframework.web.reactive.function.client.WebClientException;

public class BadRequestException extends WebClientException {

    public BadRequestException(String msg) {
        super(msg);
    }

    public BadRequestException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
