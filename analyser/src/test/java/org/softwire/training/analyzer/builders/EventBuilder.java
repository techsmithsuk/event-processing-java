package org.softwire.training.analyzer.builders;

import org.softwire.training.analyzer.model.Event;

import java.time.Instant;
import java.util.UUID;

public class EventBuilder {
    private static final UUID DEFAULT_LOCATION_ID = UUID.randomUUID();
    private static final UUID DEFAULT_EVENT_ID = UUID.randomUUID();

    private UUID locationId = DEFAULT_LOCATION_ID;
    private UUID eventId = DEFAULT_EVENT_ID;

    private double value = 0;
    private long timestamp = Instant.now().toEpochMilli();

    public EventBuilder setLocationId(UUID locationId) {
        this.locationId = locationId;
        return this;
    }

    public EventBuilder setEventId(UUID eventId) {
        this.eventId = eventId;
        return this;
    }

    public EventBuilder setValue(double value) {
        this.value = value;
        return this;
    }

    public EventBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Event createEvent() {
        return new Event(locationId, eventId, value, timestamp);
    }
}