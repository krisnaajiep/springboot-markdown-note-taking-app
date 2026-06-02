package com.krisnaajiep.markdownnotetaking.controller;

import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.model.Note;
import com.krisnaajiep.markdownnotetaking.model.NoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@Transactional
class NoteControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private MockRestServiceServer mockServer;

    @Value("${app.storage.root-location}")
    private String fileLocation;

    private MockMultipartFile file;

    private URI uri;

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
                "# Introduction\n Iam a software engineer.".getBytes()
        );

        uri = UriComponentsBuilder.fromUriString("https://api.languagetool.org/v2/check")
                .queryParam("text", "# Introduction\n Iam a software engineer.")
                .queryParam("language", "en-US")
                .build().toUri();
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
                new TypeReference<>() {
                }
        );

        assertFalse(response.get("error").isBlank());
    }

    @ParameterizedTest
    @MethodSource("errorRestClient")
    void check_withExistingFileAndErrorRestClient_shouldReturn502(DefaultResponseCreator responseCreator) throws Exception {
        mockServer.expect(requestTo(uri)).andRespond(responseCreator);

        mockMvc.perform(multipart("/notes").file(file))
                .andExpect(status().isCreated());

        Note note = noteRepository.findByOriginalFilename(file.getOriginalFilename()).orElseThrow();

        MvcResult result = mockMvc.perform(get("/notes/check").param("filename", note.getOriginalFilename()))
                .andExpect(status().isBadGateway())
                .andReturn();

        Map<String, String> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertFalse(response.get("error").isBlank());

        mockServer.verify();
    }

    @Test
    void check_withExistingFileAndSuccessRestClient_shouldReturn200() throws Exception {
        mockServer.expect(requestTo(uri)).andRespond(withSuccess(getSuccessBody("/success-body.json"), MediaType.APPLICATION_JSON));

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

        mockServer.verify();
    }

    @Test
    void list_withAllMatchListOfPath_shouldReturn200WithAppropriateResponseSize() throws Exception {
        int count = 10;

        for (int i = 0; i < count; i++) {
            file = new MockMultipartFile(
                    "file",
                    i + "-test.md",
                    MediaType.TEXT_MARKDOWN_VALUE,
                    "content".getBytes()
            );
            mockMvc.perform(multipart("/notes").file(file))
                    .andExpect(status().isCreated());
        }

        MvcResult result = mockMvc.perform(get("/notes"))
                .andExpect(status().isOk())
                .andReturn();

        List<Note> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertFalse(response.isEmpty());
        assertEquals(count, response.size());
    }

    @Test
    void render_withNonExistingFile_shouldReturn404() throws Exception {
        MvcResult result = mockMvc.perform(get("/notes/render").param("filename", "nonexistent.md"))
                .andExpect(status().isNotFound())
                .andReturn();

        Map<String, String> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        assertFalse(response.get("error").isBlank());
    }

    @Test
    void render_withExistingFileContainMarkdownHeading_shouldReturn200WAndContainHtmlHeading() throws Exception {
        mockMvc.perform(multipart("/notes").file(file))
                .andExpect(status().isCreated());

        try (Stream<Path> walk = Files.walk(Path.of(fileLocation))) {
            List<Path> pathList = walk.filter(Files::isRegularFile).toList();
            Path first = pathList.getFirst().getFileName();

            MvcResult result = mockMvc.perform(get("/notes/render").param("filename", first.toString()))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            assertTrue(content.contains("<h1>Introduction</h1>"));
        }
    }

    static Stream<Arguments> invalidFile() {
        return Stream.of(
                Arguments.argumentSet("Empty File", new MockMultipartFile("file", new byte[0])),
                Arguments.argumentSet("application/json", new MockMultipartFile("file", "test.json", MediaType.APPLICATION_JSON_VALUE, new byte[1])),
                Arguments.argumentSet("PDF extension", new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[1]))
        );
    }

    static Stream<Arguments> errorRestClient() {
        return Stream.of(
                Arguments.argumentSet("Bad request", withBadRequest()),
                Arguments.argumentSet("Invalid content type", withSuccess("Invalid content type", MediaType.TEXT_PLAIN)),
                Arguments.argumentSet("Mismatch response body", withSuccess(getSuccessBody("/mismatched-success-body.json"), MediaType.APPLICATION_JSON)),
                Arguments.argumentSet("Empty response body", withSuccess(new byte[0], MediaType.APPLICATION_JSON)),
                Arguments.argumentSet("Null response body", withSuccess().contentType(MediaType.APPLICATION_JSON))
        );
    }

    private static byte[] getSuccessBody(String name) {
        try (InputStream is = NoteControllerIT.class.getResourceAsStream(name)) {
            if (is == null) {
                throw new Exception("Failed to read test resource");
            }

            return is.readAllBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}