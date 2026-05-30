package com.krisnaajiep.markdownnotetaking.controller;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
