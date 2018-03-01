package org.softwire.training.analyzer.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.services.LocationService;
import org.softwire.training.analyzer.builders.EventBuilder;
import org.softwire.training.analyzer.builders.LocationBuilder;
import org.softwire.training.analyzer.model.Event;
import org.softwire.training.analyzer.model.Location;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationFilterTest {

    private static final Location KNOWN_LOCATION = new LocationBuilder().createLocation();
    private static final Instant NOW = Instant.ofEpochMilli(0);

    private LocationFilter locationFilter;

    @BeforeEach
    void beforeEach() {
        locationFilter = new LocationFilter(Collections.singletonList(KNOWN_LOCATION));
    }

    @Test
    void passThroughKnownLocations() {
        Event event = new EventBuilder().setLocationId(KNOWN_LOCATION.id).createEvent();

        assertThat(locationFilter.handle(NOW, event).collect(Collectors.toList()), contains(event));
    }

    @Test
    void ignoreUnknownLocations() {
        Event event = new EventBuilder().setLocationId(UUID.randomUUID()).createEvent();

        assertThat(locationFilter.handle(NOW, event).collect(Collectors.toList()), empty());
    }
}