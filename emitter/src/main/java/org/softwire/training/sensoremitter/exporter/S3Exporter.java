package org.softwire.training.sensoremitter.exporter;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.model.Location;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;

public class S3Exporter implements Exporter {
    private final ApplicationConfig config;
    private final AmazonS3 s3;
    private ObjectMapper objectMapper;

    @Inject
    S3Exporter(ApplicationConfig config, AmazonS3 s3, ObjectMapper objectMapper) {
        this.config = config;
        this.s3 = s3;
        this.objectMapper = objectMapper;
    }

    public void writeOut(Collection<Location> locations) throws IOException {
        byte[] json = objectMapper.writeValueAsString(locations).getBytes(Charsets.UTF_8);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/json");
        metadata.setContentEncoding(Charsets.UTF_8.displayName());
        metadata.setContentLength(json.length);

        s3.putObject(new PutObjectRequest(config.s3Bucket, config.s3Key, new ByteArrayInputStream(json), metadata));
    }
}
