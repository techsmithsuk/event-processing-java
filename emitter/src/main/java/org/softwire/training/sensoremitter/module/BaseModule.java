package org.softwire.training.sensoremitter.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.softwire.training.sensoremitter.application.ApplicationConfig;

import java.time.Clock;
import java.util.Random;

class BaseModule extends AbstractModule {

    private final ApplicationConfig config;

    BaseModule(ApplicationConfig config) {
        this.config = config;
    }

    @Provides
    ApplicationConfig config() {
        return config;
    }

    @Provides
    Random random() {
        return new Random();
    }

    @Provides
    Clock clock() {
        return Clock.systemUTC();
    }

    @Provides
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
