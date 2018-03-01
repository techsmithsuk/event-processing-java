package org.softwire.training.analyzer.receiver;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class QueueSubscription implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(QueueSubscription.class);

    private final AmazonSQS sqs;
    private final AmazonSNS sns;
    private final String queueUrl;
    private final String subscriptionArn;

    public QueueSubscription(AmazonSQS sqs, AmazonSNS sns, Receiver.TypedConfig config) {
        this.sqs = sqs;
        this.sns = sns;

        String queueName = UUID.randomUUID().toString();
        queueUrl = sqs.createQueue(new CreateQueueRequest(queueName)).getQueueUrl();
        subscriptionArn = Topics.subscribeQueue(sns, sqs, config.topicArn, queueUrl);
        LOG.info("Created queue URL: {} and subscription: {}", queueName, subscriptionArn);
    }

    @Override
    public void close() {
        LOG.info("Deleting queue: {} and subscription: {}", queueUrl, subscriptionArn);
        sqs.deleteQueue(queueUrl);
        sns.unsubscribe(subscriptionArn);
    }

    public String getQueueUrl() {
        return queueUrl;
    }
}
