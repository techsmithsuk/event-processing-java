package org.softwire.training.analyzer.receiver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.builders.EventBuilder;
import org.softwire.training.analyzer.model.Event;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The receiver is painful to unit test because it handles threads.  Rather than trying to get decent coverage of all
 * it's functionality, this class just has some smoke tests.
 */
class ReceiverTest {
    private static final Receiver.TypedConfig CONFIG = new Receiver.TypedConfig("topic arn", 1);
    private SqsEventRetriever sqsEventReceiver;

    @BeforeEach
    void beforeEach() {
        sqsEventReceiver = mock(SqsEventRetriever.class);
    }

    @Test
    void receivesEventsFromSqsEventReceiver() throws InterruptedException {
        Event event = new EventBuilder().createEvent();
        when(sqsEventReceiver.get()).thenReturn(Stream.of(event));

        try (Receiver receiver = new Receiver(sqsEventReceiver, CONFIG)) {
            assertThat(receiver.get().collect(Collectors.toList()).get(0), equalTo(event));
        }
    }

    @Test
    void throwsWhenWorkerThreadEncountersFatalError() throws InterruptedException {
        when(sqsEventReceiver.get()).thenThrow(new RuntimeException());

        try (Receiver receiver = new Receiver(sqsEventReceiver, CONFIG)) {
            assertThrows(Receiver.FatalErrorInWorkerThreadException.class, receiver::get);
        }
    }
}
