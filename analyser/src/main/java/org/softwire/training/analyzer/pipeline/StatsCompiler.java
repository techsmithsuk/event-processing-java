package org.softwire.training.analyzer.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.application.EventLoop;
import org.softwire.training.analyzer.model.Event;

import java.time.Instant;
import java.util.stream.Stream;

public class StatsCompiler implements Pipeline<Event, Event> {
    private static final Logger LOG = LoggerFactory.getLogger(StatsCompiler.class);

    private final EventLoop.TypedConfig config;
    private int eventCount = 0;

    public StatsCompiler(EventLoop.TypedConfig config) {
        this.config = config;
    }

    @Override
    public Stream<Event> handle(Instant now, Event event) {
        eventCount++;
        return Stream.of(event);
    }

    public void dumpStats() {
        LOG.info("Event Count: {}", eventCount);
        LOG.info("Duration: {}", config.duration);
        LOG.info("Events/s: {}", ((float) eventCount) / config.duration.getSeconds());
    }
}
