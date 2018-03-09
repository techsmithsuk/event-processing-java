package org.softwire.training.analyzer.pipeline;

import org.softwire.training.analyzer.model.Event;

import java.time.Instant;

public interface Sink<T> {
    void handle(Instant now, T element);

    static Sink<Event> parallel(Sink<Event> lhs, Sink<Event> rhs) {
        return (now, element) -> {
            lhs.handle(now, element);
            rhs.handle(now, element);
        };
    }
}
