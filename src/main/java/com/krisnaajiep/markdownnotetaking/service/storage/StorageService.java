package com.krisnaajiep.markdownnotetaking.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface StorageService {
    void save(String filename, InputStream inputStream) throws IOException;

    List<Path> list() throws IOException;

    String load(String filename) throws IOException;
}
