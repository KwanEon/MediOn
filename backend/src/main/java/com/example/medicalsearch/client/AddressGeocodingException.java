package com.example.medicalsearch.client;

public class AddressGeocodingException extends RuntimeException {

    public AddressGeocodingException(String message) {
        super(message);
    }

    public AddressGeocodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
