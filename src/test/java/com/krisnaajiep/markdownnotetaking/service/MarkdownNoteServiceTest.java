package com.krisnaajiep.markdownnotetaking.service;

import com.krisnaajiep.markdownnotetaking.controller.BadGatewayException;
import com.krisnaajiep.markdownnotetaking.controller.ConflictException;
import com.krisnaajiep.markdownnotetaking.controller.InvalidFileException;
import com.krisnaajiep.markdownnotetaking.controller.NotFoundException;
import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.model.Note;
import com.krisnaajiep.markdownnotetaking.model.NoteRepository;
import com.krisnaajiep.markdownnotetaking.service.grammar.GrammarCheckService;
import com.krisnaajiep.markdownnotetaking.service.storage.LocalStorageService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkdownNoteServiceTest {
    @Mock
    private NoteRepository noteRepository;

    @Mock
    private GrammarCheckService grammarCheckService;

    @TempDir
    private Path tempDir;

    private MarkdownNoteService noteService;

    @Mock
    private LocalStorageService storageService;

    private MarkdownNoteService noteServiceWithMockStorageService;

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

        noteService = new MarkdownNoteService(
                noteRepository,
                new LocalStorageService(tempDir.toString()),
                new MarkdownFileValidator(),
                grammarCheckService
        );

        noteServiceWithMockStorageService = new MarkdownNoteService(
                noteRepository,
                storageService,
                new MarkdownFileValidator(),
                grammarCheckService
        );
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

    @Test
    void check_withNoExistingFile_shouldThrowNotFoundException() {
        when(noteRepository.findByOriginalFilename(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> noteService.check("nonexistent.md"));

        verify(noteRepository, times(1)).findByOriginalFilename(anyString());
        verifyNoMoreInteractions(noteRepository);
        verifyNoInteractions(grammarCheckService);
    }

    @Test
    void check_withExistingFileAndErrorCheckApi_shouldThrowBadGatewayException() throws IOException {
        when(noteRepository.findByOriginalFilename(anyString())).thenReturn(Optional.of(note));
        when(storageService.load(anyString())).thenReturn("content");
        when(grammarCheckService.check(anyString(), anyString())).thenThrow(new BadGatewayException("LanguageTool API is currently unavailable."));

        assertThrows(BadGatewayException.class, () -> noteServiceWithMockStorageService.check(note.getOriginalFilename()));

        verify(noteRepository, times(1)).findByOriginalFilename(anyString());
        verify(storageService, times(1)).load(anyString());
        verify(grammarCheckService, times(1)).check(anyString(), anyString());
        verifyNoMoreInteractions(noteRepository, storageService, grammarCheckService);

    }

    @Test
    void check_withExistingFile_shouldReturnGrammarCheckResponse() throws IOException {
        GrammarCheckResponse mockResponse = GrammarCheckResponse.builder().build();
        when(noteRepository.findByOriginalFilename(anyString())).thenReturn(Optional.of(note));
        when(storageService.load(anyString())).thenReturn("content");
        when(grammarCheckService.check(anyString(), anyString())).thenReturn(mockResponse);

        GrammarCheckResponse response = noteServiceWithMockStorageService.check(note.getOriginalFilename());
        assertEquals(mockResponse, response);

        verify(noteRepository, times(1)).findByOriginalFilename(anyString());
        verify(storageService, times(1)).load(anyString());
        verify(grammarCheckService, times(1)).check(anyString(), anyString());
        verifyNoMoreInteractions(noteRepository, storageService, grammarCheckService);
    }

    @Test
    void list_withUnmatchFilenameInListOfPath_shouldReturnEmptyList() throws IOException {
        when(noteRepository.findAll()).thenReturn(List.of(note));
        when(storageService.list()).thenReturn(List.of(tempDir.resolve("unmatched.md")));

        List<Note> result = noteServiceWithMockStorageService.list();

        assertTrue(result.isEmpty());

        verify(noteRepository, times(1)).findAll();
        verify(storageService, times(1)).list();
        verifyNoMoreInteractions(noteRepository, storageService);
    }

    @Test
    void list_withMatchFilenameInListOfPath_shouldReturnListOfNotes() throws IOException {
        when(noteRepository.findAll()).thenReturn(List.of(note));
        when(storageService.list()).thenReturn(List.of(tempDir.resolve(note.getFilename() + ".md")));

        List<Note> result = noteServiceWithMockStorageService.list();

        assertEquals(1, result.size());
        assertEquals(note, result.getFirst());

        verify(noteRepository, times(1)).findAll();
        verify(storageService, times(1)).list();
        verifyNoMoreInteractions(noteRepository, storageService);
    }

    @Test
    void render_withFlatContent_shouldReturnStringContainHtmlParagraphHeading() throws IOException {
        when(storageService.load(anyString())).thenReturn("Hello, World!");

        String render = noteServiceWithMockStorageService.render("test.md");
        assertTrue(render.contains("<p>") && render.contains("</p>"));

        verify(storageService, times(1)).load(anyString());
        verifyNoMoreInteractions(storageService);
    }

    @Test
    void render_withMarkdownHeading_shouldReturnStringContainHtmlHeading() throws IOException {
        when(storageService.load(anyString())).thenReturn("# Intro");

        String render = noteServiceWithMockStorageService.render("test.md");
        assertTrue(render.contains("<h1>") && render.contains("</h1>"));

        verify(storageService, times(1)).load("test.md");
        verifyNoMoreInteractions(storageService);
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