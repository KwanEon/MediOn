package com.example.medicalsearch.client;

public class PublicDataClientException extends RuntimeException {

    public PublicDataClientException(String message) {
        super(message);
    }

    public PublicDataClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
