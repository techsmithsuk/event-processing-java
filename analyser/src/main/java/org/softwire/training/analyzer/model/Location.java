package org.softwire.training.analyzer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class Location {
    public final float x;
    public final float y;
    public final UUID id;

    @JsonCreator
    public Location(@JsonProperty("x") float x,
                    @JsonProperty("y") float y,
                    @JsonProperty("id") UUID id) {
        this.x = x;
        this.y = y;
        this.id = id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("x", x)
                .add("y", y)
                .add("id", id)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Float.compare(location.x, x) == 0 &&
                Float.compare(location.y, y) == 0 &&
                Objects.equal(id, location.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y, id);
    }
}
