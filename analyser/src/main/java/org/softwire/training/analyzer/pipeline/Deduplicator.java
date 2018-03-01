package org.softwire.training.analyzer.pipeline;

import com.google.common.base.MoreObjects;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.model.Event;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * It would be possible to write this using, for example, one of Guava's expiring caches rather than writing our own.
 */
public class Deduplicator implements Pipeline<Event, Event> {
    private static final Logger LOG = LoggerFactory.getLogger(Deduplicator.class);

    private final Set<UUID> idCache = new HashSet<>();
    private final Queue<ExpiryRecord> expiryRecords = new LinkedList<>();

    private final Duration expiryTime;

    public Deduplicator(TypedConfig config) {
        this.expiryTime = config.expiryTime;
    }

    @Override
    public Stream<Event> handle(Instant now, Event event) {
        UUID id = event.eventId;
        Stream<Event> result;

        while (!expiryRecords.isEmpty() && now.isAfter(expiryRecords.peek().expiry)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Expiring record: {} (Cache size: {})", expiryRecords.peek(), idCache.size());
            }
            idCache.remove(expiryRecords.remove().uuid);
        }

        if (idCache.contains(id)) {
            LOG.info("Found duplicate id: {}", id);
            result = Stream.empty();
        } else {
            result = Stream.of(event);
            idCache.add(id);
            expiryRecords.add(new ExpiryRecord(id, now.plus(expiryTime)));
        }

        return result;
    }

    private static class ExpiryRecord {
        final UUID uuid;
        final Instant expiry;

        ExpiryRecord(UUID uuid, Instant expiry) {
            this.uuid = uuid;
            this.expiry = expiry;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("uuid", uuid)
                    .add("expiry", expiry)
                    .toString();
        }
    }

    public static class TypedConfig {
        final Duration expiryTime;

        public TypedConfig(Duration expiryTime) {
            this.expiryTime = expiryTime;
        }

        public static TypedConfig fromUntypedConfig(Config config) {
            return new TypedConfig(config.getDuration("cacheTimeToLive"));
        }
    }
}
