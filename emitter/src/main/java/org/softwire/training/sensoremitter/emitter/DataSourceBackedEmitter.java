package org.softwire.training.sensoremitter.emitter;

import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.model.Event;
import org.softwire.training.sensoremitter.model.Location;
import org.softwire.training.sensoremitter.datasource.DataSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Emits events on average every config.meanMillisToNextEvent, using a DataSource to obtain the value to be emitted.
 */
public class DataSourceBackedEmitter implements Emitter {

    private final DataSource dataSource;
    private final Location location;
    private final ApplicationConfig config;

    private final Random random = new Random();
    private final double startMillis;
    private final double endMillis;
    private Instant next;

    public DataSourceBackedEmitter(DataSource dataSource,
                                   Location location,
                                   ApplicationConfig config) {
        this.dataSource = dataSource;
        this.location = location;
        this.config = config;

        // Distribute `next` uniformly through time
        next = config.beginningOfTime.plus(Duration.ofMillis(random.nextInt(config.meanMillisToNextEvent)));

        startMillis = config.beginningOfTime.toEpochMilli();
        endMillis = config.endOfTime.toEpochMilli();
    }

    public Stream<Event> emitIfRequired(Instant now) {
        if (now.isAfter(next)) {
            int millis = Math.max(config.minimumMillisToNextEvent,
                    (int) Math.ceil(config.meanMillisToNextEvent + (random.nextGaussian() * config.stdDeviationMillisToNextEvent)));
            next = now.plus(Duration.ofMillis(millis));

            double t = (now.toEpochMilli() - startMillis) / (endMillis - startMillis);
            return Stream.of(new Event(
                    location.id,
                    dataSource.get(location.x / config.universeWidth, location.y / config.universeWidth, t),
                    now));
        }
        return Stream.empty();
    }
}
