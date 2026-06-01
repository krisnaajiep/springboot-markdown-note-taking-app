package com.krisnaajiep.markdownnotetaking.controller;

import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.model.Note;
import com.krisnaajiep.markdownnotetaking.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Note> save(MultipartFile file) throws IOException {
        Note createdNote = noteService.save(file);
        return ResponseEntity.created(URI.create("/notes/" + createdNote.getId())).body(createdNote);
    }

    @GetMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GrammarCheckResponse> check(@RequestParam String filename) throws IOException {
        GrammarCheckResponse response = noteService.check(filename);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Note>> list() throws IOException {
        List<Note> noteList = noteService.list();
        return ResponseEntity.ok(noteList);
    }
}
