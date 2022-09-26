package com.donphds.core.exception;

import org.springframework.boot.web.server.WebServerException;

public class InternalErrorException extends WebServerException {

    public InternalErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
