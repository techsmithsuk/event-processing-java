package org.softwire.training.analyzer.pipeline;

import java.time.Instant;
import java.util.stream.Stream;

@FunctionalInterface
public interface Pipeline<T, R> {
    Stream<R> handle(Instant now, T element);

    default <S> Pipeline<T, S> compose(Pipeline<R, S> p) {
        return (now, element) -> Pipeline.this.handle(now, element).flatMap(e -> p.handle(now, e));
    }
}
