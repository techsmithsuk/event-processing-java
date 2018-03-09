package org.softwire.training.analyzer.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.time.Instant;

@SuppressWarnings("WeakerAccess")
public class ChronologicalAverage {
    public final Instant to;
    public final Instant from;
    public final double value;

    public ChronologicalAverage(Instant to, Instant from, double value) {
        this.to = to;
        this.from = from;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChronologicalAverage chronologicalAverage = (ChronologicalAverage) o;
        return Double.compare(chronologicalAverage.value, value) == 0 &&
                Objects.equal(to, chronologicalAverage.to) &&
                Objects.equal(from, chronologicalAverage.from);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(to, from, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("to", to)
                .add("from", from)
                .add("value", value)
                .toString();
    }
}
