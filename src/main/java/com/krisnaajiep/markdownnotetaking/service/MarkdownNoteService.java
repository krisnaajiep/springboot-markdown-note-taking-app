package com.krisnaajiep.markdownnotetaking.service;

import com.krisnaajiep.markdownnotetaking.controller.ConflictException;
import com.krisnaajiep.markdownnotetaking.controller.NotFoundException;
import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.model.Note;
import com.krisnaajiep.markdownnotetaking.model.NoteRepository;
import com.krisnaajiep.markdownnotetaking.service.grammar.GrammarCheckService;
import com.krisnaajiep.markdownnotetaking.service.storage.StorageService;
import com.krisnaajiep.markdownnotetaking.validator.MarkdownFileValidator;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
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
    public List<Note> list() throws IOException {
        List<Path> pathList = storageService.list();
        return noteRepository.findAll().stream()
                .filter(note -> pathList.stream()
                        .anyMatch(path -> path.getFileName().toString().equals(note.getFilename() + ".md")))
                .toList();
    }

    @Override
    public String render(String filename) throws IOException {
        MutableDataSet options = new MutableDataSet();

        options.set(
                Parser.EXTENSIONS,
                Arrays.asList(
                        TablesExtension.create(),
                        StrikethroughExtension.create(),
                        TaskListExtension.create(),
                        AutolinkExtension.create()
                )
        );

        String content = storageService.load(filename);

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Document parse = parser.parse(content);
        return renderer.render(parse);
    }
}
