package com.example.medicalsearch.client;

public class OpenStreetMapClientException extends RuntimeException {

    public OpenStreetMapClientException(String message) {
        super(message);
    }

    public OpenStreetMapClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
