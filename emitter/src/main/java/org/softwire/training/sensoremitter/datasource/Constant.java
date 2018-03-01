package org.softwire.training.sensoremitter.datasource;

public class Constant implements DataSource {

    private final double value;

    public Constant(double value) {
        this.value = value;
    }

    @Override
    public double get(double x, double y, double t) {
        return value;
    }
}
