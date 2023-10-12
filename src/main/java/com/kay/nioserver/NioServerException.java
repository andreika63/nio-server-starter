package com.kay.nioserver;

public class NioServerException extends RuntimeException {
    public NioServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NioServerException(String message) {
        super(message);
    }
}
