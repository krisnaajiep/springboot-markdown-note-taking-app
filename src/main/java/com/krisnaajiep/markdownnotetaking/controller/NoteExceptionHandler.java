package com.krisnaajiep.markdownnotetaking.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class NoteExceptionHandler {
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Object> handleInvalidFileException(InvalidFileException ex) {
        log.warn("Invalid file error occurred: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                Map.of("error", ex.getMessage())
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflictException(ConflictException ex) {
        log.warn("Conflict error occurred: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("error", ex.getMessage())
        );
    }
}
