package io.listen.exception;

import io.listen.enums.ResponseStatusEnum;

public class ServiceException extends RuntimeException{

    private final String message;

    public ServiceException(String message) {
        super(message);
        this.message = message;
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
