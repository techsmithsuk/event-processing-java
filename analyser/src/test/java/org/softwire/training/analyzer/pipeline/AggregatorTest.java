package org.softwire.training.analyzer.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.builders.EventBuilder;
import org.softwire.training.analyzer.model.Average;
import org.softwire.training.analyzer.model.Event;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AggregatorTest {
    private static final int EXPIRY_SECONDS = 30;
    private static final int AVERAGING_PERIOD_SECONDS = 10;
    private static final Aggregator.TypedConfig CONFIG = new Aggregator.TypedConfig(
            Duration.ofSeconds(AVERAGING_PERIOD_SECONDS),
            Duration.ofSeconds(EXPIRY_SECONDS));
    private static final Instant START = Instant.EPOCH.plus(Duration.ofSeconds(100));

    private Clock clock;
    private AggregatorWrapper aggregator;

    @BeforeEach
    void beforeEach() {
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(START);

        aggregator = new AggregatorWrapper(new Aggregator(CONFIG, clock
        ));
    }

    @Test
    void calculatesExpectedAverageForSingleEvent() {
        aggregator.sendEvent(0, 0, 1);

        assertThat(aggregator.expireAndGetAverages(),
                equalTo(singletonList(buildAverage(0, 1))));
    }

    @Test
    void calculatesExpectedAverageForMultipleEvents() {
        aggregator.sendEvent(0, 0, 3);
        aggregator.sendEvent(0, 1, 4);
        aggregator.sendEvent(0, AVERAGING_PERIOD_SECONDS - 1, 20);

        assertThat(aggregator.expireAndGetAverages(),
                equalTo(singletonList(buildAverage(0, 9))));
    }

    @Test
    void calculatesMultipleAverages() {
        aggregator.sendEvent(0, 0, 7);
        aggregator.sendEvent(0, AVERAGING_PERIOD_SECONDS - 1, 20);
        aggregator.sendEvent(0, AVERAGING_PERIOD_SECONDS, -3);
        aggregator.sendEvent(0, AVERAGING_PERIOD_SECONDS + 1, -4);

        assertThat(aggregator.expireAndGetAverages(), equalTo(Arrays.asList(
                buildAverage(0, 13.5),
                buildAverage(AVERAGING_PERIOD_SECONDS, -3.5)
        )));
    }

    @Test
    void returnsEmptyAverageIfNoEvents() {
        assertThat(aggregator.expireAndGetAverages(), equalTo(emptyList()));
    }

    @Test
    void ignoreMessageWithTimestampsBeforeAggregatorStartup() {
        aggregator.sendEvent(0, -1, 1);
        aggregator.sendEvent(0, -EXPIRY_SECONDS, 1);
        aggregator.sendEvent(0, -EXPIRY_SECONDS - 1, 1);

        assertThat(aggregator.expireAndGetAverages(), equalTo(emptyList()));
    }

    @Test
    void dropMessageWhichIsLaterThanExpiryDate() {
        aggregator.sendEvent(EXPIRY_SECONDS + AVERAGING_PERIOD_SECONDS, 0, 3);

        assertThat(aggregator.expireAndGetAverages(), equalTo(emptyList()));
    }

    @Test
    void allowMessageWhichIsJustInsideTheExpiryDuration() {
        aggregator.sendEvent(EXPIRY_SECONDS, 0, 3);

        assertThat(aggregator.expireAndGetAverages(), equalTo(singletonList(buildAverage(0, 3))));
    }

    @Test
    void dropMessageWhichIsMoreThanOneAveragingPeriodInTheFuture() {
        aggregator.sendEvent(0,   2 * AVERAGING_PERIOD_SECONDS, 3);

        assertThat(aggregator.expireAndGetAverages(), equalTo(emptyList()));
    }

    @Test
    void allowMessageWhichIsUnderOneAveragingPeriodInTheFuture() {
        aggregator.sendEvent(0, AVERAGING_PERIOD_SECONDS, 3);

        assertThat(aggregator.expireAndGetAverages(), equalTo(singletonList(buildAverage(AVERAGING_PERIOD_SECONDS, 3))));
    }

    @Test
    void refuseToRunNearTheEpoch() {
        when(clock.instant()).thenReturn(Instant.EPOCH);

        assertThrows(IllegalStateException.class, () -> new Aggregator(CONFIG, clock));
    }

    @Test
    void rejectsConfigWhereAveragingPeriodDoesntDivideExpiry() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aggregator.TypedConfig(Duration.ofSeconds(2), Duration.ofSeconds(1)));
    }

    @Test
    void rejectsConfigWhereAveragingPeriodHasNanos() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aggregator.TypedConfig(Duration.ofSeconds(1).plusNanos(1), Duration.ofSeconds(1)));
    }

    private static Average buildAverage(int fromSeconds, double value) {
        return new Average(
                START.plusSeconds(fromSeconds),
                START.plusSeconds(fromSeconds + AVERAGING_PERIOD_SECONDS),
                value);
    }

    private static class AggregatorWrapper {
        private final Aggregator aggregator;
        private final List<Average> averages;
        private int lastNowSeconds = 0;

        AggregatorWrapper(Aggregator aggregator) {
            this.aggregator = aggregator;
            averages = new ArrayList<>();
        }

        void sendEvent(int nowSeconds, int eventTimestampSeconds, double eventValue) {
            if (nowSeconds < lastNowSeconds) {
                throw new IllegalStateException("'now' must always increase");
            }
            lastNowSeconds = nowSeconds;
            Event event = new EventBuilder()
                    .setTimestamp(START.plusSeconds(eventTimestampSeconds).toEpochMilli())
                    .setValue(eventValue)
                    .createEvent();
            averages.addAll(aggregator.handle(START.plusSeconds(nowSeconds), event).collect(Collectors.toList()));
        }

        List<Average> expireAndGetAverages() {
            // Force expiry
            int time = lastNowSeconds + 100 * EXPIRY_SECONDS;
            sendEvent(time, time, 0);
            return averages;
        }
    }
}