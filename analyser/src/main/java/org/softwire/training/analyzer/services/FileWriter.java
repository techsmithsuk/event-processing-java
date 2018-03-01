package org.softwire.training.analyzer.services;

import com.google.common.base.Charsets;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.model.Average;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileWriter {
    private static final Logger LOG = LoggerFactory.getLogger(FileWriter.class);
    private static final OpenOption[] FILE_OPEN_OPTIONS = {StandardOpenOption.CREATE, StandardOpenOption.APPEND};

    private final Path path;

    public FileWriter(TypedConfig config) throws IOException {
        path = Paths.get(config.filename).toAbsolutePath();
        Files.write(path, "\n\nStarting new analysis\n".getBytes(Charsets.UTF_8), FILE_OPEN_OPTIONS);
        LOG.info("Will be writing output to: {}", path);
    }

    public void write(Average average) {
        try {
            LOG.info("Wrote average to file: {}", average);
            Files.write(path, (average.toString() + "\n").getBytes(Charsets.UTF_8), FILE_OPEN_OPTIONS);
        } catch (IOException e) {
            // Will be called from inside a Lambda, so we need the unchecked version
            //
            // Arguably this shouldn't cause the application to die, as there might be issues writing to the file
            // because someone is messing with it at just the wrong time.
            throw new UncheckedIOException(e);
        }
    }

    public static class TypedConfig {
        final String filename;

        TypedConfig(String filename) {
            this.filename = filename;
        }

        public static TypedConfig fromUntypedConfig(Config config) {
            return new TypedConfig(config.getString("filename"));
        }
    }
}
