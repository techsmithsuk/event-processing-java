package org.softwire.training.sensoremitter.pipeline;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.application.Application;
import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.model.Event;

import java.time.Instant;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Skew the clock a tiny bit into the future sometimes, not enough to effect the trends but enough that events will
 * arrive slightly early.
 */
public class ClockSkew implements Pipeline<Event, Event> {

    private static final Logger LOG = LoggerFactory.getLogger(ClockSkew.class);

    private final Random random;
    private final ApplicationConfig config;

    @Inject
    public ClockSkew(ApplicationConfig config, Random random) {
        this.config = config;
        this.random = random;
    }

    @Override
    public Stream<Event> handle(Instant now, Event event) {
        if (random.nextDouble() < config.clockSkewProbability) {
            int skew = (int) Math.ceil(random.nextDouble() * config.clockSkewMax.toMillis());
            LOG.info("Clock skew by for event {} by {} millis", event.eventId, skew);
            return Stream.of(new Event(event.locationId, event.eventId, event.value, event.timestamp + skew));
        }
        return Stream.of(event);
    }
}
