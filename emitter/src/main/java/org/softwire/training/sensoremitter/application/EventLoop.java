package org.softwire.training.sensoremitter.application;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.consumer.ConsoleTopic;
import org.softwire.training.sensoremitter.consumer.Consumer;
import org.softwire.training.sensoremitter.emitter.Emitter;
import org.softwire.training.sensoremitter.model.Event;
import org.softwire.training.sensoremitter.pipeline.ClockSkew;
import org.softwire.training.sensoremitter.pipeline.Delay;
import org.softwire.training.sensoremitter.pipeline.Duplicator;
import org.softwire.training.sensoremitter.pipeline.JsonDestroyer;
import org.softwire.training.sensoremitter.pipeline.JsonMarshaller;
import org.softwire.training.sensoremitter.pipeline.Pipeline;
import org.softwire.training.sensoremitter.pipeline.StatsCompiler;

import java.time.Clock;
import java.time.Instant;

public class EventLoop {

    private static final Logger LOG = LoggerFactory.getLogger(EventLoop.class);

    private final ApplicationConfig config;
    private final Provider<Consumer> consumerProvider;
    private final Clock clock;
    private final Pipeline<Event, String> pipeline;

    @Inject
    EventLoop(ApplicationConfig config,
              Provider<Consumer> consumerProvider,
              Duplicator duplicator,
              Delay delay,
              ClockSkew clockSkew,
              JsonMarshaller<Event> jsonMarshaller,
              JsonDestroyer jsonDestroyer,
              StatsCompiler statsCompiler,
              Clock clock) {
        this.config = config;
        this.consumerProvider = consumerProvider;
        this.clock = clock;
        this.pipeline = duplicator
                .compose(delay)
                .compose(clockSkew)
                .compose(jsonMarshaller)
                .compose(jsonDestroyer)
                .compose(statsCompiler);
    }

    public void run(Emitter emitter) throws Exception {
        LOG.info("Entering main event loop, will run until {}", config.endOfTime);
        try (Consumer consumer = consumerProvider.get()) {
            while (true) {
                Instant now = clock.instant();
                if (now.isAfter(config.endOfTime)) {
                    break;
                }
                emitter.emitIfRequired(now)
                        .flatMap(event -> pipeline.handle(now, event))
                        .forEach(consumer::send);
            }
        }
    }
}
