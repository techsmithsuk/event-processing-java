package org.softwire.training.analyzer.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.builders.EventBuilder;
import org.softwire.training.analyzer.model.Average;
import org.softwire.training.analyzer.model.Event;
import org.softwire.training.analyzer.pipeline.Aggregator;
import org.softwire.training.analyzer.pipeline.Deduplicator;
import org.softwire.training.analyzer.pipeline.LocationFilter;
import org.softwire.training.analyzer.pipeline.Pipeline;
import org.softwire.training.analyzer.pipeline.StatsCompiler;
import org.softwire.training.analyzer.services.FileWriter;
import org.softwire.training.analyzer.services.QueueInfoLogger;
import org.softwire.training.analyzer.receiver.Receiver;

import java.time.*;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
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
    private FileWriter fileWriter;
    private Pipeline<Event, Average> pipeline;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        receiver = mock(Receiver.class);
        clock = mock(Clock.class);
        fileWriter = mock(FileWriter.class);
        pipeline = (Pipeline<Event, Average>) mock(Pipeline.class);
        QueueInfoLogger queueInfoLogger = mock(QueueInfoLogger.class);

        // Start time is measured in the application constructor
        when(clock.instant()).thenReturn(START);

        eventLoop = new EventLoop(
                CONFIG,
                receiver,
                queueInfoLogger,
                pipeline,
                fileWriter,
                clock);
    }

    @Test
    void returnImmediatelyOnceEnoughTimeHasElapsed() {
        when(receiver.get()).thenReturn(Stream.empty());
        when(clock.instant()).thenReturn(START.plus(CONFIG.duration).plusSeconds(1));

        eventLoop.run();

        verify(receiver, never()).get();
    }

    @Test
    void passEventsThroughPipelineToFileWriter() {
        Instant tick1 = START.plusSeconds(1);
        Instant tick2 = START.plus(CONFIG.duration).plusSeconds(1);
        when(clock.instant()).thenReturn(tick1, tick2);

        Event event = new EventBuilder().setEventId(UUID.randomUUID()).createEvent();
        when(receiver.get()).thenReturn(Stream.of(event));

        Average average = new Average(Instant.EPOCH, Instant.EPOCH, 1);
        when(pipeline.handle(tick2, event)).thenReturn(Stream.of(average));

        eventLoop.run();

        verify(fileWriter, times(1)).write(average);
    }

}