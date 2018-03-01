package org.softwire.training.sensoremitter.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.time.Instant;
import java.util.stream.Stream;

public class JsonMarshaller<T> implements Pipeline<T, String> {
    private final ObjectMapper objectMapper;

    @Inject
    JsonMarshaller(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Stream<String> handle(Instant now, T element) {
        try {
            return Stream.of(objectMapper.writeValueAsString(element));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
