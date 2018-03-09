package org.softwire.training.analyzer.pipeline;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.model.ChronologicalAverage;
import org.softwire.training.analyzer.model.Event;
import org.softwire.training.analyzer.services.FileWriter;
import org.softwire.training.analyzer.services.Marshaller;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Aggregates received events by time, computing averages every config.averagingPeriod.
 * Events may be late by as much as expiryTime and early by as much as averagingPeriod.
 * <p>
 * ChronologicalAggregator keeps track of n+1 buckets where n = expiryTime / averagingPeriod
 * <p>
 * Let's say expiryTime = 10s and averagingPeriod = 1s, at 12:00:30:123, the situation is:
 * <p>
 * 12:00:20:000        now, 12:00:30:123
 * |                    |
 * v                    v
 * +-----+-----+-----+-----+-----+
 * |  0  | ... | n-1 |  n  | n+1 |
 * +-----+-----+-----+-----+-----+
 * <p>
 * There will be 12 buckets in total.   Each bucket is 1 second wide.  If an event comes
 * in with timestamp 12:00:20:XXX then it will go in bucket 0, an event at 12:00:30:XXX
 * will go in bucket 10, and an event with timestamp 12:00:31:XXX would go in bucket 11.
 * <p>
 * When now reaches the end of the nth bucket, ie. 12:00:31:000, we calculate an average
 * of bucket 0, output the average, delete it from the list, and add a new bucket at the
 * tail.
 * <p>
 * This allows us to absorb messages which are as much as expiryTime in the past as much
 * as the averagingPeriod in the future.
 * <p>
 * To avoid lots of conversions between epoch milliseconds and Instants, do all
 * calculations in whole numbers of milliseconds.
 * <p>
 * One alternative implementation is to split the aggregator into two stages:
 *
 * 1. Sort the elements by holding them in a (sorted) buffer queue for expiryTime minutes
 * 2. Aggregate by accumulating averagingPeriod worth of elements at a time (easy since
 *    they come in order)
 *
 * This has the following benefits:
 *
 * * Avoids the need for the averagingPeriod to evenly divide the expiryTime
 * * Each piece is simpler to understand
 * * Early elements 'just work'
 *
 * On the other hand the current implementation is more direct and possibly more
 * efficient.  We don't recommend one solution over the other.
 */
public class ChronologicalAggregator implements Sink<Event> {
    private static final Logger LOG = LoggerFactory.getLogger(ChronologicalAggregator.class);

    private final TypedConfig config;

    private final LinkedList<Bucket> buckets;
    private final FileWriter<ChronologicalAverage> fileWriter;

    // Keep track of all these to avoid recalculating for every event received.
    private long firstBucketStart;
    private long lastBucketStart;
    private long lastBucketEnd;

    public ChronologicalAggregator(TypedConfig config,
                                   Clock clock,
                                   FileWriter.Factory fileWriterFactory,
                                   Marshaller<ChronologicalAverage> marshaller) throws IOException {
        this.config = config;
        this.fileWriter = fileWriterFactory.get(config.filename, marshaller);

        final long now = clock.instant().toEpochMilli();
        if (now < config.expiryTime) {
            // This will never happen, it's not the '70s.
            throw new IllegalStateException("clock.instant returned time to near the epoch");
        }

        firstBucketStart = now - config.expiryTime;
        lastBucketStart = now + config.averagingPeriod;
        lastBucketEnd = now + 2 * config.averagingPeriod;

        buckets = IntStream.range(0, config.numberOfBuckets)
                // Suppress the first n-1 buckets as they won't receive any useful data
                .mapToObj(i -> new Bucket(
                        i < config.numberOfBuckets - 2,
                        firstBucketStart + i * config.averagingPeriod,
                        firstBucketStart + (i + 1) * config.averagingPeriod
                ))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public void handle(Instant now, Event event) {
        expireBuckets(now).forEach(fileWriter::write);
        getBucketOffset(event.timestamp)
                .ifPresent(offset -> buckets.get(offset).add(event.value));
    }

    private Stream<ChronologicalAverage> expireBuckets(Instant now) {
        long nowMillis = now.toEpochMilli();

        Stream<ChronologicalAverage> result = Stream.empty();
        while (nowMillis >= lastBucketStart) {
            result = Stream.concat(result, buckets.removeFirst().average());

            firstBucketStart += config.averagingPeriod;
            lastBucketStart += config.averagingPeriod;
            lastBucketEnd += config.averagingPeriod;

            buckets.addLast(new Bucket(false, lastBucketStart, lastBucketEnd));
        }

        return result;
    }

    private OptionalInt getBucketOffset(long time) {
        if (time < firstBucketStart || time >= lastBucketEnd) {
            LOG.warn("Rejecting event, timestamp out of range: {}", time);
            return OptionalInt.empty();
        }

        int bucketOffset = (int) Math.floor((time - firstBucketStart) / config.averagingPeriod);
        assert bucketOffset >= 0 : bucketOffset;
        assert bucketOffset < config.numberOfBuckets : bucketOffset;

        return OptionalInt.of(bucketOffset);
    }

    static class Bucket {
        private final LinkedList<Double> values = new LinkedList<>();
        private final boolean suppressOutput;
        private final long from;
        private final long to;

        Bucket(boolean suppressOutput, long from, long to) {
            this.suppressOutput = suppressOutput;
            this.to = to;
            this.from = from;
        }

        void add(double value) {
            values.add(value);
        }

        Stream<ChronologicalAverage> average() {
            OptionalDouble average = values.stream().mapToDouble(i -> i).average();
            // OptionalDouble doesn't have a map method!
            if (average.isPresent() && !suppressOutput) {
                return Stream.of(new ChronologicalAverage(
                        Instant.ofEpochMilli(from),
                        Instant.ofEpochMilli(to),
                        average.getAsDouble()));
            }
            return Stream.empty();
        }
    }

    public static class TypedConfig {
        final long averagingPeriod;
        final long expiryTime;
        final String filename;

        final int numberOfBuckets;

        public TypedConfig(Duration averagingPeriod, Duration expiryTime, String filename) {
            if (averagingPeriod.getNano() != 0 || expiryTime.getNano() != 0) {
                throw new IllegalArgumentException("averagingPeriod and expiryTime must be a whole number of milliseconds");
            }

            if (expiryTime.toMillis() % averagingPeriod.toMillis() != 0) {
                throw new IllegalArgumentException("ChronologicalAggregator averagingPeriod must divide evenly into expiryTime");
            }

            this.averagingPeriod = averagingPeriod.toMillis();
            this.expiryTime = expiryTime.toMillis();
            this.filename = filename;

            numberOfBuckets = (int) (this.expiryTime / this.averagingPeriod) + 2;
        }

        static public TypedConfig fromUntypedConfig(Config config) {
            return new TypedConfig(
                    config.getDuration("averagingPeriod"),
                    config.getDuration("expiryTime"),
                    config.getString("filename"));
        }
    }
}
