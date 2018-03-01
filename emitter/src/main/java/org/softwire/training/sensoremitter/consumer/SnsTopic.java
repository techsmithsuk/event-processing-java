package org.softwire.training.sensoremitter.consumer;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.application.ApplicationConfig;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles sending messages to SNS.  Since this is the slowest part of the pipeline, we use a thread pool to make sure
 * we can get enough throughput.
 */
@Singleton
public class SnsTopic implements Consumer {
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 2;

    private static final Logger LOG = LoggerFactory.getLogger(SnsTopic.class);

    private final AmazonSNS sns;
    private final String topicArn;

    private final AtomicBoolean errorDuringPublish = new AtomicBoolean(false);
    private final AtomicBoolean errorDuringReap = new AtomicBoolean(false);
    private final DeadQueueReaper reaper;
    private final ExecutorService executorService;


    @Inject
    SnsTopic(ApplicationConfig config, AmazonSNS sns, AmazonSQS sqs) {
        topicArn = config.topicArn;
        this.sns = sns;
        this.reaper = new DeadQueueReaper(sns, sqs, topicArn, errorDuringReap);
        LOG.info("Starting SNS publishing thread pool with {} threads", config.threadCount);
        executorService = Executors.newFixedThreadPool(config.threadCount);
    }

    public void send(String message) {
        if (errorDuringPublish.get()) {
            throw new IllegalStateException("SNS Publishing thread reported error");
        }
        if (errorDuringReap.get()) {
            throw new IllegalStateException("SNS Dead Queue Reaper reported error");
        }
        LOG.debug("Publishing message: {}", message);
        executorService.execute(() -> publish(message));
    }

    private void publish(String message) {
        try {
            PublishResult publishResult = sns.publish(
                    new PublishRequest(topicArn, message));
            LOG.debug("SNS Client publish succeeded, id: {}", publishResult.getMessageId());
        } catch (Throwable t) {
            LOG.error("SNS Client publish failed: {}", t);
            errorDuringPublish.set(true);
        }
    }

    @Override
    public void close() {
        LOG.info("Shutting down thread pool");
        shutdownExecutorServiceSafely(executorService);
        LOG.info("Shutting down reaper");
        this.reaper.shutdown();
    }

    /**
     * If a client is written poorly, so that it creates a queue but does not delete it, then we could end up sending
     * messages to lots and lots of queues, which will cost us money!  To prevent this, we periodically look for queues
     * which have more than a certain number of messages and delete them.
     */
    static class DeadQueueReaper {
        private static final Logger LOG = LoggerFactory.getLogger(DeadQueueReaper.class);

        private static final int MAX_ALLOWED_MESSAGES_IN_QUEUE = 1000;
        private static final int REAPER_PERIOD_MINUTES = 5;

        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private final AmazonSNS sns;
        private final AmazonSQS sqs;
        private final String topicArn;
        private final AtomicBoolean encounteredError;

        DeadQueueReaper(AmazonSNS sns, AmazonSQS sqs, String topicArn, AtomicBoolean encounteredError) {
            this.sns = sns;
            this.sqs = sqs;
            this.topicArn = topicArn;
            this.encounteredError = encounteredError;
            scheduler.scheduleAtFixedRate(this::reap, 0,REAPER_PERIOD_MINUTES, TimeUnit.MINUTES);
        }

        private void reap() {
            try {
                sns.listSubscriptionsByTopic(topicArn).getSubscriptions().forEach(subscription -> {
                    LOG.debug("Processing subscription: {} - {}",
                            subscription.getSubscriptionArn(), subscription.getEndpoint());
                    processSubscription(subscription);
                });
            } catch (Exception e) {
                LOG.error("Reaper failed", e);
                encounteredError.set(true);
            }
        }

        private void processSubscription(Subscription subscription) {
            // We don't want to delete the example queues, but everything else is fair game.
            if (!subscription.getEndpoint().contains("ExampleQueue")) {
                Optional<String> queueUrlO = extractSqsQueueName(subscription.getEndpoint()).flatMap(this::getQueueUrl);
                if (queueUrlO.isPresent()) {
                    processQueue(subscription, queueUrlO.get());
                } else {
                    LOG.warn("Reaping subscription {} as the subscribed queue ({}) does not exist",
                            subscription.getSubscriptionArn(), subscription.getEndpoint());
                    sns.unsubscribe(subscription.getSubscriptionArn());
                }
            }
        }

        private void processQueue(Subscription subscription, String queueUrl) {
            int approxNumMessages = Integer.parseInt(sqs.getQueueAttributes(
                    queueUrl,
                    Collections.singletonList("ApproximateNumberOfMessages")
            ).getAttributes().get("ApproximateNumberOfMessages"));
            LOG.debug("Queue {} has {} messages", queueUrl, approxNumMessages);
            if (approxNumMessages > MAX_ALLOWED_MESSAGES_IN_QUEUE) {
                LOG.warn("Reaping queue {}  as it has over {} messages ({})",
                        queueUrl, MAX_ALLOWED_MESSAGES_IN_QUEUE, approxNumMessages);
                sns.unsubscribe(subscription.getSubscriptionArn());
                sqs.deleteQueue(queueUrl);
            }
        }

        private Optional<String> getQueueUrl(String queueName) {
            try {
                return Optional.of(sqs.getQueueUrl(queueName).getQueueUrl());
            } catch (QueueDoesNotExistException e) {
                return Optional.empty();
            }
        }

        private Optional<String> extractSqsQueueName(String endpoint) {
            String[] parts = endpoint.split(":");
            if (parts[2].equals("sqs")) {
                return Optional.of(parts[parts.length - 1]);
            }
            return Optional.empty();
        }

        void shutdown() {
            shutdownExecutorServiceSafely(scheduler);
        }
    }

    private static void shutdownExecutorServiceSafely(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (executorService.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                executorService.shutdownNow();
            if (!executorService.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOG.error("Unable to shutdown executor cleanly");
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted during close", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
