package org.softwire.training.analyzer.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.builders.EventBuilder;
import org.softwire.training.analyzer.builders.LocationBuilder;
import org.softwire.training.analyzer.model.Location;
import org.softwire.training.analyzer.model.LocationAverage;
import org.softwire.training.analyzer.services.FileWriter;
import org.softwire.training.analyzer.services.ToStringMarshaller;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationAggregatorTest {
    private static final LocationAggregator.TypedConfig CONFIG =
            new LocationAggregator.TypedConfig("filename", 3);

    private final List<Location> locations = Arrays.asList(
            new LocationBuilder().setX(0).setY(0).setId(UUID.randomUUID()).createLocation(),
            new LocationBuilder().setX(1).setY(1).setId(UUID.randomUUID()).createLocation());

    private LocationAggregator locationAggregator;
    private List<List<LocationAverage>> writtenToFile;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() throws IOException {
        writtenToFile = new ArrayList<>();

        FileWriter.Factory fileWriterFactory = mock(FileWriter.Factory.class);
        FileWriter<LocationAverage> fileWriter = (FileWriter<LocationAverage>) mock(FileWriter.class);
        doAnswer(invocation -> {
            writtenToFile.add((List<LocationAverage>) invocation.getArguments()[0]);
            return null;
        }).when(fileWriter).write(any());
        when(fileWriterFactory.<LocationAverage>get(any(), any())).thenReturn(fileWriter);

        locationAggregator = new LocationAggregator(CONFIG, locations, fileWriterFactory, new ToStringMarshaller<>());
    }

    @Test
    void writesNothingToFileIfTooFewEvents() {
        sendEvent(locations.get(0), 0);
        sendEvent(locations.get(0), 0);

        assertThat(writtenToFile, empty());
    }

    @Test
    void calculatesAveragesForMultipleLocations() {
        sendEvent(locations.get(0), 1);
        sendEvent(locations.get(1), 3);
        sendEvent(locations.get(1), 5);

        assertThat(writtenToFile.size(), equalTo(1));
        assertThat(writtenToFile.get(0), contains(
                new LocationAverage(locations.get(0), 1),
                new LocationAverage(locations.get(1), 4)
        ));
    }

    @Test
    void handlesMissingLocations() {
        sendEvent(locations.get(1), 1);
        sendEvent(locations.get(1), 3);
        sendEvent(locations.get(1), 5);

        assertThat(writtenToFile.size(), equalTo(1));
        assertThat(writtenToFile.get(0), contains(
                new LocationAverage(locations.get(1), 3)
        ));
    }

    @Test
    void calculatesMultipleAverages() {
        sendEvent(locations.get(0), 1);
        sendEvent(locations.get(1), 3);
        sendEvent(locations.get(1), 5);

        sendEvent(locations.get(0), 2);
        sendEvent(locations.get(1), 4);
        sendEvent(locations.get(1), 6);

        assertThat(writtenToFile.size(), equalTo(2));
        assertThat(writtenToFile.get(0), contains(
                new LocationAverage(locations.get(0), 1),
                new LocationAverage(locations.get(1), 4)
        ));
        assertThat(writtenToFile.get(1), contains(
                new LocationAverage(locations.get(0), 1.5),
                new LocationAverage(locations.get(1), 4.5)
        ));
    }

    private void sendEvent(Location location, double value) {
        locationAggregator.handle(
                Instant.EPOCH,
                new EventBuilder()
                        .setLocationId(location.id)
                        .setValue(value)
                        .setEventId(UUID.randomUUID())
                        .createEvent()
        );
    }

}
