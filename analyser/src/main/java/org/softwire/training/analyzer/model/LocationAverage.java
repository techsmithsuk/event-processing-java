package org.softwire.training.analyzer.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class LocationAverage {
    public final Location location;
    public final double value;

    public LocationAverage(Location location, double value) {
        this.location = location;
        this.value = value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("location", location)
                .add("value", value)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationAverage that = (LocationAverage) o;
        return Double.compare(that.value, value) == 0 &&
                Objects.equal(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location, value);
    }
}
