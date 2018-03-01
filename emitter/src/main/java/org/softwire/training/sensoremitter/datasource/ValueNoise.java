package org.softwire.training.sensoremitter.datasource;

import org.apache.commons.math3.analysis.interpolation.TricubicInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.TricubicInterpolator;
import org.softwire.training.sensoremitter.application.ApplicationConfig;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * https://en.wikipedia.org/wiki/Value_noise
 */
public class ValueNoise implements DataSource {
    private static final double MAX_VARIATION = 0.2D;

    private final TricubicInterpolatingFunction interpolator;
    private final ApplicationConfig config;

    public ValueNoise(final ApplicationConfig config, Random random) {
        this.config = config;
        final double[][][] lattice = new double[config.xIncrements][config.yIncrements][config.timeIncrements];
        for (int x = 0; x < config.xIncrements; x++) {
            for (int y = 0; y < config.yIncrements; y++) {
                for (int z = 0; z < config.timeIncrements; z++) {
                    lattice[x][y][z] = random.nextDouble() * MAX_VARIATION - (MAX_VARIATION / 2);
                }
            }
        }

        interpolator = new TricubicInterpolator().interpolate(
                IntStream.range(0, config.xIncrements).mapToDouble(x -> x).toArray(),
                IntStream.range(0, config.yIncrements).mapToDouble(x -> x).toArray(),
                IntStream.range(0, config.timeIncrements).mapToDouble(x -> x).toArray(),
                lattice);
    }

    public double get(final double x, final double y, final double z) {
        return interpolator.value(x * (config.xIncrements - 1), y * (config.yIncrements - 1), z * (config.timeIncrements - 1));
    }
}
