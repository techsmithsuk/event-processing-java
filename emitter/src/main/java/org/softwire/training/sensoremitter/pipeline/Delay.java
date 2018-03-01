package org.softwire.training.sensoremitter.pipeline;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.model.Event;

import java.time.Instant;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * With a probability of config.delayProbability, delays events by a uniform amount between 0 and config.delayMax.
 */
public class Delay implements Pipeline<Event, Event> {
    private static final Logger LOG = LoggerFactory.getLogger(Delay.class);

    private final Random random;
    private final ApplicationConfig config;

    private final SortedSet<DelayedEvent> backlog = new TreeSet<>();

    @Inject
    Delay(ApplicationConfig config, Random random) {
        this.config = config;
        this.random = random;
    }

    @Override
    public Stream<Event> handle(Instant now, Event event) {
        Stream.Builder<Event> result = Stream.builder();
        if (random.nextDouble() < config.delayProbability) {
            Instant due = now.plusNanos((long) Math.ceil(config.delayMax.toNanos() * random.nextDouble()));
            LOG.info("Delay: {} {}", due, event.eventId);
            backlog.add(new DelayedEvent(event, due));
        } else {
            result.add(event);
        }

        while (!backlog.isEmpty() && backlog.first().due.isBefore(now)) {
            result.add(backlog.first().event);
            backlog.remove(backlog.first());
        }

        return result.build();
    }

    private static class DelayedEvent implements Comparable<DelayedEvent> {
        final Event event;
        final Instant due;

        DelayedEvent(Event event, Instant due) {
            this.event = event;
            this.due = due;
        }

        @Override
        public int compareTo(DelayedEvent o) {
            return due.compareTo(o.due);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("event", event)
                    .add("due", due)
                    .toString();
        }
    }
}
