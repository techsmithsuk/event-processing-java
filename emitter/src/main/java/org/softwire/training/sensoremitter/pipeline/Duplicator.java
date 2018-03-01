package org.softwire.training.sensoremitter.pipeline;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.model.Event;

import java.time.Instant;
import java.util.Random;
import java.util.stream.Stream;

/**
 * With a probability of config.duplicationProbability, duplicates an event and emits the original event plus it's
 * duplicate.
 */
public class Duplicator implements Pipeline<Event, Event> {
    private static final Logger LOG = LoggerFactory.getLogger(Duplicator.class);

    private final Random random;
    private final double duplicationProbability;

    @Inject
    Duplicator(ApplicationConfig config, Random random) {
        this.random = random;
        duplicationProbability = config.duplicationProbability;
    }

    @Override
    public Stream<Event> handle(Instant now, Event event) {
        if (random.nextDouble() < duplicationProbability) {
            LOG.info("Duplicated event: {}", event.eventId);
            return Stream.of(event, event);
        }
        return Stream.of(event);
    }
}
