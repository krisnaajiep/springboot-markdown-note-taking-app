package com.krisnaajiep.markdownnotetaking.service;

import com.krisnaajiep.markdownnotetaking.controller.ConflictException;
import com.krisnaajiep.markdownnotetaking.controller.InvalidFileException;
import com.krisnaajiep.markdownnotetaking.model.Note;
import com.krisnaajiep.markdownnotetaking.model.NoteRepository;
import com.krisnaajiep.markdownnotetaking.validator.MarkdownFileValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkdownNoteServiceTest {
    @Mock
    private NoteRepository noteRepository;

    @TempDir
    private Path tempDir;

    private MarkdownNoteService noteService;

    private Note note;
    private MultipartFile file;

    @BeforeEach
    void setUp() throws IOException {
        note = Note.builder().id(1L).filename(UUID.randomUUID()).originalFilename("test.md").build();

        file = new MockMultipartFile(
                "file",
                note.getOriginalFilename(),
                MediaType.TEXT_MARKDOWN_VALUE,
                "content".getBytes(StandardCharsets.UTF_8)
        );

        noteService = new MarkdownNoteService(noteRepository, new LocalStorageService(tempDir.toString()), new MarkdownFileValidator());
    }

    @AfterEach
    void tearDown() {
    }

    @ParameterizedTest
    @MethodSource("invalidFile")
    void save_withInvalidFile_shouldThrowInvalidFileException(MultipartFile file) {
        assertThrows(InvalidFileException.class, () -> noteService.save(file));
    }

    @Test
    void save_withValidFileAndDuplicateOriginalName_shouldThrowConflictException() {
        when(noteRepository.existsByOriginalFilename(anyString())).thenReturn(true);

        assertThrows(ConflictException.class, () -> noteService.save(file));

        verify(noteRepository, times(1)).existsByOriginalFilename(anyString());
        verifyNoMoreInteractions(noteRepository);
    }

    @Test
    void save_withValidFileAndUniqueOriginalName_shouldReturnSavedNote() throws IOException {
        when(noteRepository.existsByOriginalFilename(anyString())).thenReturn(false);
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        Note savedNote = noteService.save(file);

        try (Stream<Path> walk = Files.walk(tempDir)) {
            assertFalse(walk.toList().isEmpty());
        }

        assertEquals(savedNote, note);

        verify(noteRepository, times(1)).existsByOriginalFilename(anyString());
        verify(noteRepository, times(1)).save(any(Note.class));
        verifyNoMoreInteractions(noteRepository);
    }

    static Stream<Arguments> invalidFile() {
        return Stream.of(
                Arguments.argumentSet("Null", (Object) null),
                Arguments.argumentSet("Empty File", new MockMultipartFile("file", new byte[0])),
                Arguments.argumentSet("application/json", new MockMultipartFile("file", "test.json", MediaType.APPLICATION_JSON_VALUE, new byte[1])),
                Arguments.argumentSet("PDF extension", new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[1]))
        );
    }
}