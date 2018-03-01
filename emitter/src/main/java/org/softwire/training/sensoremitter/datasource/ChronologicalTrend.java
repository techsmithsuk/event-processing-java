package org.softwire.training.sensoremitter.datasource;

import com.google.common.base.MoreObjects;

public class ChronologicalTrend implements DataSource {

    public final int from;
    public final int to;

    public ChronologicalTrend(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public double get(double x, double y, double t) {
        return t * to + (1 - t) * from;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("from", from)
                .add("to", to)
                .toString();
    }
}
