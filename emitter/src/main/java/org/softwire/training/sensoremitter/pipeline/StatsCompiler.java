package org.softwire.training.sensoremitter.pipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.application.ApplicationConfig;

import java.time.Instant;
import java.util.stream.Stream;

/**
 * Counts all the events which pass through it, and logs some statistics to the console when asked.
 */
@Singleton
public class StatsCompiler implements Pipeline<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(StatsCompiler.class);

    private final ApplicationConfig config;
    private final double expectedEventsPerSecond;

    private int eventCount = 0;

    @Inject
    StatsCompiler(ApplicationConfig config) {
        this.config = config;

        expectedEventsPerSecond = calculateExpectedEventsPerSecond(config);
    }

    @Override
    public Stream<String> handle(Instant now, String element) {
        eventCount++;
        return Stream.of(element);
    }

    public double getExpectedEventsPerSecond() {
        return expectedEventsPerSecond;
    }

    public void logStats() {
        LOG.info("Event Count: {}", eventCount);
        LOG.info("Duration: {}", config.duration);
        LOG.info("Events/s: {}", ((double) eventCount) / config.duration.getSeconds());
        LOG.info("Expected events/s: {}", expectedEventsPerSecond);
    }

    private double calculateExpectedEventsPerSecond(ApplicationConfig config) {
        int totalLocations = config.numValidLocations + config.numUnknownLocations;
        double eventsPerSecondPerLocation = (double) 1000 / config.meanMillisToNextEvent;
        return (eventsPerSecondPerLocation * totalLocations) * (1 + config.duplicationProbability);
    }
}
