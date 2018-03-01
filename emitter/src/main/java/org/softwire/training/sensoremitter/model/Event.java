package org.softwire.training.sensoremitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.time.Instant;
import java.util.UUID;

public class Event {
    @JsonProperty
    public final UUID locationId;
    @JsonProperty
    public final UUID eventId;
    @JsonProperty
    public final double value;
    @JsonProperty
    public final long timestamp;

    public Event(UUID locationId, UUID eventId, double value, long timestamp) {
        this.locationId = locationId;
        this.eventId = eventId;
        this.value = value;
        this.timestamp = timestamp;
    }

    public Event(UUID locationId, double value, Instant timestamp) {
        this(locationId, UUID.randomUUID(), value, timestamp.toEpochMilli());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Double.compare(event.value, value) == 0 &&
                timestamp == event.timestamp &&
                Objects.equal(locationId, event.locationId) &&
                Objects.equal(eventId, event.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(locationId, eventId, value, timestamp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("locationId", locationId)
                .add("eventId", eventId)
                .add("value", value)
                .add("timestamp", timestamp)
                .toString();
    }
}
