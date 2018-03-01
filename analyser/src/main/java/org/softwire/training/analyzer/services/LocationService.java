package org.softwire.training.analyzer.services;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.model.Location;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class LocationService {
    private static final Logger LOG = LoggerFactory.getLogger(LocationService.class);

    private final TypedConfig config;
    private final AmazonS3 s3;
    private ObjectMapper objectMapper;

    public LocationService(TypedConfig config, AmazonS3 s3, ObjectMapper objectMapper) {
        this.config = config;
        this.s3 = s3;
        this.objectMapper = objectMapper;
    }

    public List<Location> get() throws IOException {
        String locationsJson = CharStreams.toString(new InputStreamReader(s3.getObject(config.s3Bucket, config.s3Key).getObjectContent()));
        LOG.debug("Read Locations JSON from S3: {}", locationsJson);
        return objectMapper.readValue(locationsJson, new TypeReference<List<Location>>() {});
    }

    public static class TypedConfig {
        final String s3Bucket;
        final String s3Key;

        public TypedConfig(String s3Bucket, String s3Key) {
            this.s3Bucket = s3Bucket;
            this.s3Key = s3Key;
        }

        public static TypedConfig fromUntypedConfig(Config config) {
            return new TypedConfig(config.getString("s3Bucket"), config.getString("s3Key"));
        }
    }
}
