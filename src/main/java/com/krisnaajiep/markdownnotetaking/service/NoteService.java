package com.krisnaajiep.markdownnotetaking.service;

import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.model.Note;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface NoteService {
    Note save(MultipartFile file) throws IOException;

    GrammarCheckResponse check(String filename) throws IOException;

    List<Note> list() throws IOException;

    void render(String filename) throws IOException;
}
