package com.donphds.core.exception;

import org.springframework.web.reactive.function.client.WebClientException;

public class NotFoundException extends WebClientException {

    public NotFoundException(String msg) {
        super(msg);
    }

    public NotFoundException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
