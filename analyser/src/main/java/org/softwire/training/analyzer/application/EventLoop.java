package org.softwire.training.analyzer.application;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class EventLoop {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoop.class);

    private final Receiver receiver;
    private final Clock clock;
    private final Instant endTime;
    private final Pipeline<Event, Average> pipeline;
    private final FileWriter fileWriter;
    private final QueueInfoLogger queueInfoLogger;

    public EventLoop(TypedConfig config,
                     Receiver receiver,
                     QueueInfoLogger queueInfoLogger,
                     Pipeline<Event, Average> pipeline,
                     FileWriter fileWriter,
                     Clock clock) {
        this.receiver = receiver;
        this.queueInfoLogger = queueInfoLogger;
        this.pipeline = pipeline;
        this.fileWriter = fileWriter;
        this.clock = clock;

        endTime = clock.instant().plus(config.duration);
    }

    public void run() {
        // We need now to be writable from inside the forEach, so wrap in an array.
        Instant[] now = new Instant[]{clock.instant()};
        LOG.info("Entering main event loop at {}, will run until {}", now[0], endTime);

        while (true) {
            if (now[0].isAfter(endTime)) {
                break;
            }

            // Receiver.get() is blocking, so we need to call clock.instant() once messages have been received.
            receiver.get().forEach(ev -> {
                now[0] = clock.instant();
                queueInfoLogger.logInfoIfNecessary(now[0]);
                pipeline.handle(now[0], ev).forEach(fileWriter::write);
            });
        }
    }

    public static class TypedConfig {
        public final Duration duration;

        public TypedConfig(Duration duration) {
            this.duration = duration;
        }

        public static TypedConfig fromUntypedConfig(Config config) {
            return new TypedConfig(config.getDuration("duration"));
        }
    }
}
