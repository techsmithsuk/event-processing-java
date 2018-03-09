package org.softwire.training.analyzer.pipeline;

import com.typesafe.config.Config;
import org.softwire.training.analyzer.model.Event;
import org.softwire.training.analyzer.model.Location;
import org.softwire.training.analyzer.model.LocationAverage;
import org.softwire.training.analyzer.services.FileWriter;
import org.softwire.training.analyzer.services.Marshaller;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocationAggregator implements Sink<Event> {
    private final List<Location> locations;
    private final Map<UUID, List<Double>> averages;
    private final FileWriter<List<LocationAverage>> fileWriter;
    private final int observationsRequired;

    private int observations = 0;

    public LocationAggregator(TypedConfig config,
                              List<Location> locations,
                              FileWriter.Factory fileWriterFactory,
                              Marshaller<List<LocationAverage>> marshaller) throws IOException {
        this.averages = locations
                .stream()
                .map(location -> location.id)
                .collect(Collectors.toMap(id -> id, id -> new LinkedList<>()));
        this.locations = locations;
        this.fileWriter = fileWriterFactory.get(config.filename, marshaller);
        observationsRequired = config.observationsRequired;
    }

    @Override
    public void handle(Instant now, Event event) {
        averages.get(event.locationId).add(event.value);
        observations += 1;

        if (observations >= observationsRequired) {
            observations = 0;
            guessLocation();
        }
    }

    private void guessLocation() {
        List<LocationAverage> locationAverages = locations.stream().flatMap(location -> {
            OptionalDouble average = averages.get(location.id).stream().mapToDouble(i -> i).average();
            if (average.isPresent()) {
                return Stream.of(new LocationAverage(location, average.getAsDouble()));
            }
            return Stream.empty();
        }).collect(Collectors.toList());

        fileWriter.write(locationAverages);
    }

    public static class TypedConfig {
        final String filename;
        final int observationsRequired;

        public TypedConfig(String filename, int observationsRequired) {
            this.filename = filename;
            this.observationsRequired = observationsRequired;
        }

        static public TypedConfig fromUntypedConfig(Config config) {
            return new TypedConfig(config.getString("filename"), config.getInt("observationsRequired"));
        }
    }
}
