package org.softwire.training.analyzer;

import com.amazonaws.http.IdleConnectionReaper;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.application.EventLoop;
import org.softwire.training.analyzer.application.AwsClientFactory;
import org.softwire.training.analyzer.application.TypedConfig;
import org.softwire.training.analyzer.model.Average;
import org.softwire.training.analyzer.model.Event;
import org.softwire.training.analyzer.model.Location;
import org.softwire.training.analyzer.pipeline.Aggregator;
import org.softwire.training.analyzer.pipeline.Deduplicator;
import org.softwire.training.analyzer.pipeline.LocationFilter;
import org.softwire.training.analyzer.pipeline.Pipeline;
import org.softwire.training.analyzer.pipeline.StatsCompiler;
import org.softwire.training.analyzer.services.FileWriter;
import org.softwire.training.analyzer.services.LocationService;
import org.softwire.training.analyzer.services.QueueInfoLogger;
import org.softwire.training.analyzer.receiver.Receiver;
import org.softwire.training.analyzer.receiver.QueueSubscription;

import java.time.Clock;
import java.util.List;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            try {
                run();
            } finally {
                shutdownIdleConnectionReaper();
            }
        } catch (Throwable e) {
            LOG.error("Fatal error, terminating", e);
            System.exit(1);
        }
    }

    /**
     * Build and inject all dependencies of {@link EventLoop} manually, then run it.
     */
    private static void run() throws Exception {
        LOG.info("Bootstrapping application");

        Clock clock = Clock.systemUTC();
        ObjectMapper objectMapper = new ObjectMapper();

        TypedConfig config = new TypedConfig();

        AwsClientFactory awsClientFactory = new AwsClientFactory();
        AmazonSNS sns = awsClientFactory.sns();
        AmazonSQS sqs = awsClientFactory.sqs();
        AmazonS3 s3 = awsClientFactory.s3();

        Deduplicator deduplicator = new Deduplicator(config.deduplicator);
        List<Location> locations = new LocationService(config.locationService, s3, objectMapper).get();
        LocationFilter locationFilter = new LocationFilter(locations);
        Aggregator aggregator = new Aggregator(config.aggregator, clock);
        StatsCompiler statsCompiler = new StatsCompiler(config.application);
        FileWriter fileWriter = new FileWriter(config.fileWriter);

        try (QueueSubscription queueSubscription = new QueueSubscription(sqs, sns, config.receiver)) {

            Receiver receiver = new Receiver(sqs, queueSubscription.getQueueUrl());
            QueueInfoLogger queueInfoLogger = new QueueInfoLogger(sqs, queueSubscription.getQueueUrl());

            Pipeline<Event, Average> pipeline = statsCompiler
                    .compose(locationFilter)
                    .compose(deduplicator)
                    .compose(aggregator);

            EventLoop eventLoop = new EventLoop(
                    config.application,
                    receiver,
                    queueInfoLogger,
                    pipeline,
                    fileWriter,
                    clock);
            eventLoop.run();

        }

        statsCompiler.dumpStats();
    }

    /**
     * Stops the JVM from complaining when the app terminates because the {@link IdleConnectionReaper} thread has not
     * shutdown properly.
     */
    private static void shutdownIdleConnectionReaper() {
        try {
            LOG.debug("Shutting down IdleConnectionReaper");
            IdleConnectionReaper.shutdown();
        } catch (Exception e) {
            LOG.warn("Failed to shutdown IdleConnectionReaper: {}", e);
        }
    }
}
