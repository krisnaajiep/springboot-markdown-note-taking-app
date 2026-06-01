package com.krisnaajiep.markdownnotetaking.controller;

import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.model.Note;
import com.krisnaajiep.markdownnotetaking.model.NoteRepository;
import com.krisnaajiep.markdownnotetaking.service.GrammarCheckService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoteControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoteRepository noteRepository;

    @MockitoBean
    private GrammarCheckService grammarCheckService;

    private GrammarCheckResponse grammarCheckResponse;

    @Value("${app.storage.root-location}")
    private String fileLocation;

    private MockMultipartFile file;

    @BeforeEach
    void setUp() throws IOException {
        if (!Path.of(fileLocation).equals(Path.of("uploads"))) {
            throw new IllegalStateException("File location must be 'upload' for testing purposes");
        }

        try (Stream<Path> walk = Files.walk(Path.of(fileLocation))) {
            for (Path path : walk.toList()) {
                if (Files.isRegularFile(path)) {
                    Files.delete(path);
                }
            }
        }

        file = new MockMultipartFile(
                "file",
                "test.md",
                MediaType.TEXT_MARKDOWN_VALUE,
                "# Introduction\n Iam a software engineer".getBytes()
        );

        grammarCheckResponse = GrammarCheckResponse.builder()
                .software("LanguageTool")
                .language("English")
                .results(Stream.of(
                        GrammarCheckResponse.Result.builder()
                                .message("Possible spelling mistake found")
                                .suggestions(Stream.of("I am").toList())
                                .offset(17)
                                .length(3)
                                .context(Map.of("text", "Iam a software engineer", "offset", 0, "length", 24))
                                .build()
                ).toList())
                .build();
    }

    @AfterEach
    void tearDown() {
    }

    @ParameterizedTest
    @MethodSource("invalidFile")
    void save_withInvalidFile_shouldReturn400(MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/notes").file(file))
                .andExpectAll(status().isBadRequest())
                .andReturn();

        Map<String, String> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertFalse(response.get("error").isBlank());
        assertTrue(noteRepository.findAll().isEmpty());

        try (Stream<Path> walk = Files.walk(Path.of(fileLocation))) {
            assertTrue(walk.noneMatch(Files::isRegularFile));
        }
    }

    @Test
    void save_withValidFileAndDuplicateOriginalName_shouldReturn409() throws Exception {
        mockMvc.perform(multipart("/notes").file(file))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(multipart("/notes").file(file))
                .andExpectAll(status().isConflict())
                .andReturn();

        Map<String, String> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertFalse(response.get("error").isBlank());
        assertEquals(1, noteRepository.findAll().size());

        try (Stream<Path> walk = Files.walk(Path.of(fileLocation))) {
            assertEquals(1, walk.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void save_withValidFileAndUniqueOriginalFilename_shouldReturn201() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/notes").file(file))
                .andExpect(status().isCreated())
                .andReturn();

        Note note = objectMapper.readValue(result.getResponse().getContentAsString(), Note.class);

        assertNotNull(note.getId());
        assertNotNull(note.getFilename());
        assertEquals("test.md", note.getOriginalFilename());
        assertNotNull(note.getCreatedAt());

        assertNotNull(noteRepository.findById(note.getId()));
        assertTrue(Files.exists(Path.of(fileLocation + "/" + note.getFilename() + ".md")));
    }

    @Test
    void check_withNoExistingFile_shouldReturn404() throws Exception {
        MvcResult result = mockMvc.perform(get("/notes/check").param("filename", "nonexistent.md"))
                .andExpect(status().isNotFound())
                .andReturn();

        Map<String, String> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        assertFalse(response.get("error").isBlank());
    }

    @Test
    void check_withExistingFileAndErrorCheckApi_shouldReturn502() throws Exception {
        when(grammarCheckService.check(anyString(), anyString())).thenThrow(new BadGatewayException("LanguageTool API is currently unavailable."));

        mockMvc.perform(multipart("/notes").file(file))
                .andExpect(status().isCreated());

        Note note = noteRepository.findByOriginalFilename(file.getOriginalFilename()).orElseThrow();

        MvcResult result = mockMvc.perform(get("/notes/check").param("filename", note.getOriginalFilename()))
                .andExpect(status().isBadGateway())
                .andReturn();

        Map<String, String> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        assertFalse(response.get("error").isBlank());

        verify(grammarCheckService, times(1)).check(anyString(), anyString());
        verifyNoMoreInteractions(grammarCheckService);
    }

    @Test
    void check_withExistingFile_shouldReturn200() throws Exception {
        when(grammarCheckService.check(anyString(), anyString())).thenReturn(grammarCheckResponse);

        mockMvc.perform(multipart("/notes").file(file))
                .andExpect(status().isCreated());

        Note note = noteRepository.findByOriginalFilename(file.getOriginalFilename()).orElseThrow();

        MvcResult result = mockMvc.perform(get("/notes/check").param("filename", note.getOriginalFilename()))
                .andExpect(status().isOk())
                .andReturn();

        GrammarCheckResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GrammarCheckResponse.class
        );

        assertNotNull(response.getSoftware());
        assertNotNull(response.getLanguage());
        assertNotNull(response.getResults());

        verify(grammarCheckService, times(1)).check(anyString(), anyString());
        verifyNoMoreInteractions(grammarCheckService);
    }

    static Stream<Arguments> invalidFile() {
        return Stream.of(
                Arguments.argumentSet("Empty File", new MockMultipartFile("file", new byte[0])),
                Arguments.argumentSet("application/json", new MockMultipartFile("file", "test.json", MediaType.APPLICATION_JSON_VALUE, new byte[1])),
                Arguments.argumentSet("PDF extension", new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[1]))
        );
    }
}