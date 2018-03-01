package org.softwire.training.sensoremitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.UUID;

public class Location {
    @JsonProperty
    public final double x;
    @JsonProperty
    public final double y;
    @JsonProperty
    public final UUID id;

    public Location(double x, double y, UUID id) {
        this.x = x;
        this.y = y;
        this.id = id;
    }

    public Location(double x, double y) {
        this(x, y, UUID.randomUUID());
    }

    public double distanceTo(Location o) {
        return distanceTo(o.x, o.y);
    }

    public double distanceTo(double x, double y) {
        return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Double.compare(location.x, x) == 0 &&
                Double.compare(location.y, y) == 0 &&
                Objects.equal(id, location.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y, id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("x", x)
                .add("y", y)
                .add("id", id)
                .toString();
    }
}
