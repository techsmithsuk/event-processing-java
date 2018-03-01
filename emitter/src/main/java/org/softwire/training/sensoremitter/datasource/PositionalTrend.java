package org.softwire.training.sensoremitter.datasource;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

public class PositionalTrend implements DataSource {
    private final MultivariateNormalDistribution multivariateNormalDistribution;
    private final double correctionFactor;

    // Probably an abuse of notation to call the parameter sigma, but I don't know the correct notation.
    public PositionalTrend(double[] peak, double sigma, double valueAtPeak) {
        this.multivariateNormalDistribution = new MultivariateNormalDistribution(
                peak,
                new double[][]{{sigma * sigma, 0}, {0, sigma * sigma}}
        );
        this.correctionFactor = valueAtPeak / multivariateNormalDistribution.density(peak);
    }

    @Override
    public double get(double x, double y, double t) {
        return multivariateNormalDistribution.density(new double[]{x, y}) * correctionFactor;
    }
}
