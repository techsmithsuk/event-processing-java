package org.softwire.training.analyzer.services;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QueueInfoLogger implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(QueueInfoLogger.class);

    private static final Duration LOG_FREQUENCY = Duration.ofMinutes(1);
    private static final long EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 1;

    private final AmazonSQS sqs;
    private final String queueUrl;
    private final ScheduledExecutorService scheduler;

    public QueueInfoLogger(AmazonSQS sqs, String queueUrl) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.scheduler = Executors.newScheduledThreadPool(1);

        this.scheduler.scheduleAtFixedRate(
                this::logNumberOfVisibleMessages,
                0,
                LOG_FREQUENCY.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private void logNumberOfVisibleMessages() {
        GetQueueAttributesResult queueAttributes = sqs
                .getQueueAttributes(queueUrl, Collections.singletonList("ApproximateNumberOfMessages"));
        int numberOfVisibleMessages = Integer
                .valueOf(queueAttributes.getAttributes().get("ApproximateNumberOfMessages"));
        LOG.info("Visible messages on queue: {}", numberOfVisibleMessages);
    }

    @Override
    public void close() {
        LOG.info("Shutting down scheduler");

        // This shutdown procedure is excessive - it's very unlikely that the scheduler will not terminate properly
        // after calling shutdown.  It's nice to do things properly though.
        scheduler.shutdown();
        try {
            if (scheduler.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                scheduler.shutdownNow();
            if (!scheduler.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOG.error("Unable to shutdown scheduler cleanly");
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted during close", e);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.debug("Scheduler terminated");
    }
}
