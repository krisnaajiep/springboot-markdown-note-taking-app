package com.krisnaajiep.markdownnotetaking.validator;

import com.krisnaajiep.markdownnotetaking.controller.InvalidFileException;
import org.springframework.web.multipart.MultipartFile;

public abstract class FileValidator {
    public void validate(MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("File is null");
        }

        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (!isValidContentType(file.getContentType())) {
            throw new InvalidFileException("File content type must be text/markdown");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileException("Original filename is null or blank");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));

        if (!isValidExtension(extension)) {
            throw new InvalidFileException("Only markdown file extension (.md) is allowed");
        }
    }

    abstract boolean isValidContentType(String contentType);

    abstract boolean isValidExtension(String extension);
}
