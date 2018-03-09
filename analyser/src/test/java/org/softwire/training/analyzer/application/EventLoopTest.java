package org.softwire.training.analyzer.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.builders.EventBuilder;
import org.softwire.training.analyzer.model.Event;
import org.softwire.training.analyzer.pipeline.Pipeline;
import org.softwire.training.analyzer.pipeline.Sink;
import org.softwire.training.analyzer.receiver.Receiver;

import java.time.*;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Theses tests are not ideal as they rely on knowing when the event loop calls clock.tick().
 */
class EventLoopTest {
    private static final Instant START = Instant.ofEpochMilli(0);
    private static final EventLoop.TypedConfig CONFIG = new EventLoop.TypedConfig(Duration.ofMinutes(1));

    private Receiver receiver;
    private Clock clock;
    private EventLoop eventLoop;
    private Pipeline<Event, Event> pipeline;
    private Sink<Event> sink;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        receiver = mock(Receiver.class);
        clock = mock(Clock.class);
        pipeline = (Pipeline<Event, Event>) mock(Pipeline.class);
        sink = (Sink<Event>) mock(Sink.class);
        // Start time is measured in the application constructor
        when(clock.instant()).thenReturn(START);

        eventLoop = new EventLoop(
                CONFIG,
                receiver,
                pipeline,
                sink,
                clock);
    }

    @Test
    void returnImmediatelyOnceEnoughTimeHasElapsed() throws InterruptedException {
        when(receiver.get()).thenReturn(Stream.empty());
        when(clock.instant()).thenReturn(START.plus(CONFIG.duration).plusSeconds(1));

        eventLoop.run();

        verify(receiver, never()).get();
    }

    @Test
    void passEventsThroughPipelineToSink() throws InterruptedException {
        Instant tick1 = START.plusSeconds(1);
        Instant tick2 = START.plus(CONFIG.duration).plusSeconds(1);
        when(clock.instant()).thenReturn(tick1, tick2);

        Event receivedEvent = new EventBuilder().setEventId(UUID.randomUUID()).createEvent();
        Event processedEvent = new EventBuilder().setEventId(UUID.randomUUID()).createEvent();
        when(receiver.get()).thenReturn(Stream.of(receivedEvent));
        doReturn(Stream.of(processedEvent)).when(pipeline).handle(tick2, receivedEvent);

        eventLoop.run();

        verify(sink, times(1)).handle(tick2, processedEvent);
    }
}