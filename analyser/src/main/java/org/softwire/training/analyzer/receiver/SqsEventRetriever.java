package org.softwire.training.analyzer.receiver;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.model.Event;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class is thread-safe as it has no mutable state of it's own, and both {@link AmazonSQS} and {@link ObjectMapper}
 * are thread-safe.
 */
public class SqsEventRetriever {
    private static final Logger LOG = LoggerFactory.getLogger(SqsEventRetriever.class);
    private final AmazonSQS sqs;
    private final String queueUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    public SqsEventRetriever(AmazonSQS sqs,
                             String queueUrl) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
    }

    public Stream<Event> get() {
        ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl).withWaitTimeSeconds(1);
        List<Message> messages = sqs.receiveMessage(request).getMessages();
        LOG.debug("Received {} messages", messages.size());
        deleteFromQueueSwallowingErrors(messages);
        return messages.stream().flatMap(this::parseMessageSwallowingErrors);
    }

    private Stream<Event> parseMessageSwallowingErrors(Message wrappedMessage) {
        String wrappedMessageBody = wrappedMessage.getBody();
        LOG.debug("Received message body: {}", wrappedMessageBody);
        try {
            // I'm surprised that there isn't a nice way of parsing the JSON envelope already in the AWS SDK, but I
            // couldn't find one.
            String messageBodyMessage = mapper.<Map<String, String>>readValue(
                    wrappedMessageBody,
                    new TypeReference<Map<String, String>>() {
                    }
            ).get("Message");
            Event event = mapper.readValue(messageBodyMessage, Event.class);
            LOG.debug("Decoded event: {}", event);
            return Stream.of(event);
        } catch (IOException e) {
            LOG.warn("Failed to parse JSON, error: {} JSON was: {}", e, wrappedMessageBody);
            return Stream.empty();
        }
    }

    private void deleteFromQueueSwallowingErrors(List<Message> messages) {
        if (messages.size() > 0) {
            List<DeleteMessageBatchRequestEntry> deleteMessageBatchRequestEntries = Streams.zip(
                    messages.stream(),
                    IntStream.range(0, messages.size()).boxed(), (message, i) ->
                            new DeleteMessageBatchRequestEntry(Integer.toString(i), message.getReceiptHandle())
            ).collect(Collectors.toList());
            LOG.debug("Deleting messages with request {}", deleteMessageBatchRequestEntries);
            DeleteMessageBatchResult deleteMessageBatchResult = sqs.deleteMessageBatch(queueUrl, deleteMessageBatchRequestEntries);
            deleteMessageBatchResult.getFailed().forEach(batchResultErrorEntry ->
                    LOG.warn("Failed to delete SQS message: {}", batchResultErrorEntry));
            LOG.debug("Message deletion complete");
        }
    }
}
