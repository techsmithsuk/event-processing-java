package org.softwire.training.sensoremitter.dataset;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.model.Location;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Generates a set of random locations, making sure they do not cluster too close to each other.
 */
public class LocationsGenerator {
    private final static Logger LOG = LoggerFactory.getLogger(LocationsGenerator.class);

    private final static int MAX_BUILD_LOCATIONS_ITERATIONS = 100000;

    private final Random random;
    private final ApplicationConfig config;

    @Inject
    LocationsGenerator(ApplicationConfig config, Random random) {
        this.config = config;
        this.random = random;
    }

    public Set<Location> buildLocations(int numLocations) {
        // Our strategy is to keep picking random points so long as they aren't too close to any existing points, until
        // we have enough.
        //
        // Packing n^2 points inside a 1x1 square with an ordinary grid gives an gap of about 1/(n - 1) between
        // points (packing hexagonally would be even better, but I'm way too lazy to do the maths).  We just want the
        // packing to not be terrible as this would make it hard to see positional trends, so set a minimum gap of 2/3
        // of this, ie. for m points: (2 / 3)  * (1 / (sqrt(m) - 1)).
        double reasonableMinDistanceBetweenLocations =
                2 * config.universeWidth / (3 * (Math.sqrt(config.numValidLocations) - 1));
        LOG.debug("Reasonable distance between locations: {}", reasonableMinDistanceBetweenLocations);

        Set<Location> locations = new HashSet<>();
        int iterations = 0;
        while (locations.size() < numLocations) {
            Location newLocation = new Location(randomDouble(config.universeWidth), randomDouble(config.universeWidth));
            boolean newLocationIsValid = locations.stream().noneMatch(existingLocation ->
                    existingLocation.distanceTo(newLocation) < reasonableMinDistanceBetweenLocations);
            if (newLocationIsValid) {
                locations.add(newLocation);
            }

            iterations++;
            if (iterations > MAX_BUILD_LOCATIONS_ITERATIONS) {
                throw new IllegalArgumentException("Unable to generate locations");
            }
        }
        return locations;
    }

    private double randomDouble(double max) {
        return random.nextDouble() * max;
    }
}
