package org.softwire.training.sensoremitter.datasource;

@FunctionalInterface
public interface DataSource {
    /**
     * Must return a sensible value for 0 <= arg <= 1, where arg is one of x, y, or t.
     */
    double get(double x, double y, double t);

    default DataSource add(DataSource p) {
        return (x, y, t) -> DataSource.this.get(x, y, t) + p.get(x, y, t);
    }
}
