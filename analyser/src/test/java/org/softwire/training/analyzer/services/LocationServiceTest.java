package org.softwire.training.analyzer.services;

import com.amazonaws.http.apache.request.impl.HttpGetWithBody;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.builders.LocationBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationServiceTest {

    private static final String S3_BUCKET = "s3bucket";
    private static final String S3_KEY = "s3key";
    private static final LocationService.TypedConfig CONFIG = new LocationService.TypedConfig(S3_BUCKET, S3_KEY);

    private LocationService locationService;
    private AmazonS3 s3;

    @BeforeEach
    void beforeEach() {
        s3 = mock(AmazonS3.class);
        locationService = new LocationService(CONFIG, s3, new ObjectMapper());
    }

    @Test
    void parseEmptyLocations() throws IOException {
        when(s3.getObject(S3_BUCKET, S3_KEY)).thenReturn(buildS3Object("[]"));

        assertThat(locationService.get(), empty());
    }

    @Test
    void parseLocations() throws IOException {
        String locations = "[ " +
                "   {\"x\": \"1.1\", \"y\": \"1.2\", \"id\": \"c9780439-3912-480e-91da-c85a72c8434c\"}, " +
                "   {\"x\": \"2.1\", \"y\": \"2.2\", \"id\": \"58c944c3-172a-4397-9b56-2b9b41f83872\"}" +
                " ]";
        when(s3.getObject(S3_BUCKET, S3_KEY)).thenReturn(buildS3Object(locations));

        assertThat(locationService.get(), contains(
                new LocationBuilder().setX(1.1f).setY(1.2f).setId(UUID.fromString("c9780439-3912-480e-91da-c85a72c8434c")).createLocation(),
                new LocationBuilder().setX(2.1f).setY(2.2f).setId(UUID.fromString("58c944c3-172a-4397-9b56-2b9b41f83872")).createLocation()
        ));
    }

    @Test
    void throwsIOExceptionOnInvalidJson() {
        String invalid = "[ oh dear ]";
        when(s3.getObject(S3_BUCKET, S3_KEY)).thenReturn(buildS3Object(invalid));

        assertThrows(IOException.class, () -> locationService.get());
    }

    private S3Object buildS3Object(String content) {
        S3Object s3object = new S3Object();
        S3ObjectInputStream objectContent = new S3ObjectInputStream(
                new ByteArrayInputStream(content.getBytes(Charsets.UTF_8)),
                mock(HttpGetWithBody.class));
        s3object.setObjectContent(objectContent);
        return s3object;
    }
}