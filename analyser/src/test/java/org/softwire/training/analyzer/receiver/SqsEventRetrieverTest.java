package org.softwire.training.analyzer.receiver;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResultEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.model.Event;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqsEventRetrieverTest {

    private static final String QUEUE_URL = "Queue URL";

    private AmazonSQS sqs;

    private SqsEventRetriever sqsEventRetriever;

    @BeforeEach
    void beforeEach() {
        sqs = mock(AmazonSQS.class);
                when(sqs.deleteMessageBatch(eq(QUEUE_URL), anyListOf(DeleteMessageBatchRequestEntry.class)))
            .thenReturn(new DeleteMessageBatchResult().withSuccessful(new DeleteMessageBatchResultEntry()));

        sqsEventRetriever = new SqsEventRetriever(sqs, QUEUE_URL);
    }

    @Test
    void ignoresInvalidMessage() {
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(buildMessages(singletonList("wat")));

        assertThat(sqsEventRetriever.get().collect(Collectors.toList()), empty());
    }

    @Test
    void parsesValidMessage() {
        String valid = "{" +
                "   \"locationId\": \"4887f60d-d65c-4594-9087-aec8373b3de0\", " +
                "   \"eventId\": \"d8d00d6a-8e91-44dd-926d-2b389c436d45\", " +
                "   \"timestamp\": 123456789, " +
                "   \"value\": 2" +
                "}";
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(buildMessages(singletonList(valid)));

        assertThat(sqsEventRetriever.get().collect(Collectors.toList()), equalTo(singletonList(new Event(
                UUID.fromString("4887f60d-d65c-4594-9087-aec8373b3de0"),
                UUID.fromString("d8d00d6a-8e91-44dd-926d-2b389c436d45"),
                2,
                123456789
        ))));
    }

    @Test
    void deletesMessagesOffQueue() {
        String valid = "{" +
                "   \"locationId\": \"4887f60d-d65c-4594-9087-aec8373b3de0\", " +
                "   \"eventId\": \"d8d00d6a-8e91-44dd-926d-2b389c436d45\", " +
                "   \"timestamp\": 123456789, " +
                "   \"value\": 2" +
                "}";
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(buildMessages(singletonList(valid)));

        sqsEventRetriever.get();

        verify(sqs, times(1)).deleteMessageBatch(eq(QUEUE_URL), anyListOf(DeleteMessageBatchRequestEntry.class));
    }

    @Test
    void ignoresErrorsWhenDeletingMessages() {
        DeleteMessageBatchResult result = new DeleteMessageBatchResult().withFailed(
                new BatchResultErrorEntry().withMessage("Oh dear"));
        when(sqs.deleteMessageBatch(anyString(), anyListOf(DeleteMessageBatchRequestEntry.class))).thenReturn(result);
        String valid = "{" +
                "   \"locationId\": \"4887f60d-d65c-4594-9087-aec8373b3de0\", " +
                "   \"eventId\": \"d8d00d6a-8e91-44dd-926d-2b389c436d45\", " +
                "   \"timestamp\": 123456789, " +
                "   \"value\": 2" +
                "}";

        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(buildMessages(singletonList(valid)));

        assertThat(sqsEventRetriever.get().collect(Collectors.toList()), contains(new Event(
                UUID.fromString("4887f60d-d65c-4594-9087-aec8373b3de0"),
                UUID.fromString("d8d00d6a-8e91-44dd-926d-2b389c436d45"),
                2,
                123456789
        )));
    }

    private ReceiveMessageResult buildMessages(List<String> messageBodies) {
        JsonStringEncoder jsonStringEncoder = new JsonStringEncoder();
        String sqsEnvelope = "{\"Message\": \"%s\"}";
        return new ReceiveMessageResult().withMessages(
                messageBodies
                        .stream()
                        .map(body -> new Message().withBody(String.format(sqsEnvelope, new String(jsonStringEncoder.quoteAsString(body)))))
                        .collect(Collectors.toList())
        );
    }

}
