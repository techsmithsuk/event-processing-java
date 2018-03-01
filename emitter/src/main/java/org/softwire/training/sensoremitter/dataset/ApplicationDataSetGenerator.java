package org.softwire.training.sensoremitter.dataset;

import com.google.inject.Inject;
import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.datasource.ChronologicalTrend;
import org.softwire.training.sensoremitter.datasource.Constant;
import org.softwire.training.sensoremitter.datasource.DataSource;
import org.softwire.training.sensoremitter.datasource.PositionalTrend;
import org.softwire.training.sensoremitter.datasource.ValueNoise;
import org.softwire.training.sensoremitter.emitter.DataSourceBackedEmitter;
import org.softwire.training.sensoremitter.emitter.Emitter;
import org.softwire.training.sensoremitter.model.Location;

import java.util.Random;
import java.util.Set;

/**
 * Generates the random application state and sets up the emitter.
 *
 * First constructs the list of valid locations, then the emitter which is made up of the following elements:
 *
 * 1. From the list of valid locations, creates an emitter for these locations which is the sum of:
 *    * A random increasing chronological trend..
 *    * A positional trend with a random peak, and values decreasing moving away from the peak.
 *
 * 2. Creates a list of unknown locations (which are not exported),  these also have a chronological and positional
 *    trend, but it is not the same as for the valid locations.
 */
public class ApplicationDataSetGenerator {
    // There is no clever maths at works in these constants, there just chosen because they give reasonable looking
    // values.
    private static final int POSITIONAL_TREND_VALUE_AT_PEAK = 10;
    private static final double POSITIONAL_TREND_SIGMA = 1D / 2;
    private static final int CHRONOLOGICAL_TREND_MIN_INCREASE = 2;
    private static final int CHRONOLOGICAL_TREND_MAX_INCREASE = 8;

    private final LocationsGenerator locationsGenerator;
    private final ApplicationConfig config;

    private final Random random;

    @Inject
    public ApplicationDataSetGenerator(LocationsGenerator locationsGenerator,
                                       ApplicationConfig config,
                                       Random random) {
        this.locationsGenerator = locationsGenerator;
        this.config = config;
        this.random = random;
    }

    public ApplicationDataSet create() {
        Set<Location> validLocations = locationsGenerator.buildLocations(config.numValidLocations);

        ChronologicalTrend chronologicalTrend = createRandomUpwardChronologicalTrend();

        double[] normalisedPeak = new double[] {random.nextDouble(),  random.nextDouble()};
        Location peak = new Location(
                normalisedPeak[0] * config.universeWidth,
                normalisedPeak[1] * config.universeWidth
        );
        PositionalTrend positionalTrend = new PositionalTrend(
                normalisedPeak,
                POSITIONAL_TREND_SIGMA,
                POSITIONAL_TREND_VALUE_AT_PEAK
        );
        DataSource validLocationsTrend = chronologicalTrend.add(positionalTrend).add(new ValueNoise(config, random));

        Emitter emitter = createEmitter(validLocations, validLocationsTrend);
        return new ApplicationDataSet(chronologicalTrend, peak, emitter, validLocations);
    }

    private Emitter createEmitter(Set<Location> validLocations,
                                  DataSource validLocationsTrend) {
        Emitter valid = validLocations
                .stream()
                .<Emitter>map(location -> new DataSourceBackedEmitter(validLocationsTrend, location, config))
                .reduce(Emitter.EMPTY, Emitter::concat);

        ChronologicalTrend badChronologicalTrend = new ChronologicalTrend(5, 0);
        PositionalTrend badPositionalTrend = new PositionalTrend(
                new double[] {random.nextDouble(),  random.nextDouble()},
                POSITIONAL_TREND_SIGMA,
                POSITIONAL_TREND_VALUE_AT_PEAK
        );
        DataSource unknownLocationSource = badChronologicalTrend.add(badPositionalTrend).add(new Constant(10));
        Emitter unknownLocations =
                locationsGenerator.buildLocations(config.numUnknownLocations)
                        .stream()
                        .<Emitter>map(location -> new DataSourceBackedEmitter(unknownLocationSource, location, config))
                        .reduce(Emitter.EMPTY, Emitter::concat);

        return valid.concat(unknownLocations);
    }

    private ChronologicalTrend createRandomUpwardChronologicalTrend() {
        int to = random.nextInt(CHRONOLOGICAL_TREND_MAX_INCREASE - CHRONOLOGICAL_TREND_MIN_INCREASE) +
                CHRONOLOGICAL_TREND_MIN_INCREASE;

        return new ChronologicalTrend(0, to);
    }
}
