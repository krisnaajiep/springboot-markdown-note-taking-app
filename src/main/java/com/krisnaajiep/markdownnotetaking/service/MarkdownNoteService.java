package com.krisnaajiep.markdownnotetaking.service;

import com.krisnaajiep.markdownnotetaking.controller.ConflictException;
import com.krisnaajiep.markdownnotetaking.controller.NotFoundException;
import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.model.Note;
import com.krisnaajiep.markdownnotetaking.model.NoteRepository;
import com.krisnaajiep.markdownnotetaking.service.grammar.GrammarCheckService;
import com.krisnaajiep.markdownnotetaking.service.storage.StorageService;
import com.krisnaajiep.markdownnotetaking.validator.MarkdownFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkdownNoteService implements NoteService {
    private final NoteRepository noteRepository;
    private final StorageService storageService;
    private final MarkdownFileValidator fileValidator;
    private final GrammarCheckService grammarCheckService;

    @Override
    @Transactional
    public Note save(MultipartFile file) throws IOException {
        fileValidator.validate(file);

        String originalFilename = file.getOriginalFilename();
        if (noteRepository.existsByOriginalFilename(originalFilename)) {
            throw new ConflictException("File with original name '" + originalFilename + "' already exists");
        }

        UUID filename = UUID.randomUUID();
        InputStream inputStream = file.getInputStream();
        storageService.save(filename + ".md", inputStream);

        Note note = Note.builder()
                .filename(filename)
                .originalFilename(originalFilename)
                .build();

        return noteRepository.save(note);
    }

    @Override
    public GrammarCheckResponse check(String filename) throws IOException {
        Note note = noteRepository.findByOriginalFilename(filename)
                .orElseThrow(() -> new NotFoundException("File with original name '" + filename + "' not found"));

        String text = storageService.load(note.getFilename() + ".md");
        return grammarCheckService.check(text, "en-US");
    }

    @Override
    public void list() {

    }

    @Override
    public void render(String filename) {

    }
}
