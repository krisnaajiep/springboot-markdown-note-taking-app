package com.krisnaajiep.markdownnotetaking.controller;

public class BadGatewayException extends RuntimeException {
    public BadGatewayException(String message) {
        super(message);
    }

    public BadGatewayException() {
        super("An error occurred while communicating with an external service.");
    }
}
