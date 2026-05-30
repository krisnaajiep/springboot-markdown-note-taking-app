package com.krisnaajiep.markdownnotetaking.validator;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class MarkdownFileValidator extends FileValidator{
    @Override
    boolean isValidContentType(String contentType) {
        return MediaType.TEXT_MARKDOWN_VALUE.equals(contentType);
    }

    @Override
    boolean isValidExtension(String extension) {
        return extension.equalsIgnoreCase(".md");
    }
}
