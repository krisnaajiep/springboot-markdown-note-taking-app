package com.krisnaajiep.markdownnotetaking.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {
    @TempDir
    private Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() throws IOException {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void save_withInvalidDestination_shouldThrowIOException() {
        MockMultipartFile file = new MockMultipartFile("../test.md", "Hello, World!".getBytes());

        assertThrows(IOException.class, () -> storageService.save(file.getName(), file.getInputStream()));
    }

    @Test
    void save_withExistingFilename_shouldThrowFileAlreadyExistsException() throws IOException {
        MockMultipartFile file = new MockMultipartFile("test.md", "Hello, World!".getBytes());
        storageService.save(file.getName(), file.getInputStream());

        assertThrows(FileAlreadyExistsException.class, () -> storageService.save(file.getName(), file.getInputStream()));
    }

    @Test
    void save_withValidDestination_shouldSaveFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile("test.md", "Hello, World!".getBytes());
        storageService.save(file.getName(), file.getInputStream());

        assertTrue(Files.exists(tempDir.resolve("test.md")));
    }

    @Test
    void list_withNoFiles_shouldReturnEmptyList() throws IOException {
        List<Path> result = storageService.list();
        assertTrue(result.isEmpty());
    }

    @Test
    void list_withExistingFiles_shouldReturnListOfFilePaths() throws IOException {
        List<MockMultipartFile> mockMultipartFiles = List.of(
                new MockMultipartFile("test1.md", "Content 1".getBytes()),
                new MockMultipartFile("test2.md", "Content 2".getBytes()),
                new MockMultipartFile("test3.md", "Content 3".getBytes())
        );

        for (MockMultipartFile mockMultipartFile : mockMultipartFiles) {
            storageService.save(mockMultipartFile.getName(), mockMultipartFile.getInputStream());
        }

        List<Path> result = storageService.list();

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(path -> path.getFileName().toString().equals("test1.md")));
        assertTrue(result.stream().anyMatch(path -> path.getFileName().toString().equals("test2.md")));
        assertTrue(result.stream().anyMatch(path -> path.getFileName().toString().equals("test3.md")));
    }

    @Test
    void load_withNoFile_shouldThrowNoSuchFileException() {
        assertThrows(NoSuchFileException.class, () -> storageService.load("test.md"));
    }

    @Test
    void load_withExistingFile_shouldReturnFileContentAsString() throws IOException {
        String content = "Hello, World!";
        MockMultipartFile file = new MockMultipartFile("test.md", content.getBytes());
        storageService.save(file.getName(), file.getInputStream());

        String test = storageService.load("test.md");

        assertEquals(content, test);
    }
}