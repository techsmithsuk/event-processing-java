package org.softwire.training.analyzer.services;

import org.junit.jupiter.api.Test;
import org.softwire.training.analyzer.model.Location;
import org.softwire.training.analyzer.model.LocationAverage;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class LocationAverageMarshallerTest {
    @Test
    void marshallLocationAverages() {
        LocationAverageMarshaller locationAverageMarshaller = new LocationAverageMarshaller();

        String marshalled = locationAverageMarshaller.marshall(Arrays.asList(
           new LocationAverage(new Location(0, 0, UUID.randomUUID()), 2.5),
           new LocationAverage(new Location(1, 2, UUID.randomUUID()), 0)
        ));

        assertThat(marshalled, equalTo("0.0,0.0,2.5\n1.0,2.0,0.0\n"));
    }
}
