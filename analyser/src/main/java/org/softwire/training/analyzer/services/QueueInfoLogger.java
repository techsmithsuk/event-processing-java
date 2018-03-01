package org.softwire.training.analyzer.services;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

public class QueueInfoLogger {
    private static final Logger LOG = LoggerFactory.getLogger(QueueInfoLogger.class);

    private static final Duration LOG_FREQUENCY = Duration.ofMinutes(1);

    private final AmazonSQS sqs;
    private final String queueUrl;

    private Instant nextLogDue = Instant.EPOCH;

    public QueueInfoLogger(AmazonSQS sqs, String queueUrl) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
    }

    public void logInfoIfNecessary(Instant now) {
        if (now.isAfter(nextLogDue)) {
            LOG.info("Visible messages on queue: {}", getNumberOfVisibleMessages());
            nextLogDue = now.plus(LOG_FREQUENCY);
        }
    }

    private int getNumberOfVisibleMessages() {
        GetQueueAttributesResult queueAttributes = sqs
                .getQueueAttributes(queueUrl, Collections.singletonList("ApproximateNumberOfMessages"));
        return Integer.valueOf(queueAttributes.getAttributes().get("ApproximateNumberOfMessages"));
    }
}
