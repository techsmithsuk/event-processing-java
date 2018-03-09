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
import org.softwire.training.analyzer.model.ChronologicalAverage;
import org.softwire.training.analyzer.model.Event;
import org.softwire.training.analyzer.model.Location;
import org.softwire.training.analyzer.model.LocationAverage;
import org.softwire.training.analyzer.pipeline.ChronologicalAggregator;
import org.softwire.training.analyzer.pipeline.Deduplicator;
import org.softwire.training.analyzer.pipeline.LocationAggregator;
import org.softwire.training.analyzer.pipeline.LocationFilter;
import org.softwire.training.analyzer.pipeline.Pipeline;
import org.softwire.training.analyzer.pipeline.Sink;
import org.softwire.training.analyzer.pipeline.StatsCompiler;
import org.softwire.training.analyzer.services.ToStringMarshaller;
import org.softwire.training.analyzer.services.FileWriter;
import org.softwire.training.analyzer.services.LocationAverageMarshaller;
import org.softwire.training.analyzer.services.LocationService;
import org.softwire.training.analyzer.services.Marshaller;
import org.softwire.training.analyzer.services.QueueInfoLogger;
import org.softwire.training.analyzer.receiver.Receiver;
import org.softwire.training.analyzer.receiver.QueueSubscription;
import org.softwire.training.analyzer.receiver.SqsEventRetriever;

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

        FileWriter.Factory fileWriterFactory = new FileWriter.Factory();
        Marshaller<List<LocationAverage>> locationAverageMarshaller = new LocationAverageMarshaller();
        Marshaller<ChronologicalAverage> chronologicalAverageMarshaller = new ToStringMarshaller<>();

        Deduplicator deduplicator = new Deduplicator(config.deduplicator);
        List<Location> locations = new LocationService(config.locationService, s3, objectMapper).get();
        LocationFilter locationFilter = new LocationFilter(locations);

        ChronologicalAggregator chronologicalAggregator = new ChronologicalAggregator(
                config.chronologicalAggregator,
                clock,
                fileWriterFactory,
                chronologicalAverageMarshaller);
        LocationAggregator locationAggregator = new LocationAggregator(
                config.locationAggregator,
                locations,
                fileWriterFactory,
                locationAverageMarshaller
        );
        StatsCompiler statsCompiler = new StatsCompiler(config.application);

        try (QueueSubscription queueSubscription = new QueueSubscription(sqs, sns, config.receiver)) {
            try (QueueInfoLogger ignored = new QueueInfoLogger(sqs, queueSubscription.getQueueUrl())) {
                SqsEventRetriever sqsEventRetriever = new SqsEventRetriever(sqs, queueSubscription.getQueueUrl());
                Receiver receiver = new Receiver(sqsEventRetriever, config.receiver);

                Pipeline<Event, Event> pipeline = statsCompiler.compose(locationFilter).compose(deduplicator);
                Sink<Event> sink = Sink.parallel(chronologicalAggregator, locationAggregator);

                EventLoop eventLoop = new EventLoop(
                        config.application,
                        receiver,
                        pipeline,
                        sink,
                        clock);
                eventLoop.run();
            }
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
