package org.softwire.training.sensoremitter.emitter;

import org.softwire.training.sensoremitter.model.Event;

import java.time.Instant;
import java.util.stream.Stream;

@FunctionalInterface
public interface Emitter {
    Emitter EMPTY = now -> Stream.empty();

    Stream<Event> emitIfRequired(Instant now);

    default Emitter concat(Emitter emitter) {
        return now -> Stream.concat(Emitter.this.emitIfRequired(now), emitter.emitIfRequired(now));
    }
}
