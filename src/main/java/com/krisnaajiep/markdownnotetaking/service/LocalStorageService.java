package com.krisnaajiep.markdownnotetaking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class LocalStorageService implements StorageService {
    private final Path rootLocation;

    public LocalStorageService(
            @Value("${app.storage.root-location}")
            String rootLocation
    ) throws IOException {
        this.rootLocation = Path.of(rootLocation);

        if (!Files.exists(this.rootLocation)) {
            Files.createDirectory(this.rootLocation);
        }
    }

    @Override
    public void save(String filename, InputStream inputStream) throws IOException {
        Path destinationFile = this.rootLocation.resolve(Path.of(filename))
                .normalize()
                .toAbsolutePath();

        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new IOException("Cannot store file outside current directory.");
        }

        Files.copy(inputStream, destinationFile);
    }

    @Override
    public List<Path> list() throws IOException {
        try (Stream<Path> walk = Files.walk(this.rootLocation)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }

    @Override
    public String load(String filename) throws IOException {
        return Files.readString(this.rootLocation.resolve(filename));
    }
}
