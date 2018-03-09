package org.softwire.training.analyzer.services;

import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileWriter<T> {
    private static final Logger LOG = LoggerFactory.getLogger(FileWriter.class);
    private static final OpenOption[] FILE_OPEN_OPTIONS = {StandardOpenOption.CREATE, StandardOpenOption.APPEND};

    private final Path path;
    private final Marshaller<T> marshaller;

    FileWriter(String filename, Marshaller<T> marshaller) throws IOException {
        path = Paths.get(filename).toAbsolutePath();
        this.marshaller = marshaller;
        Files.write(path, "\n\nStarting new analysis\n".getBytes(Charsets.UTF_8), FILE_OPEN_OPTIONS);
    }

    public void write(T element) {
        try {
            LOG.info("Wrote details to file: {}", element);
            Files.write(path, (marshaller.marshall(element) + "\n").getBytes(Charsets.UTF_8), FILE_OPEN_OPTIONS);
        } catch (IOException e) {
            // Will be called from inside a Lambda, so we need the unchecked version
            //
            // Arguably this shouldn't cause the application to die, as there might be issues writing to the file
            // because someone is messing with it at just the wrong time.
            throw new UncheckedIOException(e);
        }
    }

    public static class Factory {
        public <T> FileWriter<T> get(String filename, Marshaller<T> marshaller) throws IOException {
            return new FileWriter<>(filename, marshaller);
        }
    }
}
