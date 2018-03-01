package org.softwire.training.analyzer.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.builders.EventBuilder;
import org.softwire.training.analyzer.model.Event;

import java.time.*;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class DeduplicatorTest {

    private static final Instant START = Instant.EPOCH;
    private static final Deduplicator.TypedConfig CONFIG = new Deduplicator.TypedConfig(Duration.ofMinutes(1));

    private Deduplicator deduplicator;

    @BeforeEach
    void beforeEach() {
        deduplicator = new Deduplicator(CONFIG);
    }

    @Test
    void allowTwoMessagesWhichDifferOnlyById() {
        Event event1 = new EventBuilder().setEventId(UUID.randomUUID()).createEvent();
        Event event2 = new EventBuilder().setEventId(UUID.randomUUID()).createEvent();

        assertThat(deduplicator.handle(START, event1).collect(Collectors.toList()), contains(event1));
        assertThat(deduplicator.handle(START, event2).collect(Collectors.toList()), contains(event2));
    }

    @Test
    void deduplicateTwoIdenticalMessages() {
        Event event = new EventBuilder().createEvent();
        assertThat(deduplicator.handle(START, event).collect(Collectors.toList()), contains(event));
        assertThat(deduplicator.handle(START.plus(CONFIG.expiryTime.minusSeconds(1)), event).collect(Collectors.toList()), empty());
    }

    @Test
    void allowDuplicateMessagesWhenEnoughTimeHasPassed() {
        Event event = new EventBuilder().createEvent();
        assertThat(deduplicator.handle(START, event).collect(Collectors.toList()), contains(event));
        assertThat(deduplicator.handle(START.plus(CONFIG.expiryTime.plusSeconds(1)), event).collect(Collectors.toList()), contains(event));
    }
}