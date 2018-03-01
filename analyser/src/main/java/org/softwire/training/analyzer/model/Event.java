package org.softwire.training.analyzer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class Event {
    public final UUID locationId;
    public final UUID eventId;
    public final double value;
    public final long timestamp;

    @JsonCreator
    public Event(@JsonProperty("locationId") UUID locationId,
                 @JsonProperty("eventId") UUID eventId,
                 @JsonProperty("value") double value,
                 @JsonProperty("timestamp") long timestamp) {
        this.locationId = locationId;
        this.eventId = eventId;
        this.value = value;
        this.timestamp = timestamp;
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
}
