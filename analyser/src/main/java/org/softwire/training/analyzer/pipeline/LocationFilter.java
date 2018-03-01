package org.softwire.training.analyzer.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.model.Location;
import org.softwire.training.analyzer.services.LocationService;
import org.softwire.training.analyzer.model.Event;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocationFilter implements Pipeline<Event,Event> {
    private static final Logger LOG = LoggerFactory.getLogger(LocationFilter.class);

    private final HashSet<UUID> locations;

    public LocationFilter(List<Location> locations) {
        this.locations = locations
                .stream()
                .map(location -> location.id)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Stream<Event> handle(Instant now, Event event) {
        if (locations.contains(event.locationId)) {
            return Stream.of(event);
        }
        LOG.debug("Dropping event with unknown location ID: {}", event);
        return Stream.empty();
    }
}
